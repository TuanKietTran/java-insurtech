package com.insurtech.regreporting.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Deployable entry point of the {@code regreporting} bounded context (PRD-R02). */
@SpringBootApplication(scanBasePackages = "com.insurtech.regreporting")
public class RegReportingServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(RegReportingServiceApplication.class, args);
  }
}
