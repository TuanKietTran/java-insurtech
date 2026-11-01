package com.insurtech.rating.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Deployable entry point of the {@code rating} bounded context (PRD-P03). */
@SpringBootApplication(scanBasePackages = "com.insurtech.rating")
public class RatingServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(RatingServiceApplication.class, args);
  }
}
