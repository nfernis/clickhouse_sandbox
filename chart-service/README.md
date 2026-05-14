Вот готовый `README.md`, составленный на основе нашего диалога. Все команды продублированы для **Bash** (Linux/macOS) и **PowerShell** (Windows). Вы можете просто скопировать этот текст и заменить им текущий файл в репозитории.

```markdown
# 📊 ClickHouse Chart Service Sandbox

Учебный проект для демонстрации работы Java-сервиса с ClickHouse. Включает генерацию тестовых данных (~80 млн записей), запуск через Docker Compose и REST API для визуализации.

---

## 📦 Требования
- Docker Desktop + Docker Compose v2
- Git (для клонирования репозитория)
- 8+ ГБ свободной памяти (рекомендуется для генерации данных)

---

## 🚀 Быстрый старт

### 1️⃣ Перейдите в директорию сервиса
Все конфигурационные файлы (`docker-compose.yml`, `Dockerfile`) находятся внутри папки `chart-service`.

**Bash / Linux / macOS:**
```bash
cd clickhouse_sandbox/chart-service
```

**PowerShell (Windows):**
```powershell
cd clickhouse_sandbox\chart-service
```

---

### 2️⃣ Запустите контейнеры
Сборка и запуск ClickHouse + Java-приложения:

```bash
docker-compose up --build -d
```
⏳ *Дождитесь полной инициализации ClickHouse (обычно 20–40 секунд).*  
Проверить готовность: `docker-compose ps` (статус `Up` для обоих сервисов).

---

### 3️⃣ Создайте таблицы (DDL)
Схема БД находится в `clickhouse-init/init.sql`.

**Bash / Linux / macOS:**
```bash
cat clickhouse-init/init.sql | docker-compose exec -T clickhouse clickhouse-client --multiquery
```

**PowerShell (Windows):**
```powershell
Get-Content clickhouse-init\init.sql | docker-compose exec -T clickhouse clickhouse-client --multiquery
```

---

### 4️⃣ Загрузите тестовые данные
Генерация и вставка ~80 млн строк через `numbers_mt`.

**Bash / Linux / macOS:**
```bash
cat generate_data.sql | docker-compose exec -T clickhouse clickhouse-client --multiquery
```

**PowerShell (Windows):**
```powershell
Get-Content generate_data.sql | docker-compose exec -T clickhouse clickhouse-client --multiquery
```
⏱️ *Выполнение занимает 1–4 минуты. Отсутствие вывода в терминале — норма, не закрывайте окно до появления приглашения командной строки.*

---

### 5️⃣ Проверка работоспособности
1. **Количество строк в таблице:**
   ```bash
   docker-compose exec clickhouse clickhouse-client --query "SELECT count() FROM medical_visits"
   ```
   ✅ Ожидаемый результат: `80000000`

2. **Статус Java-сервиса:**
   Откройте в браузере: `http://localhost:8080/actuator/health`  
   ✅ Ожидаемый ответ: `{"status":"UP"}`

---

## 🐛 Частые проблемы и решения

| Ошибка / Симптом | Причина | Решение |
|------------------|---------|---------|
| `no configuration file provided: not found` | Запуск не из папки `chart-service` | Перейдите в `cd chart-service` перед выполнением команд |
| `Оператор "<" зарезервирован...` (PowerShell) | PS не поддерживает `bash`-редирект `<` | Используйте `Get-Content файл.sql \| docker-compose exec -T ...` |
| `SYNTAX_ERROR` на `;` | `clickhouse-client` не поддерживает мультизапросы по умолчанию | Добавьте флаг `--multiquery` к команде `clickhouse-client` |
| `service "clickhouse" is not running` | Контейнер ещё не успел стартовать | Проверьте `docker-compose ps`, дождитесь статуса `Up` (20-40 сек) |
| `time="..." level=warning ... version is obsolete` | Docker Compose v2 игнорирует поле `version:` | Безопасно игнорировать. Можно удалить строку `version: '3.x'` из `docker-compose.yml` |

---

## 🛠 Полезные команды

| Действие | Команда |
|----------|---------|
| Просмотр логов сервиса | `docker-compose logs -f app` или `... clickhouse` |
| Остановка и очистка контейнеров + томов | `docker-compose down -v` |
| Проверка статуса контейнеров | `docker-compose ps` |
| Подключение к CLI ClickHouse | `docker-compose exec clickhouse clickhouse-client` |

---

## 📝 Примечания
- Флаг `-T` в `docker-compose exec -T` обязателен при передаче данных через pipe, иначе Docker пытается выделить псевдо-терминал и блокирует ввод.
- Генерация данных выполняется на стороне ClickHouse (`numbers_mt`), поэтому нагрузка ложится на CPU контейнера, а не на хост-машину.
- При нехватке памяти увеличьте лимиты в Docker Desktop: ⚙️ `Resources` → `Memory` ≥ 4GB.
