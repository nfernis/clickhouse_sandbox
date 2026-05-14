TRUNCATE TABLE IF EXISTS medical_visits;
INSERT INTO medical_visits (
    visit_date,
    patient_id,
    icd10_code,
    age,
    sex,
    municipality_id,
    municipality_name
)
SELECT
    today() - number % 730 AS visit_date,
    (number % 5000000) + 1 AS patient_id,
    arrayElement(
        ['A00', 'A09', 'B34', 'J00', 'J06', 'J10', 'J18', 'I10', 'I20', 'I25', 'E11', 'K21', 'N39', 'M54', 'R50'],
        (number % 15) + 1
    ) AS icd10_code,
    toUInt8((number % 91)) AS age,
    if(number % 2 = 0, 'M', 'F') AS sex,
    toUInt32((number % 120) + 1) AS municipality_id,
    concat('Region-', toString(municipality_id)) AS municipality_name
FROM numbers_mt(80000000);