package com.insurtech.ledger.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Deployable entry point of the {@code ledger} bounded context (PRD-F04). */
@SpringBootApplication(scanBasePackages = "com.insurtech.ledger")
public class LedgerServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(LedgerServiceApplication.class, args);
  }
}
