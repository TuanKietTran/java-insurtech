package com.insurtech.vendors.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Deployable entry point of the {@code vendors} bounded context (PRD-V01). */
@SpringBootApplication(scanBasePackages = "com.insurtech.vendors")
public class VendorsServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(VendorsServiceApplication.class, args);
  }
}
