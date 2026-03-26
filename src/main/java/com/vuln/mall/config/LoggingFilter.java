package com.vuln.mall.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class LoggingFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(LoggingFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        long startTime = System.currentTimeMillis();

        // Continue processing the request
        chain.doFilter(request, response);

        long duration = System.currentTimeMillis() - startTime;

        String method = req.getMethod();
        String uri = req.getRequestURI();
        String queryString = req.getQueryString();
        int status = res.getStatus();
        String remoteAddr = req.getRemoteAddr();

        if (queryString != null) {
            uri += "?" + queryString;
        }

        // Exclude some noisy paths if needed (e.g., /css, /images), but we show all for
        // now
        logger.info("[Access Log] {} {} {} - Status: {} ({} ms)", remoteAddr, method, uri, status, duration);
    }
}
