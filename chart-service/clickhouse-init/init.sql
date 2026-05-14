CREATE TABLE IF NOT EXISTS medical_visits (
    visit_date Date,
    patient_id UInt64,
    icd10_code LowCardinality(String),
    icd10_group LowCardinality(String) MATERIALIZED substring(icd10_code, 1, 3),
    age UInt8,
    age_group LowCardinality(String) MATERIALIZED multiIf(age < 18, '0-17', age < 30, '18-29', age < 45, '30-44', age < 60, '45-59', age < 75, '60-74', '75+'),
    sex Enum8('M' = 1, 'F' = 2),
    municipality_id UInt32,
    municipality_name LowCardinality(String)
) ENGINE = MergeTree
PARTITION BY toYYYYMM(visit_date)
ORDER BY (icd10_code, visit_date, municipality_id);

CREATE TABLE IF NOT EXISTS weekly_epid_stats (
    week_start Date,
    icd10_group String,
    municipality_id UInt32,
    unique_patients AggregateFunction(uniqCombined, UInt64)
) ENGINE = AggregatingMergeTree
ORDER BY (week_start, icd10_group, municipality_id);

CREATE MATERIALIZED VIEW IF NOT EXISTS mv_weekly_epid TO weekly_epid_stats AS
SELECT
    toMonday(visit_date) AS week_start,
    substring(icd10_code, 1, 3) AS icd10_group,
    municipality_id,
    uniqCombinedState(patient_id) AS unique_patients
FROM medical_visits
WHERE notEmpty(icd10_code)
  AND icd10_code NOT LIKE 'Z%'
  AND icd10_code NOT LIKE 'R%'
  AND icd10_code NOT LIKE 'V%'
  AND icd10_code NOT LIKE 'W%'
  AND icd10_code NOT LIKE 'X%'
  AND icd10_code NOT LIKE 'Y%'
GROUP BY week_start, icd10_group, municipality_id
ORDER BY week_start, icd10_group, municipality_id;



