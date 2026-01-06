package rotld.apscrm.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Filter that adds request context to MDC for logging and logs request/response details.
 * All logs during a request will include: requestId, method, URI, and user info.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID = "requestId";
    private static final String METHOD = "method";
    private static final String URI = "uri";
    private static final String QUERY = "query";
    private static final String CLIENT_IP = "clientIp";
    private static final String USER = "user";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Generate unique request ID for tracing
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        
        // Wrap request/response for caching (allows reading body multiple times)
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        long startTime = System.currentTimeMillis();

        try {
            // Set MDC context - these values will appear in all logs during this request
            MDC.put(REQUEST_ID, requestId);
            MDC.put(METHOD, request.getMethod());
            MDC.put(URI, request.getRequestURI());
            MDC.put(QUERY, request.getQueryString() != null ? request.getQueryString() : "");
            MDC.put(CLIENT_IP, getClientIp(request));
            MDC.put(USER, request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "anonymous");

            // Log incoming request
            logRequest(wrappedRequest);

            // Continue with the filter chain
            filterChain.doFilter(wrappedRequest, wrappedResponse);

        } finally {
            long duration = System.currentTimeMillis() - startTime;
            
            // Log response
            logResponse(wrappedResponse, duration);

            // Copy response body back to original response
            wrappedResponse.copyBodyToResponse();

            // Clear MDC to prevent memory leaks
            MDC.clear();
        }
    }

    private void logRequest(ContentCachingRequestWrapper request) {
        String queryString = request.getQueryString();
        String fullPath = queryString != null 
            ? request.getRequestURI() + "?" + queryString 
            : request.getRequestURI();

        log.info(">>> REQUEST: {} {} | IP: {} | User-Agent: {}",
                request.getMethod(),
                fullPath,
                getClientIp(request),
                request.getHeader("User-Agent"));

        // Log request body for POST/PUT/PATCH (but not for file uploads)
        if (shouldLogBody(request)) {
            String body = getRequestBody(request);
            if (!body.isEmpty()) {
                // Truncate very long bodies and mask sensitive fields
                String maskedBody = maskSensitiveData(truncate(body, 2000));
                log.debug(">>> REQUEST BODY: {}", maskedBody);
            }
        }
    }

    private void logResponse(ContentCachingResponseWrapper response, long duration) {
        int status = response.getStatus();
        String statusText = status >= 400 ? "ERROR" : "OK";
        
        if (status >= 500) {
            log.error("<<< RESPONSE: {} {} | Duration: {}ms", status, statusText, duration);
        } else if (status >= 400) {
            log.warn("<<< RESPONSE: {} {} | Duration: {}ms", status, statusText, duration);
        } else {
            log.info("<<< RESPONSE: {} {} | Duration: {}ms", status, statusText, duration);
        }

        // Log response body for errors
        if (status >= 400 && response.getContentSize() > 0) {
            String body = getResponseBody(response);
            if (!body.isEmpty()) {
                log.debug("<<< RESPONSE BODY: {}", truncate(body, 1000));
            }
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    private boolean shouldLogBody(HttpServletRequest request) {
        String contentType = request.getContentType();
        if (contentType == null) return false;
        
        String method = request.getMethod();
        boolean isModifyingMethod = "POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method);
        boolean isJsonContent = contentType.contains("application/json");
        
        return isModifyingMethod && isJsonContent;
    }

    private String getRequestBody(ContentCachingRequestWrapper request) {
        byte[] content = request.getContentAsByteArray();
        if (content.length == 0) {
            return "";
        }
        return new String(content, StandardCharsets.UTF_8);
    }

    private String getResponseBody(ContentCachingResponseWrapper response) {
        byte[] content = response.getContentAsByteArray();
        if (content.length == 0) {
            return "";
        }
        return new String(content, StandardCharsets.UTF_8);
    }

    private String truncate(String str, int maxLength) {
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength) + "... [TRUNCATED]";
    }

    private String maskSensitiveData(String body) {
        // Mask common sensitive fields in JSON
        return body
            .replaceAll("\"password\"\\s*:\\s*\"[^\"]*\"", "\"password\":\"***\"")
            .replaceAll("\"token\"\\s*:\\s*\"[^\"]*\"", "\"token\":\"***\"")
            .replaceAll("\"accessToken\"\\s*:\\s*\"[^\"]*\"", "\"accessToken\":\"***\"")
            .replaceAll("\"refreshToken\"\\s*:\\s*\"[^\"]*\"", "\"refreshToken\":\"***\"")
            .replaceAll("\"secret\"\\s*:\\s*\"[^\"]*\"", "\"secret\":\"***\"")
            .replaceAll("\"otp\"\\s*:\\s*\"[^\"]*\"", "\"otp\":\"***\"");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Skip logging for health checks and static resources
        String path = request.getRequestURI();
        return path.startsWith("/actuator") || 
               path.startsWith("/favicon") ||
               path.endsWith(".css") ||
               path.endsWith(".js") ||
               path.endsWith(".png") ||
               path.endsWith(".jpg");
    }
}

