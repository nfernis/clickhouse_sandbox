package com.example.chart_service.service;

import com.example.chart_service.dto.WeeklyEpidData;
import com.example.chart_service.repository.WeeklyEpidRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Service
public class WeeklyEpidService {

    private final WeeklyEpidRepository repository;
    private final RedisCacheService cache;

    public WeeklyEpidService(WeeklyEpidRepository repository, RedisCacheService cache) {
        this.repository = repository;
        this.cache = cache;
    }


    public List<WeeklyEpidData> getDataForWeek(String weekParam, List<Long> municipalityIds) {
        //сначала идем в редис
        List<WeeklyEpidData> cached = cache.get(weekParam);
        if (cached != null) {
            return cached; // ⚡ Вернули мгновенно
        }

        LocalDate today = LocalDate.now();
        LocalDate targetWeek = "current".equals(weekParam)
                ? today.with(DayOfWeek.MONDAY)
                : today.with(DayOfWeek.MONDAY).minusWeeks(1);
        //если в редисе нет то идем в кх
        List<WeeklyEpidData> result = repository.findByWeekStartAndMunicipality(targetWeek, municipalityIds);

        //Сохраняем в Redis на будущее
        cache.put(weekParam, result);

        return result;
    }

    public List<WeeklyEpidData> getWeeklyData(String icd10Group, Long municipalityId, int weeks) {
        return repository.findWeeklyData(icd10Group, municipalityId, weeks);
    }
    public List<WeeklyEpidData> getLastWeekByMunicipality(List<Long> municipalityIds) {
        return repository.findLastWeekByMunicipality(municipalityIds);
    }
}