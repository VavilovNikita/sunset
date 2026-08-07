# Отчёт по анализу бэкенда — Sunset Beach Resort & Spa

## 1. Стек

| Компонент | Значение | Где видно |
|---|---|---|
| Язык | Java 21 | `pom.xml:30` |
| Фреймворк | Spring Boot 4.1.0 (starter-parent), Spring MVC, Spring Data JPA, Spring Security | `pom.xml:6-52` |
| БД | PostgreSQL | `pom.xml:54-58`, `application.properties:5-7` |
| ORM | Hibernate/JPA, `ddl-auto=validate` — схему не создаёт, только сверяет | `application.properties:27` |
| Миграции схемы | Flyway, `baseline-on-migrate` — принимает существующую (доставшуюся от Prisma) схему как версию 1 | `application.properties:37-43`, `db/migration/V1__baseline.sql` |
| Аутентификация | Собственные JWT (jjwt 0.12.6, HS256), без серверных сессий | `pom.xml:82-99`, `security/JwtService.java` |
| API-контракт | Contract-first через `openapi.yaml`, часть контроллеров реализует сгенерированные интерфейсы `api/*Api.java` (OpenAPI Generator, jar в репо) | `openapi.yaml`, `openapi-generator-cli.jar`, `api/BookingsApi.java` и т.п. |
| Файлы/картинки | Apache Tika (детект MIME по магическим байтам) | `pom.xml:103-107`, `service/RoomService.java:111` |
| Сборка/деплой | Maven, Docker, docker-compose (VPS-деплой, отдельный процесс/сеть для Postgres) | `Dockerfile`, `docker-compose.yml` |
| Наблюдаемость | Spring Actuator, экспонирован только `/actuator/health` | `application.properties:66-70` |

Важный контекст из комментариев в коде и git-истории: это **переписанный на Java бэкенд** ранее существовавшего Next.js/Prisma-приложения (см. `427cd60 Move authentication ownership from NextAuth to Java`). Схема БД унаследована от Prisma (camelCase-идентификаторы в кавычках, отсюда `globally_quoted_identifiers=true` в `application.properties:34`). `openapi.yaml` прямо описан как "reverse-engineered from Next.js route handlers" — контракт зафиксирован для миграции 1:1.

Контекстный путь API: `/api` (`server.servlet.context-path=/api`, `application.properties:3`).

## 2. Карта API-эндпоинтов

Реализованы точно по `openapi.yaml`. Базовый путь `/api` опущен ниже.

### Auth (`controller/AuthController.java`)
| Метод | Путь | Доступ | Что делает |
|---|---|---|---|
| POST | `/auth/login` | public | Проверка email/bcrypt-пароль, rate-limit по (ip,email), выдача JWT |
| POST | `/auth/register` | ADMIN | Алиас `POST /users` |
| GET | `/auth/me` | любой staff | Текущий пользователь из JWT-принципала |
| POST | `/auth/logout` | public | No-op (stateless JWT) |

### Rooms (`controller/RoomController.java`, реализует `api/RoomsApi.java`)
| Метод | Путь | Доступ | Что делает |
|---|---|---|---|
| GET | `/rooms` | staff | Список номеров |
| GET | `/rooms/{id}` | staff | Номер по id |
| POST | `/rooms` | staff | Создание номера |
| PATCH | `/rooms/{id}` | staff | Полная замена полей номера |
| DELETE | `/rooms/{id}` | staff | Удаление (409 если есть брони — FK) |
| POST | `/rooms/{id}/images` | staff | Загрузка фото (валидация типа по контенту + размер ≤8MB, всё-или-ничего) |
| DELETE | `/rooms/{id}/images` (body `path`) | staff | Удаление фото из массива + файла с диска (best-effort) |

### Pricing (`controller/PricingController.java`)
| Метод | Путь | Доступ | Что делает |
|---|---|---|---|
| GET | `/pricing/{roomId}?month=` | staff | Цены по дням на месяц (override или basePrice) |
| PATCH | `/pricing/{roomId}` | staff | Upsert цены по диапазону дат (≤366 дней) |

### Availability (`controller/AvailabilityController.java`)
| Метод | Путь | Доступ | Что делает |
|---|---|---|---|
| GET | `/availability/{roomId}?month=` | staff | Доступность по дням с источником блокировки (`booking`/`manual`) |
| PATCH | `/availability/{roomId}` | staff | Ручная блокировка/разблокировка диапазона (не трогает брони) |

### Bookings (`controller/BookingController.java`)
| Метод | Путь | Доступ | Что делает |
|---|---|---|---|
| GET | `/bookings?from&to&status` | staff | Список броней с фильтрами |
| GET | `/bookings/{id}` | staff | Бронь по id |
| POST | `/bookings` | **public** | Создание брони гостем (без сессии) |
| PATCH | `/bookings/{id}` | staff | Смена статуса/платёжной заметки |
| GET | `/bookings/export?from&to&status` | staff | Экспорт CSV |

