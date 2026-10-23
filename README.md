# Платформа бронирования билетов

Spring Boot 3, Spring Cloud 2023.0, Java 21, PostgreSQL, Liquibase. Сервисы на Spring MVC
и на WebFlux, доступ к базе через JPA и через R2DBC.

## Схема

```
                         config-server :8888 (конфиги всем)      discovery-server :8761 (Eureka)
                                   ^                                     ^
клиент --> gateway :8080 ----------+-------------------------------------+
             |
             |-- /api/users/**  ------------------> user-service :8082   WebFlux + R2DBC, база users
             |
             |-- /api/events, venues, categories,
             |   halls, tags ---------------------> event-service :8081  MVC + JPA, база ticketing
             |                                        |  Feign: роль ---> user-service
             |                                        |  Feign: отмена броней ---> booking-service
             |
             |-- /api/bookings, /api/tickets,
                 /api/events/{id}/seats|sales ------> booking-service :8083  WebFlux + JPA, база bookings
                                                       |  Feign: мероприятие, зал, места ---> event-service
                                                       |  Feign: роль ---> user-service
```

Каждый Feign-вызов обёрнут в circuit breaker с fallback. Снаружи открыт только gateway,
сервисы находят друг друга через Eureka.

## Модули

| Модуль             | Порт | Что делает                                                    |
|--------------------|------|---------------------------------------------------------------|
| `config-server`    | 8888 | Spring Cloud Config с git-бэкендом, конфиги в отдельном репозитории `configs-repo` |
| `discovery-server` | 8761 | Eureka, реестр сервисов                                        |
| `gateway`          | 8080 | Spring Cloud Gateway, единая точка входа и общий Swagger       |
| `event-service`    | 8081 | мероприятия, площадки, категории, теги (MVC, JPA); роли берёт у user-service, отмену броней просит у booking-service |
| `user-service`     | 8082 | пользователи и роли (WebFlux, R2DBC), своя база `users`       |
| `booking-service`  | 8083 | брони, резервы мест, билеты, схема занятости и продажи (WebFlux, JPA), своя база `bookings` |

Сервисы при старте забирают конфигурацию с config-server (`spring.config.import`,
адрес в `CONFIG_URL`) и регистрируются в Eureka. Активный профиль задаёт переменная
`PROFILE`: `dev` (по умолчанию, всё на localhost) или `docker` (адреса из docker-compose).
Снаружи открыт только gateway: маршруты в `configs-repo/gateway/application.yml`, `/api/users/**` уходит на
`lb://user-service`, брони, билеты, схема мест и продажи (`/api/bookings/**`, `/api/tickets`,
`/api/events/{id}/seats`, `/api/events/{id}/sales`) на `lb://booking-service`, остальное
`/api/**` на `lb://event-service`. Адрес инстанса берётся из Eureka. Спецификации OpenAPI и
actuator сервисов проксируются по `/{service}/v3/api-docs` и `/{service}/actuator/**`.

## Запуск

Всё в Docker (сборка jar внутри образа, Java локально не нужна):

```bash
docker compose up --build
```

Образы собираются одним `Dockerfile` с аргументом `MODULE`. Порядок старта задаёт
`depends_on`: Postgres и инфраструктура, потом сервисы. У каждого сервиса своя база в одном
Postgres: `ticketing` создаёт сам образ, остальные (`users`, `bookings`) создаёт `docker/init-databases.sql`
при первом запуске тома. Если том уже есть, базу нужно создать руками или удалить том
(`docker compose down -v`).

Общий Swagger UI на gateway: `http://localhost:8080/swagger-ui.html`. В выпадающем списке
по одному пункту на сервис, спецификация каждого проксируется через gateway
(`/event-service/v3/api-docs`), так что Try it out ходит тем же путём, что и клиенты.
Кнопка Authorize принимает id пользователя и подставляет его в заголовок `X-User-Id`.
Реестр Eureka: `http://localhost:8761`. Конфиг сервиса глазами клиента:
`http://localhost:8888/event-service/docker`.

## Конфигурация

