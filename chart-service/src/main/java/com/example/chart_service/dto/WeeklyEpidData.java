package com.example.chart_service.dto;

import java.time.LocalDate;

public record WeeklyEpidData(
        LocalDate weekStart,
        String icd10Group,
        Long municipalityId,
        Long uniquePatientsCount
) {}