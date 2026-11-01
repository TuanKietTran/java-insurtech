package com.insurtech.platform.application.security;

import java.util.Map;
import java.util.Objects;

/** Resource being accessed, with the attributes the PDP needs for ABAC decisions (PRD-F01). */
public record ResourceRef(String type, String id, Map<String, String> attributes) {

  public ResourceRef {
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(id, "id");
    attributes = Map.copyOf(attributes);
  }
}
