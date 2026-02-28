package com.thurman.consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OutboxConsumerServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OutboxConsumerServiceApplication.class, args);
    }
}