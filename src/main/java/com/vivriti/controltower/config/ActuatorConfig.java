package com.vivriti.controltower.config;

import org.springframework.boot.actuate.web.exchanges.HttpExchangeRepository;
import org.springframework.boot.actuate.web.exchanges.InMemoryHttpExchangeRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration bean for Spring Boot 3 Actuator observability.
 * Exposes HttpExchangeRepository to track HTTP traces, request latencies,
 * and status codes for each and every step of execution.
 */
@Configuration
public class ActuatorConfig {

    @Bean
    public HttpExchangeRepository httpExchangeRepository() {
        InMemoryHttpExchangeRepository repository = new InMemoryHttpExchangeRepository();
        repository.setCapacity(1000);
        return repository;
    }
}