### Users (`controller/UserController.java`)
| Метод | Путь | Доступ | Что делает |
|---|---|---|---|
| GET | `/users` | **ADMIN** | Список сотрудников |
| GET | `/users/{id}` | ADMIN | Сотрудник по id |
| POST | `/users` | ADMIN | Создание сотрудника |
| PATCH | `/users/{id}` | ADMIN | Смена роли (нельзя себе) |

### Public (`controller/PublicController.java`)
| Метод | Путь | Доступ | Что делает |
|---|---|---|---|
| GET | `/public/rooms` | public | Список номеров для сайта |
| GET | `/public/rooms/{id}` | public | Номер для сайта |
| GET | `/public/rooms/{id}/pricing?month=` | public | Цены (без утечки данных staff) |
| GET | `/public/rooms/{id}/availability?month=` | public | Доступность без источника блокировки |
| GET | `/uploads/rooms/{roomId}/{filename}` | public | Отдача файлов фото номеров (защита от path traversal) |

Права разграничены в `security/SecurityConfig.java:34-43`: `/users/**` и `POST /auth/register` — только `ROLE_ADMIN`; `/public/**`, `/uploads/**`, `POST /bookings`, `POST /auth/login|logout` — открыты; всё остальное требует любой валидный JWT (ADMIN или MANAGER).

## 3. Модель данных

Схема из `db/migration/V1__baseline.sql` (задокументирована, реально не выполняется — Flyway принимает существующую БД как baseline) + JPA-сущности:

**User** (`entity/UserEntity.java`)
- `id` (UUID, PK), `email` (unique), `passwordHash` (bcrypt, cost 12), `role` (enum `ADMIN`/`MANAGER`, default `MANAGER`), `createdAt`.

**Room** (`entity/RoomEntity.java`)
- `id` (UUID), `name`, `description`, `capacity` (int), `basePrice` (Decimal 10,2), `images` (Postgres `text[]`, пути `/uploads/rooms/{id}/...`), `createdAt`.

**Booking** (`entity/BookingEntity.java`)
- `id` (UUID), `roomId` (FK → Room, `@ManyToOne` только для чтения), `guestName`, `guestEmail`, `guestPhone`, `checkIn`/`checkOut` (LocalDate), `totalPrice` (Decimal, всегда пересчитывается сервером), `status` (enum `NEW`/`CONFIRMED`/`PAID`/`CANCELLED`), `paymentNote` (nullable text), `createdAt`, `updatedAt`.

**Availability** (`entity/AvailabilityEntity.java`)
- `id`, `roomId`, `date`, `isBlocked` (bool, default true), `createdAt`. Уникальность `(roomId, date)`. Это отдельный слой "ручных" блокировок, независимый от бронирований.

**RatePlan** (`entity/RatePlanEntity.java`)
- `id`, `roomId`, `date`, `price` (Decimal), `createdAt`. Уникальность `(roomId, date)`. Переопределение цены на конкретный день.

Связи: `Room 1—N Booking`, `Room 1—N Availability`, `Room 1—N RatePlan`. Прямых FK-констрейнтов на уровне JPA-маппинга кроме `roomId` не описано явно (связь read-only через `@JoinColumn(insertable=false, updatable=false)`), но БД (унаследованная от Prisma) поддерживает целостность — удаление номера с бронями даёт `DataIntegrityViolationException` → 409 (`service/RoomService.java:84-86`).

## 4. Реализованная бизнес-логика

