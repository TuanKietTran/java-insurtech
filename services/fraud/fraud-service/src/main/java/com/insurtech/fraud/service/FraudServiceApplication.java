package com.insurtech.fraud.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Deployable entry point of the {@code fraud} bounded context (PRD-R03). */
@SpringBootApplication(scanBasePackages = "com.insurtech.fraud")
public class FraudServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(FraudServiceApplication.class, args);
  }
}
