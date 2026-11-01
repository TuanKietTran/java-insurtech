package com.insurtech.rules.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Deployable entry point of the {@code rules} bounded context (PRD-F05). */
@SpringBootApplication(scanBasePackages = "com.insurtech.rules")
public class RulesServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(RulesServiceApplication.class, args);
  }
}
