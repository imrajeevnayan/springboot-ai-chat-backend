package com.example.chatbackend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TrailingWhitespacePathFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String normalizedUri = trimTrailingEncodedWhitespace(request.getRequestURI());
        if (!normalizedUri.equals(request.getRequestURI())) {
            filterChain.doFilter(new NormalizedPathRequest(request, normalizedUri), response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String trimTrailingEncodedWhitespace(String uri) {
        String normalized = uri;
        boolean changed;
        do {
            String lower = normalized.toLowerCase();
            changed = false;
            if (lower.endsWith("%250a") || lower.endsWith("%250d") || lower.endsWith("%2520") || lower.endsWith("%2509")) {
                normalized = normalized.substring(0, normalized.length() - 5);
                changed = true;
            } else if (lower.endsWith("%0a") || lower.endsWith("%0d") || lower.endsWith("%20") || lower.endsWith("%09")) {
                normalized = normalized.substring(0, normalized.length() - 3);
                changed = true;
            } else if (!normalized.isEmpty() && Character.isWhitespace(normalized.charAt(normalized.length() - 1))) {
                normalized = normalized.substring(0, normalized.length() - 1);
                changed = true;
            }
        } while (changed);
        return normalized.isEmpty() ? "/" : normalized;
    }

    private static final class NormalizedPathRequest extends HttpServletRequestWrapper {
        private final String normalizedUri;

        private NormalizedPathRequest(HttpServletRequest request, String normalizedUri) {
            super(request);
            this.normalizedUri = normalizedUri;
        }

        @Override
        public String getRequestURI() {
            return normalizedUri;
        }

        @Override
        public StringBuffer getRequestURL() {
            HttpServletRequest request = (HttpServletRequest) getRequest();
            StringBuffer url = new StringBuffer();
            url.append(request.getScheme()).append("://").append(request.getServerName());
            int port = request.getServerPort();
            if (port > 0 && !isDefaultPort(request.getScheme(), port)) {
                url.append(':').append(port);
            }
            url.append(normalizedUri);
            return url;
        }

        @Override
        public String getServletPath() {
            String contextPath = getContextPath();
            if (!contextPath.isEmpty() && normalizedUri.startsWith(contextPath)) {
                return normalizedUri.substring(contextPath.length());
            }
            return normalizedUri;
        }

        private boolean isDefaultPort(String scheme, int port) {
            return ("http".equalsIgnoreCase(scheme) && port == 80)
                    || ("https".equalsIgnoreCase(scheme) && port == 443);
        }
    }
}
