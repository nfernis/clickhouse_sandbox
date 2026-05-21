package com.example.chart_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ChartServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ChartServiceApplication.class, args);
    }
}