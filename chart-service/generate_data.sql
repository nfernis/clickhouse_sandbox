-- 🔹 Очищаем таблицу перед генерацией (на всякий случай)
TRUNCATE TABLE IF EXISTS medical_visits;

-- 🔹 Генерация 80 млн записей
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
    -- 1. Дата: случайный день за последние 2 года (730 дней)
    today() - number % 730 AS visit_date,

    -- 2. ID пациента: от 1 до 5 млн (чтобы были повторения пациентов)
    (number % 5000000) + 1 AS patient_id,

    -- 3. Код МКБ-10: берем из списка популярных кодов
    arrayElement(
        ['A00', 'A09', 'B34', 'J00', 'J06', 'J10', 'J18', 'I10', 'I20', 'I25', 'E11', 'K21', 'N39', 'M54', 'R50'],
        (number % 15) + 1
    ) AS icd10_code,

    -- 4. Возраст: от 0 до 90 лет (с перекосом в сторону взрослых)
    toUInt8((number % 91)) AS age,

    -- 5. Пол: M или F
    if(number % 2 = 0, 'M', 'F') AS sex,

    -- 6. ID муниципалитета: от 1 до 120
    toUInt32((number % 120) + 1) AS municipality_id,

    -- 7. Название муниципалитета (просто "Region-{ID}")
    concat('Region-', toString(municipality_id)) AS municipality_name

FROM numbers_mt(80000000); -- 80 миллионов строк