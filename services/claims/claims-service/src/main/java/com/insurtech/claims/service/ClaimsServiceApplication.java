package com.insurtech.claims.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Deployable entry point of the {@code claims} bounded context (PRD-C02, PRD-C03, PRD-C04,
 * PRD-C05).
 */
@SpringBootApplication(scanBasePackages = "com.insurtech.claims")
public class ClaimsServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(ClaimsServiceApplication.class, args);
  }
}
