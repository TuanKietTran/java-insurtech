package com.insurtech.search.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Deployable entry point of the {@code search} bounded context (PRD-S01). */
@SpringBootApplication(scanBasePackages = "com.insurtech.search")
public class SearchServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(SearchServiceApplication.class, args);
  }
}
