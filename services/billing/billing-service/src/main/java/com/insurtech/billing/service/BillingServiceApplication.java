package com.insurtech.billing.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Deployable entry point of the {@code billing} bounded context (PRD-B01). */
@SpringBootApplication(scanBasePackages = "com.insurtech.billing")
public class BillingServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(BillingServiceApplication.class, args);
  }
}
