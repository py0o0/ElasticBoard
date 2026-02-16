package com.example.escommunity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EScommunityApplication {

    public static void main(String[] args) {
        SpringApplication.run(EScommunityApplication.class, args);
    }

}
