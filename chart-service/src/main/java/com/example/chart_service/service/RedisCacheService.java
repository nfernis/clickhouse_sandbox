package com.example.chart_service.service;

import com.example.chart_service.dto.WeeklyEpidData;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class RedisCacheService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String KEY_PREFIX = "epid:week:";
    private static final long SHORT_TTL_MINUTES = 5; // Для текущей недели

    public RedisCacheService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public List<WeeklyEpidData> get(String weekParam) {
        String key = KEY_PREFIX + weekParam;
        String json = redisTemplate.opsForValue().get(key);

        if (json != null) {
            try {
                System.out.println("✅ CACHE HIT: " + key);
                return objectMapper.readValue(json, new TypeReference<List<WeeklyEpidData>>() {});
            } catch (Exception e) {
                redisTemplate.delete(key);
            }
        }
        return null;
    }

    public void put(String weekParam, List<WeeklyEpidData> data) {
        if (data == null || data.isEmpty()) return;

        String key = KEY_PREFIX + weekParam;
        long ttlSeconds = calculateTTL(weekParam);

        try {
            String json = objectMapper.writeValueAsString(data);
            redisTemplate.opsForValue().set(key, json, ttlSeconds, TimeUnit.SECONDS);
            System.out.println("💾 CACHE SET: " + key + " (TTL: " + ttlSeconds/60 + " мин)");
        } catch (Exception e) {
            System.err.println("⚠️ Cache write failed: " + e.getMessage());
        }
    }

    // 🔥 Умный расчёт TTL
    private long calculateTTL(String weekParam) {
        if ("current".equals(weekParam)) {
            // 🔹 Текущая неделя: 5 минут (данные могут обновляться)
            return SHORT_TTL_MINUTES * 60;

        } else {
            // 🔹 Прошлая неделя: до конца текущей недели (данные не изменятся)
            LocalDate today = LocalDate.now();
            LocalDateTime endOfWeek = today
                    .with(DayOfWeek.SUNDAY)      // Воскресенье текущей недели
                    .atTime(23, 59, 59);         // Конец дня

            long secondsUntilEndOfWeek = Duration.between(LocalDateTime.now(), endOfWeek).getSeconds();

            // На всякий случай: минимум 1 день, максимум 7 дней
            return Math.max(86400, Math.min(604800, secondsUntilEndOfWeek));
        }
    }
}