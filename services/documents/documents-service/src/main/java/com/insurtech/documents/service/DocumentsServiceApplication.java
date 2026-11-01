package com.insurtech.documents.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Deployable entry point of the {@code documents} bounded context (PRD-F06). */
@SpringBootApplication(scanBasePackages = "com.insurtech.documents")
public class DocumentsServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(DocumentsServiceApplication.class, args);
  }
}
