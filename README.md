# Платформа бронирования билетов

Spring Boot 3, Spring Cloud 2023.0, Java 21, PostgreSQL, Liquibase.

## Модули

| Модуль             | Порт | Что делает                                                    |
|--------------------|------|---------------------------------------------------------------|
| `config-server`    | 8888 | Spring Cloud Config, конфиги сервисов лежат в `src/main/resources/config` |
| `discovery-server` | 8761 | Eureka, реестр сервисов                                        |
| `gateway`          | 8080 | Spring Cloud Gateway, единая точка входа и общий Swagger       |
| `event-service`    | 8081 | мероприятия, площадки, категории, теги, пользователи, брони   |

Сервисы при старте забирают конфигурацию с config-server (`spring.config.import`)
и регистрируются в Eureka. Адреса задаются переменными `CONFIG_URL` и `EUREKA_URL`.
Снаружи открыт только gateway: маршруты в `config/gateway.yml`, `/api/**` уходит на
`lb://event-service`, адрес инстанса берётся из Eureka.

## Запуск

Всё в Docker (сборка jar внутри образа, Java локально не нужна):

```bash
docker compose up --build
```

Образы собираются одним `Dockerfile` с аргументом `MODULE`. Порядок старта задаёт
`depends_on`: Postgres и инфраструктура, потом сервисы.

Общий Swagger UI на gateway: `http://localhost:8080/swagger-ui.html`. В выпадающем списке
по одному пункту на сервис, спецификация каждого проксируется через gateway
(`/event-service/v3/api-docs`), так что Try it out ходит тем же путём, что и клиенты.
Кнопка Authorize принимает id пользователя и подставляет его в заголовок `X-User-Id`.
Реестр Eureka: `http://localhost:8761`. Конфиг сервиса глазами клиента:
`http://localhost:8888/event-service/default`.

Локально, только БД в Docker (каждый сервис в своём терминале):

```bash
docker compose up -d postgres
mvn -pl config-server spring-boot:run
mvn -pl discovery-server spring-boot:run
mvn -pl event-service spring-boot:run
mvn -pl gateway spring-boot:run
```

Настройки БД: переменные `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`
(по умолчанию `localhost:5432/ticketing`, `ticketing/ticketing`), читаются config-server
из `config/event-service.yml`.

Тесты (`mvn test`) поднимают PostgreSQL через Testcontainers, нужен запущенный Docker.
В тестах config-server и Eureka не нужны: тестовый `application.yml` не импортирует
конфиг и отключает клиент Eureka. `mvn verify` дополнительно считает покрытие JaCoCo
в `event-service` и падает, если строк покрыто меньше 70 %.

## Роли

Пользователь передаётся заголовком `X-User-Id`. Роли: `CUSTOMER`, `ORGANIZER`, `ADMIN`.

| Роль      | Что может                                                        |
|-----------|------------------------------------------------------------------|
| CUSTOMER  | смотреть мероприятия, бронировать места, отменять свои брони     |
| ORGANIZER | создавать и редактировать свои мероприятия, смотреть продажи     |
| ADMIN     | пользователи, площадки, категории, теги, модерация мероприятий   |

## API

