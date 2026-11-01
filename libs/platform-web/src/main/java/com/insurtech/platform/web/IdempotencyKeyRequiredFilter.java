package com.insurtech.platform.web;

import com.insurtech.kernel.error.InvalidValueException;
import com.insurtech.kernel.id.IdempotencyKey;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rejects mutating requests without a well-formed {@code Idempotency-Key} header with {@code 400}
 * (CRC-IDEM-001). Replay and conflict semantics (CRC-IDEM-002) are handled by the context's {@code
 * IdempotencyPort} implementation.
 */
public final class IdempotencyKeyRequiredFilter extends OncePerRequestFilter implements Ordered {

  public static final String HEADER = "Idempotency-Key";

  private static final Set<String> MUTATING_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !MUTATING_METHODS.contains(request.getMethod())
        || request.getRequestURI().startsWith("/actuator");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String key = request.getHeader(HEADER);
    if (key == null || key.isBlank()) {
      Problems.write(
          response,
          HttpServletResponse.SC_BAD_REQUEST,
          "IDEMPOTENCY_KEY_REQUIRED",
          "Mutating requests require an Idempotency-Key header");
      return;
    }
    try {
      new IdempotencyKey(key);
    } catch (InvalidValueException e) {
      Problems.write(
          response,
          HttpServletResponse.SC_BAD_REQUEST,
          e.code().value(),
          "Idempotency-Key must be 8-128 characters of [A-Za-z0-9_-:.]");
      return;
    }
    chain.doFilter(request, response);
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 10;
  }
}
