package com.insurtech.platform.web;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.Locale;

/** RFC 9457 Problem Details helpers with stable {@code type} URIs (CRC-API-003). */
public final class Problems {

  public static final String MEDIA_TYPE = "application/problem+json";

  private Problems() {}

  /** {@code ILLEGAL_STATE_TRANSITION} → {@code urn:insurtech:problem:illegal-state-transition}. */
  public static URI typeFor(String errorCode) {
    return URI.create(
        "urn:insurtech:problem:" + errorCode.toLowerCase(Locale.ROOT).replace('_', '-'));
  }

  static void write(HttpServletResponse response, int status, String errorCode, String detail)
      throws IOException {
    response.setStatus(status);
    response.setContentType(MEDIA_TYPE);
    response
        .getWriter()
        .write(
            "{\"type\":\""
                + typeFor(errorCode)
                + "\",\"status\":"
                + status
                + ",\"code\":\""
                + errorCode
                + "\",\"detail\":\""
                + escape(detail)
                + "\"}");
  }

  private static String escape(String value) {
    return value.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}
