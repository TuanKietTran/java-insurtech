package com.insurtech.aiharness.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Deployable entry point of the {@code aiharness} bounded context (PRD-F07, PRD-AI01, PRD-AI02,
 * PRD-AI03, PRD-AI04, PRD-AI05, PRD-AI06, PRD-AI07).
 */
@SpringBootApplication(scanBasePackages = "com.insurtech.aiharness")
public class AiHarnessServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(AiHarnessServiceApplication.class, args);
  }
}
