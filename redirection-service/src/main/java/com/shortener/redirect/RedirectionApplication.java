package com.shortener.redirect;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.shortener")
public class RedirectionApplication {
    public static void main(String[] args) {
        SpringApplication.run(RedirectionApplication.class, args);
    }
}
