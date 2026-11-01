package com.insurtech.platform.application.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the permission a command or query handler requires, e.g. {@code claims:claim-line:close}
 * (CRC-SEC-001). Every public {@code handle} method of a {@code *Handler} in an application package
 * must carry it; ArchUnit enforces this.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Authorized {

  /** Permission in the form {@code <context>:<resource>:<verb>}. */
  String action();
}
