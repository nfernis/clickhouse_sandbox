package com.example.chart_service.service;

import com.example.chart_service.dto.WeeklyEpidData;
import com.example.chart_service.repository.WeeklyEpidRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WeeklyEpidService {

    private final WeeklyEpidRepository repository;

    public WeeklyEpidService(WeeklyEpidRepository repository) {
        this.repository = repository;
    }

    public List<WeeklyEpidData> getWeeklyData(String icd10Group, Long municipalityId, int weeks) {
        return repository.findWeeklyData(icd10Group, municipalityId, weeks);
    }
    public List<WeeklyEpidData> getLastWeekByMunicipality(List<Long> municipalityIds) {
        return repository.findLastWeekByMunicipality(municipalityIds);
    }
}