package com.example.chart_service.service;

import com.example.chart_service.repository.CountRepository;
import org.springframework.stereotype.Service;

@Service
public class CountService {

    private final CountRepository repository;

    public CountService(CountRepository repository) {
        this.repository = repository;
    }

    public Long getRecordCount() {
        return repository.getRecordCount();
    }
}