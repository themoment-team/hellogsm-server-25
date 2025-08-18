package team.themoment.hellogsmv3.global.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

@Component
public class LoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(LoggingFilter.class);

    private static final String[] NOT_LOGGING_URL = {
            "/api-docs/**", "/swagger-ui/**"
    };

    private final AntPathMatcher matcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) {

        if (isNotLoggingURL(request.getRequestURI())) {
            try {
                filterChain.doFilter(request, response);
            } catch (Exception e) {
                log.error("로깅 제외 경로 예외", e);
            }

            return;
        }

        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        UUID logId = UUID.randomUUID();
        long startTime = System.currentTimeMillis();

        try {
            requestLogging(requestWrapper, logId);
            filterChain.doFilter(requestWrapper, responseWrapper);
        } catch (Exception e) {
            log.error("LoggingFilter의 FilterChain에서 예외가 발생했습니다.", e);
        } finally {
            responseLogging(responseWrapper, startTime, logId);
            try {
                responseWrapper.copyBodyToResponse();
            } catch (IOException e) {
                log.error("LoggingFilter에서 response body를 출력하는 도중 예외가 발생했습니다.", e);
            }
        }
    }

    private boolean isNotLoggingURL(String requestURI) {
        return Arrays.stream(NOT_LOGGING_URL)
                .anyMatch(pattern -> matcher.match(pattern, requestURI));
    }

    private void requestLogging(ContentCachingRequestWrapper request, UUID logId) {
        log.info(
                String.format(
                        "Log-ID: %s, IP: %s, URI: %s, Http-Method: %s, Params: %s, Content-Type: %s, User-Cookies: %s, User-Agent: %s, Request-Body: %s",
                        logId,
                        request.getRemoteAddr(),
                        request.getRequestURI(),
                        request.getMethod(),
                        request.getQueryString(),
                        request.getContentType(),
                        request.getCookies() != null ? String.join(", ", getCookiesAsString(request.getCookies())) : "[none]",
                        request.getHeader("User-Agent"),
                        getRequestBody(request.getContentAsByteArray())
                )
        );
    }

    private void responseLogging(ContentCachingResponseWrapper response, long startTime, UUID logId) {
        long endTime = System.currentTimeMillis();
        long responseTime = endTime - startTime;
        log.info(
                String.format(
                        "Log-ID: %s, Status-Code: %d, Content-Type: %s, Response Time: %dms, Response-Body: %s",
                        logId,
                        response.getStatus(),
                        response.getContentType(),
                        responseTime,
                        new String(response.getContentAsByteArray(), StandardCharsets.UTF_8)
                )
        );
    }

    private String getRequestBody(byte[] byteArrayContent) {
        String oneLineContent = new String(byteArrayContent, StandardCharsets.UTF_8).replaceAll("\\s", "");
        return StringUtils.hasText(oneLineContent) ? oneLineContent : "[empty]";
    }

    private String[] getCookiesAsString(Cookie[] cookies) {
        return Arrays.stream(cookies)
                .map(cookie -> String.format("%s=%s", cookie.getName(), cookie.getValue()))
                .toArray(String[]::new);
    }
}
