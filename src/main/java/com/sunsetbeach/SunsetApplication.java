package com.sunsetbeach;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// EnableScheduling drives PrintService's @Scheduled background retry sweep for PENDING print jobs.
@SpringBootApplication
@EnableScheduling
public class SunsetApplication {

    public static void main(String[] args) {
        SpringApplication.run(SunsetApplication.class, args);
    }

}
