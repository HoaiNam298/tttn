package com.project.shopapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@org.springframework.scheduling.annotation.EnableScheduling
public class ShopappApplication {
    public static void main(String[] args) {
        SpringApplication.run(ShopappApplication.class, args);
    }
}
