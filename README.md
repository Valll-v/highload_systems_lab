# Платформа бронирования билетов

Spring Boot 3, Java 21, PostgreSQL, Liquibase.

## Запуск

Всё в Docker (сборка jar внутри образа, Java локально не нужна):

```bash
docker compose up --build
```

Приложение на `http://localhost:8080`, PostgreSQL на `localhost:5432`.

Локально, только БД в Docker:

```bash
docker compose up -d postgres
mvn spring-boot:run
```

Настройки БД: переменные `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`
(дефолты — `localhost:5432/ticketing`, `ticketing/ticketing`).

Тесты (`mvn test`) используют встроенный PostgreSQL, Docker для них не нужен.

## Роли

Пользователь передаётся заголовком `X-User-Id`. Роли: `CUSTOMER`, `ORGANIZER`, `ADMIN`.

| Роль      | Что может                                                        |
|-----------|------------------------------------------------------------------|
| CUSTOMER  | смотреть мероприятия, бронировать места, отменять свои брони     |
| ORGANIZER | создавать и редактировать свои мероприятия, смотреть продажи     |
| ADMIN     | пользователи, площадки, категории, модерация мероприятий         |

## API

| Метод  | Путь                              | Роль             |
|--------|-----------------------------------|------------------|
| POST   | /api/users                        | —                |
| GET    | /api/users/me                     | любой            |
| GET    | /api/users, /api/users/{id}       | ADMIN            |
| PATCH  | /api/users/{id}/role              | ADMIN            |
| DELETE | /api/users/{id}                   | ADMIN            |
| GET    | /api/categories                   | —                |
| POST   | /api/categories                   | ADMIN            |
| PUT    | /api/categories/{id}              | ADMIN            |
| DELETE | /api/categories/{id}              | ADMIN            |
| GET    | /api/venues, /api/venues/{id}     | —                |
| POST   | /api/venues                       | ADMIN            |
| PUT    | /api/venues/{id}                  | ADMIN            |
| DELETE | /api/venues/{id}                  | ADMIN            |
| POST   | /api/venues/{id}/halls            | ADMIN            |
| GET    | /api/halls/{id}, /api/halls/{id}/seats | —           |
| GET    | /api/events                       | —                |
| GET    | /api/events/{id}                  | —                |
| GET    | /api/events/{id}/seats            | —                |
| GET    | /api/events/my                  | ORGANIZER        |
| POST   | /api/events                       | ORGANIZER        |
| PUT    | /api/events/{id}                  | ORGANIZER        |
| POST   | /api/events/{id}/submit           | ORGANIZER        |
| POST   | /api/events/{id}/cancel           | ORGANIZER, ADMIN |
| GET    | /api/events/moderation            | ADMIN            |
| POST   | /api/events/{id}/publish          | ADMIN            |
| POST   | /api/events/{id}/reject           | ADMIN            |
| GET    | /api/events/{id}/sales            | ORGANIZER, ADMIN |
| POST   | /api/bookings                     | CUSTOMER         |
| GET    | /api/bookings, /api/bookings/{id} | CUSTOMER, ADMIN  |
| POST   | /api/bookings/{id}/cancel         | CUSTOMER, ADMIN  |
| GET    | /api/tickets                      | CUSTOMER         |

Статусы мероприятия: `DRAFT` → `PENDING_MODERATION` → `PUBLISHED` или `REJECTED`, из любого
статуса можно перейти в `CANCELLED`. Бронировать можно только `PUBLISHED`.

## Бронирование

`POST /api/bookings` выполняется в одной транзакции:

1. проверка, что мероприятие опубликовано и не началось;
2. создание бронирования;
3. блокировка выбранных мест (`SELECT ... FOR UPDATE`), проверка, что они свободны и принадлежат залу;
4. выпуск билетов.

Любая ошибка откатывает всё. Уникальный индекс `seat_reservations(event_id, seat_id)`
не даёт продать одно место дважды даже при параллельных запросах.

Примеры запросов: `docs/requests.http`.
