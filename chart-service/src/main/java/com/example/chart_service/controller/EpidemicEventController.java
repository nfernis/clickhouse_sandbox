package com.example.chart_service.controller;

import com.example.chart_service.service.EmergencyCodeValidator;
import com.example.chart_service.service.EpidemicMetricsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/epidemic")
@RequiredArgsConstructor
@Slf4j
public class EpidemicEventController {

    private final EpidemicMetricsService metricsService;
    private final EmergencyCodeValidator emergencyValidator; // см. ниже
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * Уникальные пациенты по ВСЕМ муниципалитетам за указанный час
     */
    @GetMapping("/hourly/unique")
    public List<HourlyStatsDto> getHourlyUniqueStats(@RequestParam String period) {
        String hourKey = getHourWindowKey(period);
        List<HourlyStatsDto> result = new ArrayList<>();

        // ✅ Перебираем ВСЕ 120 регионов (как в твоём справочнике)
        for (int munId = 1; munId <= 120; munId++) {
            // ✅ Перебираем все группы МКБ, которые могут быть в данных
            List<String> icd10Groups = List.of("J00","J01","A00","T50","I21","I63","Z00","R50","S06","O60");

            for (String icd10 : icd10Groups) {
                String redisKey = String.format("epid:unique:%d:%s:%s", munId, icd10, hourKey);

                try {
                    Long count = redisTemplate.opsForHyperLogLog().size(redisKey);
                    if (count != null && count > 0) {
                        result.add(new HourlyStatsDto(munId, icd10, count, period));
                    }
                } catch (Exception e) {
                    // Если ключа нет или ошибка — просто пропускаем
                    log.debug("No data for key: {}", redisKey);
                }
            }
        }

        log.info("Returned {} unique patient records for period {}", result.size(), period);
        return result;
    }

    /**
     * Экстренные обращения по ВСЕМ муниципалитетам за указанный час
     */
    @GetMapping("/hourly/emergency")
    public List<HourlyStatsDto> getHourlyEmergencyStats(@RequestParam String period) {
        String hourKey = getHourWindowKey(period);
        List<HourlyStatsDto> result = new ArrayList<>();

        for (int munId = 1; munId <= 120; munId++) {
            String redisKey = String.format("epid:emergency:%d:%s", munId, hourKey);

            try {
                // Читаем ВСЕ поля из хеша
                Map<Object, Object> hash = redisTemplate.opsForHash().entries(redisKey);

                if (hash != null && !hash.isEmpty()) {
                    // Пробуем разные варианты имени поля (на случай опечаток)
                    Object value = hash.get("emergency_total");
                    Long total = null;

                    if (value instanceof Long) {
                        total = (Long) value;
                    } else if (value instanceof Integer) {
                        total = ((Integer) value).longValue();
                    } else if (value instanceof String) {
                        total = Long.valueOf((String) value);
                    } else if (value != null) {
                        // На всякий случай: пробуем распарсить любой объект
                        total = Long.valueOf(value.toString());
                    }

                    if (total != null && total > 0) {
                        result.add(new HourlyStatsDto(munId, "EMERGENCY", total, period));
                    }
                }
            } catch (Exception e) {
                log.debug("No emergency data for key: {}", redisKey);
            }
        }

        log.info("Returned {} emergency records for period {}", result.size(), period);
        return result;
    }

    // Вспомогательный метод: вычисляет ключ часа для периода
    private String getHourWindowKey(String period) {
        // Для демо: фиксированные часы, чтобы графики всегда показывали данные
        // В прода: LocalDateTime.now().minusHours(1).format(DateTimeFormatter.ofPattern("yyyyMMddHH"))

        if ("previous".equals(period)) {
            return "2026052114"; // 14:00–14:59, 21 мая 2026
        } else {
            return "2026052115"; // 15:00–15:59
        }
    }

