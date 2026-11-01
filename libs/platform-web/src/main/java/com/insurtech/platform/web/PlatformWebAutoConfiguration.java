package com.insurtech.platform.web;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

/** Registers the platform HTTP edge in every servlet-based service and BFF. */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class PlatformWebAutoConfiguration {

  @Bean
  CorrelationIdFilter correlationIdFilter() {
    return new CorrelationIdFilter();
  }

  @Bean
  IdempotencyKeyRequiredFilter idempotencyKeyRequiredFilter() {
    return new IdempotencyKeyRequiredFilter();
  }

  @Bean
  DomainExceptionHandler domainExceptionHandler() {
    return new DomainExceptionHandler();
  }
}