Config-server работает с git-бэкендом: конфиги лежат в отдельном репозитории
[highload-systems-lab-configs](https://github.com/Valll-v/highload-systems-lab-configs),
подключённом подмодулем `configs-repo` (`git clone --recurse-submodules` или
`git submodule update --init`). В корне общие `application.yml` и `application-<профиль>.yml`,
в каталоге каждого сервиса его собственные (`search-paths: {application}`). Профили: `dev`
(localhost) и `docker` (хосты из compose). Адрес репозитория задаёт `CONFIG_REPO_URI`:
локально по умолчанию `file://<проект>/configs-repo`, в docker-compose `file:///configs-repo`
(снимок подмодуля, собранный в образе), можно указать и адрес на GitHub.

Локально, только БД в Docker (каждый сервис в своём терминале):

```bash
docker compose up -d postgres
mvn -pl config-server spring-boot:run       # читает ../configs-repo
mvn -pl discovery-server spring-boot:run
mvn -pl event-service spring-boot:run
mvn -pl user-service spring-boot:run
mvn -pl booking-service spring-boot:run
mvn -pl gateway spring-boot:run
```

Настройки БД лежат в `configs-repo/<сервис>/application-<профиль>.yml`: `ticketing/ticketing`,
база `ticketing` у event-service, `users` у user-service, `bookings` у booking-service;
в профиле `dev` хост `localhost:5432`, в `docker` хост `postgres`.

Тесты (`mvn test`) поднимают PostgreSQL через Testcontainers, нужен запущенный Docker.
В тестах config-server и Eureka не нужны: тестовый `application.yml` не импортирует
конфиг и отключает клиент Eureka. `mvn verify` дополнительно считает покрытие JaCoCo
в каждом сервисе и падает, если строк покрыто меньше 70 %.

## user-service

Реактивный сервис: Spring WebFlux, Spring Data R2DBC, драйвер `r2dbc-postgresql`.
Контроллер возвращает `Mono`, репозиторий `ReactiveCrudRepository`, ни один поток не
блокируется на запросе к базе. Схему накатывает Liquibase: у него нет R2DBC-режима,
поэтому в сервисе два драйвера, JDBC только для миграций при старте
(`spring.liquibase.url`), R2DBC для запросов (`spring.r2dbc.url`).

Проверка пользователя: аннотация `@CurrentUser` на параметре контроллера, резолвер
читает `X-User-Id`, достаёт пользователя из базы и сверяет роль со списком в аннотации.
Нет заголовка или такого пользователя: 401, не та роль: 403.

## booking-service

Второй реактивный сервис, но с блокирующим хранилищем: Spring WebFlux снаружи, Spring Data JPA
внутри. Контроллеры возвращают `Mono`, а каждый вызов сервиса завёрнут в
`Mono.fromCallable(...).subscribeOn(Schedulers.boundedElastic())` (`common/Blocking.java`):
JPA и Feign блокируют поток, поэтому работают на пуле boundedElastic, а event loop Netty
остаётся свободным. Транзакция бронирования открывается через `TransactionTemplate` уже
после запросов в event-service, чтобы не держать соединение с базой на время HTTP.

Сервис знает только id мероприятий, мест и пользователей. Мероприятие, зал и схему мест он
спрашивает у event-service (`EventClient`), роль у user-service (`UserClient`), оба через
OpenFeign с circuit breaker и fallback: 404 от event-service пробрасывается как 404,
недоступность как 503. Схема занятости `GET /api/events/{id}/seats` собирается из страницы
зала event-service и резервов в своей базе.

## Feign и Circuit Breaker

event-service пользователей не хранит, в `events.organizer_id` и `bookings.customer_id`
лежат только их id (миграция 006 убирает таблицу `users` и внешние ключи на неё). Роль
берётся через OpenFeign: `UserClient` зовёт `GET /api/users/me` на `user-service` (адрес из
Eureka) с тем же `X-User-Id`, что прислал клиент. Ответ кладётся в атрибут запроса, так что
проверка `@RequireRole` и параметр `@CurrentUser` делают один вызов на запрос.

Вызов обёрнут в resilience4j через Spring Cloud CircuitBreaker
(`spring.cloud.openfeign.circuitbreaker.enabled`). Настройки в `configs-repo/event-service/application.yml`:
окно 10 вызовов, при половине ошибок выключатель открывается на 10 секунд, таймаут вызова
3 секунды. Ответы 401 и 404 от user-service ошибками не считаются (`ignore-exceptions`):
это неизвестный пользователь, а не авария.

Fallback (`UserClientFallback`): если user-service недоступен или выключатель открыт,
защищённые эндпоинты отвечают 503, а публичные (`GET /api/events`, `GET /api/events/{id}`)
работают дальше и считают клиента анонимом. Состояние выключателя видно в
`/actuator/health` и `/actuator/circuitbreakers` event-service.

## Как посмотреть выключатель вживую

Через gateway доступны actuator каждого сервиса: `/event-service/actuator/health`,
`/booking-service/actuator/circuitbreakers`, то же для user-service.

1. `docker compose stop user-service`.
2. `GET /api/events/my` с `X-User-Id`: 503 с сообщением user-service is unavailable из fallback.
   `GET /api/events` без заголовка работает.
3. Повторить запрос шесть раз: в `/event-service/actuator/circuitbreakers` у выключателя
   `UserClient#me(long)` состояние OPEN, event-service больше не звонит соседу.
4. `docker compose start user-service`: через 10 секунд HALF_OPEN, после двух удачных
   вызовов CLOSED.

Тесты на то же самое без Docker-соседей: `UserClientCircuitBreakerTest` в event-service и
`EventClientCircuitBreakerTest` в booking-service поднимают WireMock вместо соседнего сервиса,
адрес подставляют через `spring.cloud.discovery.client.simple` вместо Eureka и проверяют, что
пять ответов 500 открывают выключатель, а 401 и 404 в него не попадают.

## Роли

Пользователь передаётся заголовком `X-User-Id`, регистрация и смена роли живут в
user-service. Роли: `CUSTOMER`, `ORGANIZER`, `ADMIN`.

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
| POST   | /api/events/{id}/bookings/cancel       | ORGANIZER, ADMIN |
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
- Many to One: мероприятие и категория, зал; билет и бронь.
- Many to Many: мероприятие и теги через таблицу `event_tags` (только два ключа).
- Many to Many с дополнительным полем: мероприятие и место через `seat_reservations`
  в booking-service, где кроме пары id хранится бронь и время. Отдельная сущность
  `SeatReservation`.
- Связи между сервисами (организатор мероприятия, покупатель брони, место билета) хранятся
  как id без внешних ключей: таблицы лежат в разных базах.

Enum в базе хранятся строками (`@Enumerated(EnumType.STRING)`).

## Валидация

Два уровня. На контроллере проверяются DTO (`@Valid`, аннотации в `*Request`),
ошибка возвращается как 400 со списком полей. На сущностях стоят те же аннотации
плюс проверки, которых в DTO нет: например `Event.isTimeWindowValid` через `@AssertTrue`.
Hibernate запускает их перед insert и update, так что некорректная строка не попадёт
в базу даже в обход контроллера.

## Транзакции

`POST /api/bookings` в booking-service. Сначала без транзакции два запроса в event-service:
мероприятие (опубликовано, не началось, цена) и страницы схемы зала (выбранные места
существуют и принадлежат залу). Потом одна транзакция через `TransactionTemplate`:

1. проверка, что места ещё свободны;
2. создание бронирования;
3. резервы мест, `flush`;
4. выпуск билетов.

Любая ошибка откатывает всё. Места лежат в базе другого сервиса, блокировать их строки
нельзя, поэтому гонку решает уникальный индекс `seat_reservations(event_id, seat_id)`:
второй параллельный запрос получает 409.

`POST /api/events/{id}/cancel` проходит через два сервиса. event-service в своей транзакции
переводит мероприятие в `CANCELLED`, а после коммита зовёт booking-service
(`POST /api/events/{id}/bookings/cancel`), который в своей транзакции отменяет активные
брони, билеты и резервы. booking-service сам проверяет у event-service, что мероприятие
отменено, поэтому вызов безопасно повторять. Если booking-service лежит, мероприятие
остаётся отменённым, а брони отменяются повторным вызовом позже (в логе event-service
есть предупреждение с нужным путём).

Примеры запросов в порядке показа: `docs/requests.http`.
