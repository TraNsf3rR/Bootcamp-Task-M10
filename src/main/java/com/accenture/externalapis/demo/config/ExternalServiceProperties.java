package com.accenture.externalapis.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "external-service")
public record ExternalServiceProperties(String baseUrl) {
}