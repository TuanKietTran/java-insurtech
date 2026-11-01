package com.insurtech.policy.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Deployable entry point of the {@code policy} bounded context (PRD-P02). */
@SpringBootApplication(scanBasePackages = "com.insurtech.policy")
public class PolicyServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(PolicyServiceApplication.class, args);
  }
}
