#  Chart Service — Визуализация эпидемиологической статистики

Веб-приложение на Spring Boot + ClickHouse. Показывает процент заболеваемости по регионам РФ.

🔗 Локальный адрес после запуска: `http://localhost:8080`

---

## 🚀 Быстрый старт

### Требования
- Docker ≥ 20.10
- Docker Compose ≥ 2.0
- Git

### 1. Клонируй репозиторий
```bash
git clone https://github.com/nfernis/clickhouse_sandbox.git
cd chart-service
```

### 2. Запусти сервисы
```bash
docker-compose up --build -d
```

### 3. Подожди ~30 секунд
Таблицы создадутся автоматически из `init.sql`.

### 4. Открой в браузере
👉 `http://localhost:8080`

> ️ График будет пустым, пока не наполнишь базу данными (см. раздел «Данные»).

---

## 🗄️ Данные

Таблицы `medical_visits` и `weekly_epid_stats` уже созданы (см. `init.sql`).

### Вариант А: Сгенерировать тестовые данные (80 млн записей)

1. Выполни скрипт генерации:
   ```bash
   docker-compose exec clickhouse clickhouse-client < generate_data.sql
   ```
   *(Займёт 2-5 минут)*

2. Проверь, что данные появились:
   ```bash
   docker-compose exec clickhouse clickhouse-client --query "SELECT count() FROM weekly_epid_stats"
   ```

3. Обнови страницу графика — готово! 🎉

### Вариант Б: Вставить свои данные

1. Подключись к ClickHouse:
   ```bash
   docker-compose exec clickhouse clickhouse-client
   ```

2. Выполняй свои `INSERT`-запросы:
   ```sql
   INSERT INTO medical_visits VALUES (...);
   ```

3. Данные в `weekly_epid_stats` появятся автоматически (сработает Materialized View).

---

## ⚙️ Конфигурация

### Переменные окружения (в `docker-compose.yml`)

| Переменная | Значение по умолчанию                                  | Описание |
|-----------|--------------------------------------------------------|----------|
| `SPRING_DATASOURCE_URL` | `jdbc:clickhouse://clickhouse:8123/default?compress=0` | URL подключения к БД |
| `SPRING_DATASOURCE_USERNAME` | `default`                                              | Пользователь БД |
| `SPRING_DATASOURCE_PASSWORD` | `test123123`                                             | Пароль БД |



### Порты

| Порт | Сервис | Описание |
|------|--------|----------|
| `8080` | chart-service | Веб-интерфейс (график) |
| `8123` | clickhouse | HTTP-интерфейс БД (для запросов) |
| `9000` | clickhouse | Native-порт (для JDBC) |

---

## 🔌 API Эндпоинты

| Метод | Путь | Описание |
|-------|------|----------|
| `GET` | `/api/epid/by-municipality` | Данные для графика (ID муниципалитета + кол-во пациентов) |

**Пример ответа:**
```json
[
  {"municipalityId": 77, "uniquePatientsCount": 2837446},
  {"municipalityId": 64, "uniquePatientsCount": 5346256}
]
```

---


```
chart-service/
├── src/                     # Исходный код Spring Boot
├── init.sql                 # DDL: CREATE TABLE / VIEW
├── generate_data.sql        # Скрипт генерации 80 млн тестовых записей
├── Dockerfile               # Сборка приложения
├── docker-compose.yml       # Запуск всего стека
├── README.md                # Этот файл
└── .gitignore              # Исключает *.tsv, target/, и т.д.
```
