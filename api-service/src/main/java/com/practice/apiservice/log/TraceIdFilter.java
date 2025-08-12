package com.practice.apiservice.log;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String TRACE_ID = "traceId";
    public static final String SPAN_ID  = "spanId";
    public static final String REQ_ID_H = "X-Request-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {
        // Si viene un request-id de un gateway, úsalo
        String incoming = req.getHeader(REQ_ID_H);
        String traceId = (incoming != null && !incoming.isBlank()) ? incoming : UUID.randomUUID().toString();
        String spanId  = UUID.randomUUID().toString().substring(0, 16);

        MDC.put(TRACE_ID, traceId);
        MDC.put(SPAN_ID, spanId);
        try {
            chain.doFilter(req, res);
        } finally {
            MDC.remove(TRACE_ID);
            MDC.remove(SPAN_ID);
        }
    }
}
