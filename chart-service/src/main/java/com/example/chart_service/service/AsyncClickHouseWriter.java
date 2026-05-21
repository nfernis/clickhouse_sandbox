package com.example.chart_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.List;
import java.util.ArrayList;

@Component
@RequiredArgsConstructor
@Slf4j
public class AsyncClickHouseWriter {

    // Буфер для событий (в памяти)
    private final ConcurrentLinkedQueue<EpidemicEvent> buffer = new ConcurrentLinkedQueue<>();

    // DTO для входящего события (дублируем из контроллера, чтобы не создавать циклические зависимости)
    public record EpidemicEvent(
            String municipalityId,
            String icd10Code,
            String icd10Group,
            String patientId,
            Integer age,
            String sex,
            String visitTimestamp
    ) {}

    /**
     * Добавляет событие в буфер для асинхронной обработки.
     * Вызывается из контроллера после успешной записи в Redis.
     */
    public void buffer(EpidemicEvent event) {
        buffer.offer(event);
        log.debug("Event buffered: {}", event.patientId());
    }

    /**
     * Периодическая задача: забирает пачку событий из буфера и логирует их.
     * В продакшене здесь будет INSERT в ClickHouse.
     *
     * Запускается раз в 5 минут ИЛИ можно вызвать вручную для тестов.
     */
    @Scheduled(fixedRate = 300_000) // 5 минут
    public void flushToClickHouse() {
        if (buffer.isEmpty()) {
            log.debug("Buffer is empty, nothing to flush");
            return;
        }

        // Забираем пачку до 1000 событий
        List<EpidemicEvent> batch = new ArrayList<>();
        EpidemicEvent event;
        while ((event = buffer.poll()) != null && batch.size() < 1000) {
            batch.add(event);
        }

        if (!batch.isEmpty()) {
            // 🔹 MVP: просто логируем. В продакшене здесь будет:
            // chartRepository.insertBatch(batch);
            log.info("Flushed {} events to ClickHouse (MVP: logged only)", batch.size());
            for (EpidemicEvent e : batch) {
                log.debug("  → {} | {} | {}", e.municipalityId(), e.icd10Code(), e.patientId());
            }
        }
    }

    /**
     * Метод для ручной отладки: принудительно флэшит буфер.
     * Можно вызвать через Actuator или тест.
     */
    public void flushNow() {
        log.info("Manual flush triggered");
        flushToClickHouse();
    }

    /**
     * Возвращает размер буфера (для мониторинга).
     */
    public int getBufferSize() {
        return buffer.size();
    }
}