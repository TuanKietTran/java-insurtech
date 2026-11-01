package com.insurtech.identity.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Deployable entry point of the {@code identity} bounded context (PRD-F01). */
@SpringBootApplication(scanBasePackages = "com.insurtech.identity")
public class IdentityServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(IdentityServiceApplication.class, args);
  }
}
