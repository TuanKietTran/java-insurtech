package com.insurtech.privacy.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Deployable entry point of the {@code privacy} bounded context (PRD-R01). */
@SpringBootApplication(scanBasePackages = "com.insurtech.privacy")
public class PrivacyServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(PrivacyServiceApplication.class, args);
  }
}
