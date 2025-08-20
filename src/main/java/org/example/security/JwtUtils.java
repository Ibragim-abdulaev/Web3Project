package org.example.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class JwtUtils {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}") // 24 часа по умолчанию
    private int jwtExpirationMs;

    @Value("${jwt.refresh-expiration:604800000}") // 7 дней по умолчанию
    private int refreshExpirationMs;

    @Value("${jwt.remember-me-expiration:2592000000}") // 30 дней
    private long rememberMeExpirationMs;

    @Value("${jwt.issuer:wallet-app}")
    private String jwtIssuer;

    private final Set<String> tokenBlacklist = ConcurrentHashMap.newKeySet();

    private final Map<String, Claims> tokenCache = new ConcurrentHashMap<>();

    private Key key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
        logger.info("JWT Utils initialized with issuer: {}", jwtIssuer);
    }

    public String generateJwtToken(Authentication authentication) {
        UserDetailsImpl userPrincipal = (UserDetailsImpl) authentication.getPrincipal();
        return generateJwtToken(userPrincipal, false);
    }

    public String generateJwtToken(Authentication authentication, boolean rememberMe) {
        UserDetailsImpl userPrincipal = (UserDetailsImpl) authentication.getPrincipal();
        return generateJwtToken(userPrincipal, rememberMe);
    }

    public String generateJwtToken(UserDetailsImpl userDetails, boolean rememberMe) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + (rememberMe ? rememberMeExpirationMs : jwtExpirationMs));

        List<String> authorities = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .setIssuer(jwtIssuer)
                .setIssuedAt(now)
                .setExpiration(expirationDate)
                .claim("userId", userDetails.getId())
                .claim("email", userDetails.getEmail())
                .claim("authorities", authorities)
                .claim("type", "access")
                .claim("rememberMe", rememberMe)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    public String generateTokenFromUsername(String username) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .setSubject(username)
                .setIssuer(jwtIssuer)
                .setIssuedAt(now)
                .setExpiration(expirationDate)
                .claim("type", "access")
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    public String generateRefreshToken(String username) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + refreshExpirationMs);

        return Jwts.builder()
                .setSubject(username)
                .setIssuer(jwtIssuer)
                .setIssuedAt(now)
                .setExpiration(expirationDate)
                .claim("type", "refresh")
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    public String getUserNameFromJwtToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? claims.getSubject() : null;
    }

    public Long getUserIdFromJwtToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims != null && claims.get("userId") != null) {
            return Long.valueOf(claims.get("userId").toString());
        }
        return null;
    }

    public String getEmailFromJwtToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? (String) claims.get("email") : null;
    }

    @SuppressWarnings("unchecked")
    public List<String> getAuthoritiesFromJwtToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? (List<String>) claims.get("authorities") : List.of();
    }

    public Date getExpirationDateFromJwtToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? claims.getExpiration() : null;
    }

    public Date getIssuedAtDateFromJwtToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? claims.getIssuedAt() : null;
    }

    public boolean isRefreshToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null && "refresh".equals(claims.get("type"));
    }

    public boolean isRememberMeToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null && Boolean.TRUE.equals(claims.get("rememberMe"));
    }

    public boolean isTokenExpired(String token) {
        Date expiration = getExpirationDateFromJwtToken(token);
        return expiration != null && expiration.before(new Date());
    }

    public boolean isTokenExpiringWithin(String token, long minutes) {
        Date expiration = getExpirationDateFromJwtToken(token);
        if (expiration == null) return true;

        long expirationTime = expiration.getTime();
        long currentTime = System.currentTimeMillis();
        long minutesToExpiration = (expirationTime - currentTime) / (1000 * 60);

        return minutesToExpiration <= minutes;
    }

    public boolean validateJwtToken(String authToken) {
        if (authToken == null || authToken.trim().isEmpty()) {
            logger.debug("JWT token is null or empty");
            return false;
        }

        if (isTokenBlacklisted(authToken)) {
            logger.warn("Attempt to use blacklisted token");
            return false;
        }

        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .requireIssuer(jwtIssuer)
                    .build()
                    .parseClaimsJws(authToken)
                    .getBody();

            tokenCache.put(authToken, claims);

            logger.debug("JWT token validated successfully for user: {}", claims.getSubject());
            return true;

        } catch (SecurityException e) {
            logger.error("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected JWT validation error: {}", e.getMessage());
        }

        return false;
    }

    public ValidationResult validateJwtTokenWithDetails(String authToken) {
        if (authToken == null || authToken.trim().isEmpty()) {
            return new ValidationResult(false, "Token is null or empty");
        }

        if (isTokenBlacklisted(authToken)) {
            return new ValidationResult(false, "Token is blacklisted");
        }

        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .requireIssuer(jwtIssuer)
                    .build()
                    .parseClaimsJws(authToken);

            return new ValidationResult(true, "Token is valid");

        } catch (ExpiredJwtException e) {
            return new ValidationResult(false, "Token is expired");
        } catch (SecurityException | MalformedJwtException e) {
            return new ValidationResult(false, "Invalid token signature or format");
        } catch (Exception e) {
            return new ValidationResult(false, "Token validation failed: " + e.getMessage());
        }
    }

    public void blacklistToken(String token) {
        if (token != null && !token.trim().isEmpty()) {
            tokenBlacklist.add(token);
            tokenCache.remove(token);
            logger.info("Token added to blacklist");
        }
    }
    public boolean isTokenBlacklisted(String token) {
        return tokenBlacklist.contains(token);
    }

    private Claims getClaimsFromToken(String token) {
        if (token == null || isTokenBlacklisted(token)) {
            return null;
        }

        Claims cachedClaims = tokenCache.get(token);
        if (cachedClaims != null) {
            return cachedClaims;
        }

        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            tokenCache.put(token, claims);
            return claims;

        } catch (Exception e) {
            logger.debug("Failed to parse claims from token: {}", e.getMessage());
            return null;
        }
    }

    @Scheduled(fixedRate = 3600000)
    public void cleanupExpiredTokens() {
        logger.debug("Starting cleanup of expired tokens...");

        int blacklistSizeBefore = tokenBlacklist.size();
        int cacheSizeBefore = tokenCache.size();

        tokenBlacklist.removeIf(token -> {
            try {
                Date expiration = getExpirationDateFromJwtToken(token);
                return expiration != null && expiration.before(new Date());
            } catch (Exception e) {
                return true;
            }
        });

        tokenCache.entrySet().removeIf(entry -> {
            try {
                Claims claims = entry.getValue();
                return claims.getExpiration().before(new Date());
            } catch (Exception e) {
                return true;
            }
        });

        logger.info("Token cleanup completed. Blacklist: {} -> {}, Cache: {} -> {}",
                blacklistSizeBefore, tokenBlacklist.size(),
                cacheSizeBefore, tokenCache.size());
    }
    public String refreshToken(String oldToken) {
        Claims claims = getClaimsFromToken(oldToken);
        if (claims == null) {
            throw new IllegalArgumentException("Invalid token for refresh");
        }

        String username = claims.getSubject();
        Long userId = claims.get("userId", Long.class);
        String email = claims.get("email", String.class);
        @SuppressWarnings("unchecked")
        List<String> authorities = claims.get("authorities", List.class);
        Boolean rememberMe = claims.get("rememberMe", Boolean.class);

        Date now = new Date();
        Date expirationDate = new Date(now.getTime() +
                (Boolean.TRUE.equals(rememberMe) ? rememberMeExpirationMs : jwtExpirationMs));

        return Jwts.builder()
                .setSubject(username)
                .setIssuer(jwtIssuer)
                .setIssuedAt(now)
                .setExpiration(expirationDate)
                .claim("userId", userId)
                .claim("email", email)
                .claim("authorities", authorities)
                .claim("type", "access")
                .claim("rememberMe", rememberMe)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }
    public TokenStats getTokenStats() {
        return new TokenStats(tokenBlacklist.size(), tokenCache.size());
    }

    public static class ValidationResult {
        private final boolean valid;
        private final String message;

        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
    }

    public static class TokenStats {
        private final int blacklistedTokens;
        private final int cachedTokens;
        private final LocalDateTime timestamp;

        public TokenStats(int blacklistedTokens, int cachedTokens) {
            this.blacklistedTokens = blacklistedTokens;
            this.cachedTokens = cachedTokens;
            this.timestamp = LocalDateTime.now();
        }

        public int getBlacklistedTokens() { return blacklistedTokens; }
        public int getCachedTokens() { return cachedTokens; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
}