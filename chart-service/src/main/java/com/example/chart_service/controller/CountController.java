package com.example.chart_service.controller;

import com.example.chart_service.service.CountService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CountController {

    private final CountService service;

    public CountController(CountService service) {
        this.service = service;
    }

    @GetMapping("/count")
    public String getCount() {
        Long count = service.getRecordCount();
        return "Записей в weekly_epid_stats: " + count;
    }
}