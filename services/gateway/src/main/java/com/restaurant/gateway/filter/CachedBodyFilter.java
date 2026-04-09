package com.restaurant.gateway.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Caches the request body so it can be read multiple times.
 * This is essential for proxying multipart requests — without caching,
 * the input stream may be consumed before ProxyController reads it.
 */
@Component
@Order(0)
public class CachedBodyFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest httpRequest) {
            String method = httpRequest.getMethod().toUpperCase();
            if ("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method)) {
                CachedBodyRequest cachedRequest = new CachedBodyRequest(httpRequest);
                chain.doFilter(cachedRequest, response);
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private static class CachedBodyRequest extends HttpServletRequestWrapper {
        private final byte[] cachedBody;

        CachedBodyRequest(HttpServletRequest request) throws IOException {
            super(request);
            this.cachedBody = StreamUtils.copyToByteArray(request.getInputStream());
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream bais = new ByteArrayInputStream(cachedBody);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() { return bais.available() == 0; }
                @Override
                public boolean isReady() { return true; }
                @Override
                public void setReadListener(ReadListener listener) { }
                @Override
                public int read() { return bais.read(); }
            };
        }
    }
}
