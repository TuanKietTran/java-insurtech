package com.insurtech.underwriting.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Deployable entry point of the {@code underwriting} bounded context (PRD-U01). */
@SpringBootApplication(scanBasePackages = "com.insurtech.underwriting")
public class UnderwritingServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(UnderwritingServiceApplication.class, args);
  }
}
