package org.example.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.entity.User;
import org.example.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String HEADER_AUTH = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";
    private static final String HEADER_REFRESH_TOKEN = "X-Refresh-Token";
    private static final String HEADER_CLIENT_ID = "X-Client-Id";
    private static final String HEADER_REQUEST_ID = "X-Request-Id";

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserService userService;

    @Value("${jwt.refresh-threshold-minutes:30}")
    private long refreshThresholdMinutes;

    @Value("${security.excluded-paths:/api/v1/auth/**,/swagger-ui/**,/v3/api-docs/**,/actuator/health}")
    private String excludedPaths;

    @Value("${security.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    @Value("${security.rate-limit.requests-per-minute:60}")
    private int requestsPerMinute;

    private final ConcurrentMap<String, RequestCounter> requestCounts = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, CachedTokenInfo> tokenCache = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String requestId = getOrGenerateRequestId(request);
        String clientIp = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");

        logger.debug("Processing request {} from IP: {}", requestId, clientIp);

        try {
            if (rateLimitEnabled && !checkRateLimit(clientIp, response)) {
                return;
            }
            String jwt = extractJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && jwtUtils.validateJwtToken(jwt)) {
                processValidToken(jwt, request, clientIp, userAgent, requestId);
            } else if (StringUtils.hasText(jwt)) {
                logger.warn("Invalid JWT token from IP: {}, Request: {}", clientIp, requestId);
                handleInvalidToken(jwt, response);
                return;
            }

        } catch (Exception ex) {
            logger.error("Cannot set user authentication in security context for request {}: {}",
                    requestId, ex.getMessage(), ex);

            SecurityContextHolder.clearContext();
            sendAuthenticationError(response, "Authentication processing failed");
            return;
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        List<String> excluded = Arrays.asList(excludedPaths.split(","));

        return excluded.stream()
                .map(String::trim)
                .anyMatch(excludedPath -> {
                    if (excludedPath.endsWith("/**")) {
                        String basePath = excludedPath.substring(0, excludedPath.length() - 3);
                        return path.startsWith(basePath);
                    }
                    return path.equals(excludedPath);
                });
    }

    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(HEADER_AUTH);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(TOKEN_PREFIX)) {
            return bearerToken.substring(TOKEN_PREFIX.length()).trim();
        }

        String tokenParam = request.getParameter("token");
        if (StringUtils.hasText(tokenParam)) {
            return tokenParam.trim();
        }

        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("jwt_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }

    private void processValidToken(String jwt, HttpServletRequest request,
                                   String clientIp, String userAgent, String requestId) {
        CachedTokenInfo cachedInfo = tokenCache.get(jwt);
        if (cachedInfo != null && !cachedInfo.isExpired()) {
            setAuthenticationFromCache(cachedInfo.getUserDetails(), request);
            logger.debug("Used cached token info for request {}", requestId);
            return;
        }

        String username = jwtUtils.getUserNameFromJwtToken(jwt);
        if (username != null) {
            Optional<User> userOpt = userService.findByUsername(username);

            if (userOpt.isPresent()) {
                User user = userOpt.get();

                if (!isUserAccountValid(user)) {
                    logger.warn("Authentication attempt by invalid user account: {} from IP: {}",
                            username, clientIp);
                    return;
                }

                UserDetailsImpl userDetails = UserDetailsImpl.build(user);

                tokenCache.put(jwt, new CachedTokenInfo(userDetails,
                        jwtUtils.getExpirationDateFromJwtToken(jwt)));
                setAuthentication(userDetails, jwt, request, clientIp, userAgent);

                checkAndSuggestTokenRefresh(jwt, request);
                updateUserActivity(user, clientIp, userAgent);

                logger.debug("Successfully authenticated user {} for request {}", username, requestId);

            } else {
                logger.warn("User not found: {} from IP: {}, Request: {}", username, clientIp, requestId);
            }
        }
    }

    private void setAuthentication(UserDetailsImpl userDetails, String jwt,
                                   HttpServletRequest request, String clientIp, String userAgent) {

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, jwt, userDetails.getAuthorities());

        WebAuthenticationDetailsSource detailsSource = new WebAuthenticationDetailsSource();
        authentication.setDetails(detailsSource.buildDetails(request));


        if (authentication.getDetails() instanceof org.springframework.security.web.authentication.WebAuthenticationDetails) {
            // Нужно будет расширить
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void setAuthenticationFromCache(UserDetailsImpl userDetails, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        WebAuthenticationDetailsSource detailsSource = new WebAuthenticationDetailsSource();
        authentication.setDetails(detailsSource.buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private boolean isUserAccountValid(User user) {
        return user != null &&
                user.getStatus() == User.UserStatus.ACTIVE &&
                (user.getIsVerified() == null || user.getIsVerified());
    }

    private boolean checkRateLimit(String clientIp, HttpServletResponse response) throws IOException {
        RequestCounter counter = requestCounts.computeIfAbsent(clientIp,
                k -> new RequestCounter());

        if (counter.isLimitExceeded(requestsPerMinute)) {
            logger.warn("Rate limit exceeded for IP: {}", clientIp);
            sendRateLimitError(response);
            return false;
        }

        counter.incrementRequests();
        return true;
    }

    private void checkAndSuggestTokenRefresh(String jwt, HttpServletRequest request) {
        if (jwtUtils.isTokenExpiringWithin(jwt, refreshThresholdMinutes)) {
            logger.debug("Token is expiring soon, suggesting refresh");
        }
    }

    private void updateUserActivity(User user, String clientIp, String userAgent) {
        // TODO: Реализовать асинхронное обновление через @Async сервис
        // userActivityService.updateActivity(user.getId(), clientIp, userAgent);
    }

    private void handleInvalidToken(String jwt, HttpServletResponse response) throws IOException {
        try {
            if (jwtUtils.isTokenExpired(jwt)) {
                logger.debug("Token expired, not blacklisting");
            } else {
                jwtUtils.blacklistToken(jwt);
                logger.info("Invalid token added to blacklist");
            }
        } catch (Exception e) {
            logger.debug("Could not process invalid token: {}", e.getMessage());
        }

        SecurityContextHolder.clearContext();
        sendAuthenticationError(response, "Invalid or expired token");
    }

    private void sendAuthenticationError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String jsonResponse = String.format(
                "{\"error\":\"Unauthorized\",\"message\":\"%s\",\"timestamp\":\"%s\"}",
                message, LocalDateTime.now()
        );

        response.getWriter().write(jsonResponse);
    }

    private void sendRateLimitError(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Retry-After", "60");

        String jsonResponse = String.format(
                "{\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded\",\"timestamp\":\"%s\"}",
                LocalDateTime.now()
        );

        response.getWriter().write(jsonResponse);
    }

    private String getOrGenerateRequestId(HttpServletRequest request) {
        String requestId = request.getHeader(HEADER_REQUEST_ID);
        if (requestId == null) {
            requestId = java.util.UUID.randomUUID().toString().substring(0, 8);
        }
        return requestId;
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    public void cleanupExpiredCacheEntries() {
        tokenCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        requestCounts.entrySet().removeIf(entry -> entry.getValue().isExpired());

        logger.debug("Cleaned up expired cache entries. Token cache: {}, Request counters: {}",
                tokenCache.size(), requestCounts.size());
    }

    private static class CachedTokenInfo {
        private final UserDetailsImpl userDetails;
        private final LocalDateTime expiresAt;
        private final LocalDateTime cachedAt;

        public CachedTokenInfo(UserDetailsImpl userDetails, java.util.Date expirationDate) {
            this.userDetails = userDetails;
            this.expiresAt = expirationDate.toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDateTime();
            this.cachedAt = LocalDateTime.now();
        }

        public UserDetailsImpl getUserDetails() {
            return userDetails;
        }

        public boolean isExpired() {
            return LocalDateTime.now().isAfter(expiresAt) ||
                    LocalDateTime.now().isAfter(cachedAt.plusMinutes(5));
        }
    }

    private static class RequestCounter {
        private int count = 0;
        private LocalDateTime windowStart = LocalDateTime.now();
        private LocalDateTime lastRequest = LocalDateTime.now();

        public synchronized boolean isLimitExceeded(int limit) {
            LocalDateTime now = LocalDateTime.now();

            if (now.isAfter(windowStart.plusMinutes(1))) {
                count = 0;
                windowStart = now;
            }

            return count >= limit;
        }

        public synchronized void incrementRequests() {
            count++;
            lastRequest = LocalDateTime.now();
        }

        public boolean isExpired() {
            return LocalDateTime.now().isAfter(lastRequest.plusHours(1));
        }
    }
}