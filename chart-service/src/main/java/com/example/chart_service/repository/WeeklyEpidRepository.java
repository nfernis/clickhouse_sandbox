package com.example.chart_service.repository;

import com.example.chart_service.dto.WeeklyEpidData;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public class WeeklyEpidRepository {

    private final JdbcTemplate jdbcTemplate;

    public WeeklyEpidRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<WeeklyEpidData> findWeeklyData(String icd10Group, Long municipalityId, int weeks) {
        StringBuilder sql = new StringBuilder("""
            SELECT 
                week_start,
                icd10_group,
                municipality_id,
                uniqCombinedMerge(unique_patients) as patient_count
            FROM weekly_epid_stats
            WHERE 1=1
            """);

        List<Object> params = new java.util.ArrayList<>();

        if (icd10Group != null && !icd10Group.isBlank()) {
            sql.append(" AND icd10_group = ?");
            params.add(icd10Group);
        }
        if (municipalityId != null && municipalityId > 0) {
            sql.append(" AND municipality_id = ?");
            params.add(municipalityId);
        }

        sql.append(" GROUP BY week_start, icd10_group, municipality_id");
        sql.append(" ORDER BY week_start ASC");

        if (weeks > 0) {
            sql.append(" LIMIT ?");
            params.add(weeks);
        }

        return jdbcTemplate.query(
                sql.toString(),
                (rs, rowNum) -> new WeeklyEpidData(
                        rs.getObject("week_start", LocalDate.class),
                        rs.getString("icd10_group"),
                        rs.getLong("municipality_id"),
                        rs.getLong("patient_count")
                ),
                params.toArray()
        );
    }
    public List<WeeklyEpidData> findLastWeekByMunicipality(List<Long> municipalityIds) {
        StringBuilder sql = new StringBuilder("""
        SELECT 
            week_start,
            icd10_group,
            municipality_id,
            uniqCombinedMerge(unique_patients) as patient_count
        FROM weekly_epid_stats
        WHERE week_start = (SELECT MAX(week_start) FROM weekly_epid_stats)
        """);

        List<Object> params = new java.util.ArrayList<>();

        if (municipalityIds != null && !municipalityIds.isEmpty()) {
            sql.append(" AND municipality_id IN (");
            sql.append(String.join(",", java.util.Collections.nCopies(municipalityIds.size(), "?")));
            sql.append(")");
        }

        sql.append(" GROUP BY week_start, icd10_group, municipality_id ORDER BY week_start ASC");

        return jdbcTemplate.query(
                sql.toString(),
                (rs, rowNum) -> new WeeklyEpidData(
                        rs.getObject("week_start", java.time.LocalDate.class),
                        rs.getString("icd10_group"),
                        rs.getLong("municipality_id"),
                        rs.getLong("patient_count")
                ),
                municipalityIds != null ? municipalityIds.toArray() : new Object[0]
        );
    }
}