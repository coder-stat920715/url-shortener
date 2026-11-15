package com.shortener.create;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.shortener")
public class CreationApplication {
    public static void main(String[] args) {
        SpringApplication.run(CreationApplication.class, args);
    }
}
