package com.insurtech.platform.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Accepts or assigns {@code X-Correlation-Id}, echoes it on the response and exposes it to
 * structured logs as {@code correlationId} (CRC-API-007, CRC-OBS-002).
 */
public final class CorrelationIdFilter extends OncePerRequestFilter implements Ordered {

  public static final String HEADER = "X-Correlation-Id";
  public static final String MDC_KEY = "correlationId";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String correlationId = validOrNew(request.getHeader(HEADER));
    response.setHeader(HEADER, correlationId);
    MDC.put(MDC_KEY, correlationId);
    try {
      chain.doFilter(request, response);
    } finally {
      MDC.remove(MDC_KEY);
    }
  }

  private static String validOrNew(@Nullable String header) {
    if (header != null) {
      try {
        return UUID.fromString(header).toString();
      } catch (IllegalArgumentException ignored) {
        // Not a UUID: assign a fresh correlation ID rather than trusting arbitrary input.
      }
    }
    return UUID.randomUUID().toString();
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }
}
