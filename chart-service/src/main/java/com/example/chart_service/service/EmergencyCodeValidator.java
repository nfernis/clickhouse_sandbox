package com.example.chart_service.service;

import org.springframework.stereotype.Component;
import java.util.Set;

@Component
public class EmergencyCodeValidator {

    // Allow-list: префиксы глав МКБ-10 + точные коды
    private static final Set<String> EMERGENCY_PREFIXES = Set.of("S", "T", "R", "I21", "I63", "O60");
    private static final Set<String> EMERGENCY_EXACT = Set.of("T50.9", "R57.0", "A00", "J00");

    public boolean isEmergency(String icd10Code) {
        if (icd10Code == null) return false;
        // Проверка по префиксу (первые 1-3 символа)
        for (String prefix : EMERGENCY_PREFIXES) {
            if (icd10Code.startsWith(prefix)) return true;
        }
        // Проверка по точному совпадению
        return EMERGENCY_EXACT.contains(icd10Code);
    }
}