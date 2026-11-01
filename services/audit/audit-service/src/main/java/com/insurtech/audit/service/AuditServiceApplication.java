package com.insurtech.audit.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Deployable entry point of the {@code audit} bounded context (PRD-F02). */
@SpringBootApplication(scanBasePackages = "com.insurtech.audit")
public class AuditServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(AuditServiceApplication.class, args);
  }
}
