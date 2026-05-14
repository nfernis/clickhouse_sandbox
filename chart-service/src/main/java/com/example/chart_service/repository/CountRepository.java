package com.example.chart_service.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CountRepository {

    private final JdbcTemplate jdbcTemplate;

    public CountRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long getRecordCount() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM weekly_epid_stats",
                Long.class
        );
    }

    public Long getTotalUniquePatients() {
        return jdbcTemplate.queryForObject(
                "SELECT uniqCombinedMerge(unique_patients) FROM weekly_epid_stats",
                Long.class
        );
    }
}