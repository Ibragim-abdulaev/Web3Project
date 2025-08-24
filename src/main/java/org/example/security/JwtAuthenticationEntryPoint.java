package org.example.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationEntryPoint.class);

    private static final String HEADER_REQUEST_ID = "X-Request-Id";
    private static final String HEADER_CORRELATION_ID = "X-Correlation-Id";
    private static final String CONTENT_TYPE_JSON = MediaType.APPLICATION_JSON_VALUE;

    @Value("${security.error-response.include-path:true}")
    private boolean includePath;

    @Value("${security.error-response.include-timestamp:true}")
    private boolean includeTimestamp;

    @Value("${security.error-response.include-request-id:true}")
    private boolean includeRequestId;

    @Value("${security.error-response.include-details:false}")
    private boolean includeDetails;

    @Value("${security.cors.allowed-origins:*}")
    private String allowedOrigins;

    @Value("${security.cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS}")
    private String allowedMethods;

    @Value("${security.cors.allowed-headers:*}")
    private String allowedHeaders;

    @Value("${security.cors.max-age:3600}")
    private long corsMaxAge;

    private ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        logger.info("JwtAuthenticationEntryPoint initialized with CORS origins: {}", allowedOrigins);
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {

        String requestId = getOrGenerateRequestId(request);
        String clientIp = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        String method = request.getMethod();
        String uri = request.getRequestURI();

        logger.warn("Unauthorized access attempt - Method: {}, URI: {}, IP: {}, User-Agent: {}, Request-ID: {}, Reason: {}",
                method, uri, clientIp, userAgent, requestId, authException.getMessage());

        ErrorType errorType = determineErrorType(authException, request);

        configureCorsHeaders(request, response);
        if ("OPTIONS".equalsIgnoreCase(method)) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }
        configureResponseHeaders(response, requestId);

        Map<String, Object> errorResponse = createErrorResponse(
                errorType, authException, request, requestId, clientIp
        );

        response.setStatus(errorType.getHttpStatus());
        objectMapper.writeValue(response.getOutputStream(), errorResponse);
        logSecurityEvent(request, authException, errorType, requestId, clientIp);
    }

    private ErrorType determineErrorType(AuthenticationException authException, HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ErrorType.MISSING_TOKEN;
        }

        String exceptionMessage = authException.getMessage().toLowerCase();

        if (exceptionMessage.contains("expired")) {
            return ErrorType.EXPIRED_TOKEN;
        } else if (exceptionMessage.contains("malformed") || exceptionMessage.contains("invalid")) {
            return ErrorType.INVALID_TOKEN;
        } else if (exceptionMessage.contains("signature")) {
            return ErrorType.INVALID_SIGNATURE;
        } else if (exceptionMessage.contains("blacklist")) {
            return ErrorType.BLACKLISTED_TOKEN;
        }

        return ErrorType.AUTHENTICATION_FAILED;
    }

    private Map<String, Object> createErrorResponse(ErrorType errorType,
                                                    AuthenticationException authException,
                                                    HttpServletRequest request,
                                                    String requestId, String clientIp) {
        Map<String, Object> body = new HashMap<>();

        body.put("error", errorType.getCode());
        body.put("message", errorType.getMessage());
        body.put("status", errorType.getHttpStatus());

        if (includeTimestamp) {
            body.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }

        if (includePath) {
            body.put("path", request.getRequestURI());
        }

        if (includeRequestId) {
            body.put("requestId", requestId);
        }

        if (includeDetails) {
            Map<String, Object> details = new HashMap<>();
            details.put("method", request.getMethod());
            details.put("clientIp", clientIp);
            details.put("userAgent", request.getHeader("User-Agent"));
            details.put("referer", request.getHeader("Referer"));

            if (authException.getCause() != null) {
                details.put("cause", authException.getCause().getMessage());
            }

            body.put("details", details);
        }

        body.put("suggestions", errorType.getSuggestions());

        return body;
    }

    private void configureCorsHeaders(HttpServletRequest request, HttpServletResponse response) {
        String origin = request.getHeader("Origin");

        if (isOriginAllowed(origin)) {
            response.setHeader("Access-Control-Allow-Origin", origin);
        } else if ("*".equals(allowedOrigins)) {
            response.setHeader("Access-Control-Allow-Origin", "*");
        }

        response.setHeader("Access-Control-Allow-Methods", allowedMethods);
        response.setHeader("Access-Control-Allow-Headers", allowedHeaders);
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Max-Age", String.valueOf(corsMaxAge));

        response.setHeader("Access-Control-Expose-Headers",
                "X-Request-Id,X-Correlation-Id,X-Rate-Limit-Remaining");
    }

    private void configureResponseHeaders(HttpServletResponse response, String requestId) {
        response.setContentType(CONTENT_TYPE_JSON);
        response.setCharacterEncoding("UTF-8");

        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("X-XSS-Protection", "1; mode=block");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        response.setHeader(HEADER_REQUEST_ID, requestId);
        response.setHeader(HEADER_CORRELATION_ID, requestId);
    }

    private boolean isOriginAllowed(String origin) {
        if (!StringUtils.hasText(origin) || "*".equals(allowedOrigins)) {
            return true;
        }

        String[] origins = allowedOrigins.split(",");
        for (String allowedOrigin : origins) {
            if (origin.equalsIgnoreCase(allowedOrigin.trim())) {
                return true;
            }
        }

        return false;
    }

    private String getOrGenerateRequestId(HttpServletRequest request) {
        String requestId = request.getHeader(HEADER_REQUEST_ID);
        if (!StringUtils.hasText(requestId)) {
            requestId = request.getHeader(HEADER_CORRELATION_ID);
        }
        if (!StringUtils.hasText(requestId)) {
            requestId = UUID.randomUUID().toString().substring(0, 8);
        }
        return requestId;
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {
                "X-Forwarded-For", "X-Real-IP", "X-Originating-IP", "X-Client-IP"
        };

        for (String headerName : headerNames) {
            String ip = request.getHeader(headerName);
            if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }

        return request.getRemoteAddr();
    }

    private void logSecurityEvent(HttpServletRequest request,
                                  AuthenticationException authException,
                                  ErrorType errorType, String requestId, String clientIp) {

        Map<String, Object> securityEvent = new HashMap<>();
        securityEvent.put("eventType", "UNAUTHORIZED_ACCESS_ATTEMPT");
        securityEvent.put("requestId", requestId);
        securityEvent.put("clientIp", clientIp);
        securityEvent.put("method", request.getMethod());
        securityEvent.put("uri", request.getRequestURI());
        securityEvent.put("userAgent", request.getHeader("User-Agent"));
        securityEvent.put("referer", request.getHeader("Referer"));
        securityEvent.put("errorType", errorType.getCode());
        securityEvent.put("errorMessage", authException.getMessage());
        securityEvent.put("timestamp", LocalDateTime.now());

        logger.info("Security event: {}", securityEvent);
    }

    public enum ErrorType {
        MISSING_TOKEN(401, "MISSING_TOKEN",
                "Authentication token is missing",
                new String[]{"Include 'Authorization: Bearer <token>' header"}),

        EXPIRED_TOKEN(401, "EXPIRED_TOKEN",
                "Authentication token has expired",
                new String[]{"Refresh your token", "Login again"}),

        INVALID_TOKEN(401, "INVALID_TOKEN",
                "Authentication token is invalid",
                new String[]{"Check token format", "Login again"}),

        INVALID_SIGNATURE(401, "INVALID_SIGNATURE",
                "Token signature is invalid",
                new String[]{"Login again", "Contact support if problem persists"}),

        BLACKLISTED_TOKEN(401, "BLACKLISTED_TOKEN",
                "Token has been revoked",
                new String[]{"Login again"}),

        AUTHENTICATION_FAILED(401, "AUTHENTICATION_FAILED",
                "Authentication failed",
                new String[]{"Check credentials", "Login again"});

        private final int httpStatus;
        private final String code;
        private final String message;
        private final String[] suggestions;

        ErrorType(int httpStatus, String code, String message, String[] suggestions) {
            this.httpStatus = httpStatus;
            this.code = code;
            this.message = message;
            this.suggestions = suggestions;
        }

        public int getHttpStatus() { return httpStatus; }
        public String getCode() { return code; }
        public String getMessage() { return message; }
        public String[] getSuggestions() { return suggestions; }
    }
}