- **Аутентификация/роли**: bcrypt + JWT (HS256, TTL по умолчанию 7 дней, настраивается), `Bearer`-токен, stateless-фильтр `security/JwtAuthFilter.java`. Две роли: `ADMIN` (полный доступ + управление пользователями), `MANAGER` (весь операционный функционал кроме `/users/**`). Rate-limit на логин: 5 неудачных попыток / 15 минут по паре (ip, email), in-memory, однопроцессный (`security/LoginRateLimiter.java`).
- **Бронирование**: создание брони — публичный эндпоинт, но сервер сам пересчитывает `totalPrice` (по `RatePlan`/`basePrice`) и валидирует доступность внутри `SERIALIZABLE`-транзакции (`service/BookingWriter.java`), исключая гонки при параллельных запросах на одни даты; конфликт сериализации Postgres превращается в понятную 409-ошибку (`service/BookingService.java:63-68`). Статусный жизненный цикл `NEW → CONFIRMED → PAID/CANCELLED` меняется вручную персоналом через `PATCH /bookings/{id}`, вместе с произвольной `paymentNote`.
- **Доступность/цены**: посуточная логика на календарный месяц, с разделением "занято бронью" vs "занято вручную" (видно только персоналу — `AvailabilityDay.source`); гостям источник блокировки скрыт (`service/AvailabilityService.java`). Массовое проставление цен/блокировок по диапазону дат (лимит 366 дней).
- **CSV-экспорт** броней для отчётности (`service/BookingService.java:135-178`).
- **Загрузка изображений номеров**: проверка реального MIME-типа по контенту (Tika), а не по заголовку клиента; лимит 8MB/файл; атомарная валидация всех файлов до записи; защита от path traversal при отдаче (`service/RoomImageService.java`).
- **Email-уведомления**: см. раздел 5.
- **Обработка ошибок**: единый `GlobalExceptionHandler` (`error/GlobalExceptionHandler.java`) конвертирует доменные исключения (`NotFoundException`, `ConflictException`, `ValidationException`, `TooManyRequestsException` и т.д.) в JSON-ошибки, формат ошибок валидации имитирует Zod (`formErrors`/`fieldErrors`) для совместимости с прежним контрактом.
- **CORS**: явный whitelist origin'ов (без wildcard, т.к. `allowCredentials=true`) (`config/CorsConfig.java`).

## 5. Подключённые интеграции

- **Email**: Resend API (`service/EmailService.java`) — HTTP-клиент на `https://api.resend.com`. Два сценария: уведомление персоналу о новой брони (всем пользователям из таблицы `User`) и письмо гостю при смене статуса на `PAID`/`CANCELLED`. **Fail-open**: любая ошибка отправки логируется и проглатывается, не влияет на основной флоу брони. Если `RESEND_API_KEY` не задан — письма просто пишутся в лог (`dev-log` режим), реальная отправка не происходит.
- **Платежи**: интеграции с платёжным провайдером (Stripe/PayPal и т.п.) **нет**. Есть только статус `PAID` и свободнотекстовое поле `paymentNote` — оплата фиксируется вручную персоналом, никакой обработки платежей/вебхуков не реализовано.
- **SMS**: не реализовано и не упоминается.
- **Прочие внешние API**: нет (кроме Resend).

## 6. Незавершённые/заглушечные участки

- `db/migration/V1__baseline.sql` — намеренная заглушка-плейсхолдер (never executed), задокументирована как способ "усыновить" уже существующую Prisma-схему через `baseline-on-migrate`. Это не недоделка, а осознанное решение.
- Явных `TODO`/`FIXME`/незаконченных заглушек в `src/main` не найдено (полнотекстовый поиск не дал совпадений, кроме комментария выше).
- `LoginRateLimiter` — комментарий в коде честно указывает на ограничение: in-memory, работает только для одного инстанса приложения; при горизонтальном масштабировании потребует внешнего хранилища (Redis и т.п.) — сейчас это не проблема, т.к. деплой однонодовый.
- В репозитории закоммичен `openapi-generator-cli.jar` — бинарный build-инструмент в VCS, необычно, но не относится к функциональности рантайма.
- Моки встречаются только в тестах (`src/test/java/...`, Mockito/MockMvc) — это нормальная тестовая инфраструктура, не заглушки в проде.
- `HELP.md` — стандартный boilerplate-файл Spring Initializr, не содержит проектной документации.

## 7. Зачатки POS/PMS-функционала

Обнаруженного явного модуля POS (point-of-sale: заказы в ресторане/баре, счета-фолио, позиции меню, кассовые операции) — **нет**.

То, что уже есть, — это ядро классической мини-**PMS** (Property Management System) для небольшого отеля:
- Инвентарь номеров с вместимостью и базовой ценой (`Room`);
- Управление тарифами по дням (`RatePlan`) — аналог revenue management "rate override";
- Календарь доступности с разделением "занято бронью" и "занято вручную персоналом" (`Availability`) — похоже на функцию ручного стоп-продажи (stop-sell);
- Движок бронирования с проверкой пересечения дат и защитой от гонок (`Booking` + `BookingWriter`);
- Жизненный цикл брони со статусами включая "оплачено" (`PAID`) — но без реального биллинга/фолио;
- Ролевой доступ персонала (ADMIN/MANAGER) — прообраз ролей ресепшена/менеджмента;
- CSV-экспорт броней — прообраз отчётности/night audit.

Отсутствуют: мультиобъектность (multi-property), housekeeping-модуль, гостевые профили/CRM/лояльность, channel manager/OTA-интеграции, счета/инвойсинг, кассовые операции, складской учёт — то есть дальше базового бронирования и тарифов PMS-функционал не развит, а POS-функционал не начат вовсе.
