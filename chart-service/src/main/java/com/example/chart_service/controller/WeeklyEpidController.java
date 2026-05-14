package com.example.chart_service.controller;

import com.example.chart_service.dto.WeeklyEpidData;
import com.example.chart_service.service.WeeklyEpidService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/epid")
public class WeeklyEpidController {

    private final WeeklyEpidService service;

    public WeeklyEpidController(WeeklyEpidService service) {
        this.service = service;
    }


    @GetMapping("/weekly")
    public List<WeeklyEpidData> getWeeklyEpidData(
            @RequestParam(required = false) String icd10_group,
            @RequestParam(required = false) Long municipality_id,
            @RequestParam(defaultValue = "8") int weeks
    ) {
        return service.getWeeklyData(icd10_group, municipality_id, weeks);
    }
    @GetMapping("/by-municipality")
    public List<WeeklyEpidData> getByMunicipality(
            @RequestParam(defaultValue = "previous") String week,
            @RequestParam(required = false) String municipality_ids) {

        List<Long> ids = municipality_ids != null
                ? Arrays.stream(municipality_ids.split(",")).map(Long::parseLong).collect(Collectors.toList())
                : null;

        return service.getDataForWeek(week, ids);
    }
}