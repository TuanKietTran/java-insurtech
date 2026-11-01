package com.insurtech.commissions.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Deployable entry point of the {@code commissions} bounded context (PRD-B02). */
@SpringBootApplication(scanBasePackages = "com.insurtech.commissions")
public class CommissionsServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(CommissionsServiceApplication.class, args);
  }
}
