package com.insurtech.notifications.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Deployable entry point of the {@code notifications} bounded context (PRD-X04). */
@SpringBootApplication(scanBasePackages = "com.insurtech.notifications")
public class NotificationsServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(NotificationsServiceApplication.class, args);
  }
}
