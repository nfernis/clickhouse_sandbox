package com.example.chart_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.time.format.DateTimeFormatter;
import java.time.ZonedDateTime;

@Service
@RequiredArgsConstructor
public class EpidemicMetricsService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final DateTimeFormatter HOUR_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHH");

    // === Сценарий 1: Уникальные пациенты (HyperLogLog) ===

    public void recordUniquePatient(String municipalityId, String icd10Group, String patientId) {
        String key = buildKey("unique", municipalityId, icd10Group);
        redisTemplate.opsForHyperLogLog().add(key, patientId);
        // TTL ставим только при первом создании ключа
        if (Boolean.FALSE.equals(redisTemplate.hasKey(key))) {
            redisTemplate.expire(key, java.time.Duration.ofHours(2));
        }
    }

    public Long countUniquePatients(String municipalityId, String icd10Group) {
        String key = buildKey("unique", municipalityId, icd10Group);
        return redisTemplate.opsForHyperLogLog().size(key);
    }

    // === Сценарий 2: Кэш недельного отчёта (String + JSON) ===

    public void cacheWeeklyReport(String jsonReport, boolean isCurrent) {
        String key = isCurrent ? "epid:week:current" : "epid:week:previous";
        long ttl = isCurrent ? 86400 : 604800; // 1 день / 7 дней
        redisTemplate.opsForValue().set(key, jsonReport, java.time.Duration.ofSeconds(ttl));
    }

    public String getWeeklyReport(boolean isCurrent) {
        String key = isCurrent ? "epid:week:current" : "epid:week:previous";
        return (String) redisTemplate.opsForValue().get(key);
    }

    // === Сценарий 3: Экстренные обращения (Hash) ===

    public void recordEmergencyVisit(String municipalityId, boolean isChild) {
        String key = buildEmergencyKey(municipalityId);
        redisTemplate.opsForHash().increment(key, "emergency_total", 1);
        if (isChild) {
            redisTemplate.opsForHash().increment(key, "emergency_children_0_6", 1);
        }
        // TTL при первом создании
        if (Boolean.FALSE.equals(redisTemplate.hasKey(key))) {
            redisTemplate.expire(key, java.time.Duration.ofHours(2));
        }
    }

    public EmergencyStats getEmergencyStats(String municipalityId) {
        String key = buildEmergencyKey(municipalityId);
        var hashOps = redisTemplate.opsForHash();
        Long total = (Long) hashOps.get(key, "emergency_total");
        Long children = (Long) hashOps.get(key, "emergency_children_0_6");
        return new EmergencyStats(
                total != null ? total : 0L,
                children != null ? children : 0L
        );
    }

    // === Вспомогательные методы ===

    private String buildKey(String metric, String municipalityId, String icd10Group) {
        String window = ZonedDateTime.now().format(HOUR_FORMAT);
        return String.format("epid:%s:%s:%s:%s", metric, municipalityId, icd10Group, window);
    }

    private String buildEmergencyKey(String municipalityId) {
        String window = ZonedDateTime.now().format(HOUR_FORMAT);
        return String.format("epid:emergency:%s:%s", municipalityId, window);
    }

    // ✅ Record вынесен на уровень класса (статический вложенный тип)
    public record EmergencyStats(Long total, Long children) {}
}