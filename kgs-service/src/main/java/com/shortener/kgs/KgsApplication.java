package com.shortener.kgs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.shortener")
public class KgsApplication {
    public static void main(String[] args) {
        SpringApplication.run(KgsApplication.class, args);
    }
}