    // DTO (можно вынести в отдельный файл)
    public record HourlyStatsDto(
            Integer municipalityId,
            String icd10Group,
            Long uniquePatientsCount,
            String period
    ) {}
    @PostMapping("/test/update-weekly-cache")
    public String updateWeeklyCache() {
        try {
            log.info("Starting cache update from ClickHouse...");

            String sql = """
            SELECT 
                toMonday(visit_date) as weekStart, 
                substring(icd10_code, 1, 3) as icd10Group,
                municipality_id as municipalityId, 
                uniqCombined(patient_id) as uniquePatientsCount 
            FROM medical_visits 
            WHERE visit_date >= '2026-05-18' AND visit_date <= '2026-05-24'
              AND notEmpty(icd10_code)
            GROUP BY weekStart, icd10Group, municipality_id 
            ORDER BY uniquePatientsCount DESC
            """;

            // 1. Получаем данные из ClickHouse
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
            log.info("Fetched {} rows from ClickHouse", results.size());

            if (results.isEmpty()) {
                return "❌ No data found for this week. Check dates.";
            }

            // 2. 🔥 ВАЖНО: Конвертируем LocalDate в String вручную!
            // Jackson не умеет в LocalDate без настроек, поэтому делаем это сами.
            for (Map<String, Object> row : results) {
                Object weekStart = row.get("weekStart");
                if (weekStart != null) {
                    // Просто вызываем toString() у даты, получаем "2026-05-18"
                    row.put("weekStart", weekStart.toString());
                }
            }

            // 3. Сериализуем в JSON (теперь там только строки, числа и списки — это Jackson умеет)
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(results);

            // 4. Пишем в Redis
            redisTemplate.opsForValue().set("epid:week:current", json, Duration.ofDays(1));

            log.info("✅ Cache updated successfully. Size: {} chars", json.length());
            return "✅ Success! Updated " + results.size() + " records. Size: " + json.length() + " bytes";

        } catch (Exception e) {
            log.error("Failed to update cache", e);
            return "❌ Error: " + e.getMessage();
        }
    }

    // 🔹 ТОЛЬКО ДЛЯ ОТЛАДКИ — удалить перед продакшеном!
    @GetMapping("/test/create-keys")
    public ResponseEntity<String> createTestKeys() {
        try {
            // Принудительно создаём ключи для муниципалитета "77"
            metricsService.recordUniquePatient("77", "POISONING", "test_patient_001");
            metricsService.recordUniquePatient("77", "POISONING", "patient_002");
            metricsService.recordEmergencyVisit("77", true); // isChild = true

            return ResponseEntity.ok(" Keys created: epid:unique:77:POISONING:XXXX, epid:emergency:77:XXXX");
        } catch (Exception e) {
            log.error("Failed to create test keys", e);
            return ResponseEntity.status(500).body(" Error: " + e.getMessage());
        }
    }
    // === Приём события от медучреждения ===

    @PostMapping("/event")
    public void receiveEvent(@RequestBody EpidemicEvent event) {
        // 1. Валидация
        if (!isValid(event)) {
            log.warn("Invalid event: {}", event);
            return;
        }

        // 2. Запись в Redis (атомарно, без блокировок)
        metricsService.recordUniquePatient(
                event.municipalityId(),
                event.icd10Group(),
                event.patientId()
        );

        // 3. Если экстренный случай — обновляем Hash-счётчик
        if (emergencyValidator.isEmergency(event.icd10Code())) {
            boolean isChild = event.age() != null && event.age() < 7;
            metricsService.recordEmergencyVisit(event.municipalityId(), isChild);
        }

        // 4. (Опционально) Отложить событие для асинхронной записи в ClickHouse
        // asyncWorker.buffer(event);
    }

    // === Чтение метрик для дашборда ===

    @GetMapping("/unique/{municipality}/{icd10Group}")
    public Long getUniqueCount(@PathVariable String municipality, @PathVariable String icd10Group) {
        return metricsService.countUniquePatients(municipality, icd10Group);
    }

    @GetMapping("/emergency/{municipality}")
    public EpidemicMetricsService.EmergencyStats getEmergencyStats(@PathVariable String municipality) {
        return metricsService.getEmergencyStats(municipality);
    }

    @GetMapping("/weekly-report")
    public String getWeeklyReport(@RequestParam(defaultValue = "false") boolean current) {
        String report = metricsService.getWeeklyReport(current);
        return report != null ? report : "{\"error\":\"not_found\"}";
    }

    // === Валидация (упрощённо) ===

    private boolean isValid(EpidemicEvent event) {
        return event.municipalityId() != null
                && event.icd10Code() != null
                && event.patientId() != null;
    }

    // DTO для входящего события
    public record EpidemicEvent(
            String municipalityId,
            String icd10Code,
            String icd10Group,
            String patientId,
            Integer age,
            String sex,
            String visitTimestamp
    ) {}
}