| Метод  | Путь                                   | Роль             |
|--------|----------------------------------------|------------------|
| POST   | /api/users                             | нет              |
| GET    | /api/users/me                          | любой            |
| GET    | /api/users, /api/users/{id}            | ADMIN            |
| PATCH  | /api/users/{id}/role                   | ADMIN            |
| DELETE | /api/users/{id}                        | ADMIN            |
| GET    | /api/categories, /api/categories/{id}  | нет              |
| POST   | /api/categories                        | ADMIN            |
| PUT    | /api/categories/{id}                   | ADMIN            |
| DELETE | /api/categories/{id}                   | ADMIN            |
| GET    | /api/tags, /api/tags/{id}              | нет              |
| POST   | /api/tags                              | ADMIN            |
| PUT    | /api/tags/{id}                         | ADMIN            |
| DELETE | /api/tags/{id}                         | ADMIN            |
| GET    | /api/venues, /api/venues/{id}          | нет              |
| POST   | /api/venues                            | ADMIN            |
| PUT    | /api/venues/{id}                       | ADMIN            |
| DELETE | /api/venues/{id}                       | ADMIN            |
| POST   | /api/venues/{id}/halls                 | ADMIN            |
| GET    | /api/halls/{id}, /api/halls/{id}/seats | нет              |
| GET    | /api/events                            | нет              |
| GET    | /api/events/{id}                       | нет              |
| GET    | /api/events/{id}/seats                 | нет              |
| GET    | /api/events/my                         | ORGANIZER        |
| POST   | /api/events                            | ORGANIZER        |
| PUT    | /api/events/{id}                       | ORGANIZER        |
| POST   | /api/events/{id}/submit                | ORGANIZER        |
| POST   | /api/events/{id}/cancel                | ORGANIZER, ADMIN |
| GET    | /api/events/moderation                 | ADMIN            |
| POST   | /api/events/{id}/publish               | ADMIN            |
| POST   | /api/events/{id}/reject                | ADMIN            |
| GET    | /api/events/{id}/sales                 | ORGANIZER, ADMIN |
| POST   | /api/bookings                          | CUSTOMER         |
| GET    | /api/bookings, /api/bookings/{id}      | CUSTOMER, ADMIN  |
| POST   | /api/bookings/{id}/cancel              | CUSTOMER, ADMIN  |
| GET    | /api/tickets                           | CUSTOMER         |

Статусы мероприятия: `DRAFT`, `PENDING_MODERATION`, `PUBLISHED` или `REJECTED`, из любого
статуса можно перейти в `CANCELLED`. Бронировать можно только `PUBLISHED`.

## Списки и пагинация

Все списки принимают `page` (с нуля) и `size`. Больше 50 записей за запрос не отдаётся:
`size=500` молча превращается в 50 (`Paging.of`).

Два формата ответа:

- страница с общим количеством: `content`, `page`, `size`, `totalElements`, `totalPages`,
  и то же общее количество в заголовке `X-Total-Count`. Так отдаются пользователи,
  категории, теги, площадки, свои мероприятия, очередь модерации, брони и билеты;
- бесконечная прокрутка без общего количества: `content`, `page`, `size`, `hasNext`.
  Так отдаются каталог мероприятий (`GET /api/events`) и схемы мест
  (`/api/halls/{id}/seats`, `/api/events/{id}/seats`). Подсчёт `count(*)` по большим
  таблицам не делается, клиент листает, пока `hasNext` не станет `false`.

## Связи

- One to Many: площадка и залы, зал и места.
- Many to One: мероприятие и категория, зал, организатор; бронь и пользователь.
- Many to Many: мероприятие и теги через таблицу `event_tags` (только два ключа).
- Many to Many с дополнительным полем: мероприятие и место через `seat_reservations`,
  где кроме пары ключей хранится бронь и время. Отдельная сущность `SeatReservation`.

Enum в базе хранятся строками (`@Enumerated(EnumType.STRING)`).

## Валидация

Два уровня. На контроллере проверяются DTO (`@Valid`, аннотации в `*Request`),
ошибка возвращается как 400 со списком полей. На сущностях стоят те же аннотации
плюс проверки, которых в DTO нет: например `Event.isTimeWindowValid` через `@AssertTrue`.
Hibernate запускает их перед insert и update, так что некорректная строка не попадёт
в базу даже в обход контроллера.

## Транзакции

`POST /api/bookings` выполняется в одной транзакции:

1. проверка, что мероприятие опубликовано и не началось;
2. создание бронирования;
3. блокировка выбранных мест (`SELECT ... FOR UPDATE`), проверка, что они свободны и принадлежат залу;
4. выпуск билетов.

Любая ошибка откатывает всё. Уникальный индекс `seat_reservations(event_id, seat_id)`
не даёт продать одно место дважды даже при параллельных запросах.

Вторая транзакция: `POST /api/events/{id}/cancel`. Меняется статус мероприятия и в том же
контексте по событию `EventCancelledEvent` отменяются все активные брони, их билеты и
резервы мест. Если отмена одной брони упадёт, мероприятие останется в прежнем статусе.

Примеры запросов: `docs/requests.http`.
