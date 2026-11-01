package com.insurtech.platform.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class IdempotencyKeyRequiredFilterTest {

  private final IdempotencyKeyRequiredFilter filter = new IdempotencyKeyRequiredFilter();

  @Test
  void rejectsMutationWithoutKey() throws Exception {
    MockHttpServletResponse response = run(new MockHttpServletRequest("POST", "/v1/claims"));

    assertThat(response.getStatus()).isEqualTo(400);
    assertThat(response.getContentType()).isEqualTo(Problems.MEDIA_TYPE);
    assertThat(response.getContentAsString()).contains("IDEMPOTENCY_KEY_REQUIRED");
  }

  @Test
  void rejectsMalformedKey() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/v1/claims");
    request.addHeader(IdempotencyKeyRequiredFilter.HEADER, "short");

    assertThat(run(request).getStatus()).isEqualTo(400);
  }

  @Test
  void passesMutationWithKey() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/v1/claims");
    request.addHeader(IdempotencyKeyRequiredFilter.HEADER, "0b9f6a3e-6c1d-4a51-9a0e-1f2d3c4b5a69");
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request, new MockHttpServletResponse(), chain);

    assertThat(chain.getRequest()).isSameAs(request);
  }

  @Test
  void ignoresSafeMethodsAndActuator() throws Exception {
    assertThat(run(new MockHttpServletRequest("GET", "/v1/claims/1")).getStatus()).isEqualTo(200);
    assertThat(run(new MockHttpServletRequest("POST", "/actuator/refresh")).getStatus())
        .isEqualTo(200);
  }

  private MockHttpServletResponse run(MockHttpServletRequest request) throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    filter.doFilter(request, response, new MockFilterChain());
    return response;
  }
}
