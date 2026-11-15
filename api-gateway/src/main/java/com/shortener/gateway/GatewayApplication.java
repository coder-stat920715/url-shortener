package com.shortener.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Edge routing. Add JWT validation (spring-security-oauth2-resource-server) and a filter that
 * copies the verified subject into the X-User-Id header before forwarding to url-creation-service.
 */
@SpringBootApplication
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
