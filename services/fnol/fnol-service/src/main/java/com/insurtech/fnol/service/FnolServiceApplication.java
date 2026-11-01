package com.insurtech.fnol.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Deployable entry point of the {@code fnol} bounded context (PRD-C01). */
@SpringBootApplication(scanBasePackages = "com.insurtech.fnol")
public class FnolServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(FnolServiceApplication.class, args);
  }
}
