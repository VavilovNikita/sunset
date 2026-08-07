# План: POS-модуль (ресторан/бар/спа) поверх текущей мини-PMS

Ничего в `Room`/`Booking`/`Availability`/`RatePlan` не меняется — только новые таблицы, новые контроллеры/сервисы, аддитивные правки `openapi.yaml`, `SecurityConfig` и первая настоящая Flyway-миграция.

## 1. Сущности

### MenuItem
`id, name, description, category (string), price (Decimal 10,2), isAvailable (bool, default true), createdAt`
— зеркалит простоту `Room`: без отдельной таблицы категорий, `category` — свободный текст (карта меню меняется чаще, чем зоны обслуживания). Если позже понадобится маршрутизация на кухню/бар/спа-кабинет (KDS-печать), это отдельное поле `department`-enum можно добавить отдельной миграцией — сейчас не нужно, в задаче не просили.

### Table (зона обслуживания)
`id, zone (enum: RESTAURANT, BAR, SPA, POOL, ROOM_SERVICE), label (string, напр. "Table 12" / "Spa Room A" / "Bar Seat 3"), capacity (int, nullable), isActive (bool)`
— одна сущность с полем `zone`, а не `Table` + отдельная `Zone`-таблица: то же решение, что и с `category` — избегаем сущности ради сущности, пока зон конечное небольшое число (enum, не отдельная таблица).

### Order
`id, tableId (FK → Table, nullable — для takeaway/pool service без стола), bookingId (FK → Booking, nullable), guestName (nullable, для walk-in без брони), status (enum: OPEN, SENT, PAID, CANCELLED), openedByUserId (FK → User), total (Decimal, всегда пересчитывается сервером из OrderItem — как `Booking.totalPrice` никогда не берётся из клиента), note (nullable), createdAt, updatedAt`

### OrderItem
`id, orderId (FK → Order, ON DELETE CASCADE), menuItemId (FK → MenuItem), quantity (int), unitPrice (Decimal — снэпшот цены на момент добавления, не ссылка на живую MenuItem.price — тот же принцип, что и в `BookingWriter.computeTotalPrice`, который фиксирует цену RatePlan/Room на момент брони), note (nullable), createdAt`

### Shift
`id, openedByUserId (FK → User), openedAt, closedByUserId (FK → User, nullable), closedAt (nullable), openingCashFloat (Decimal, nullable), closingCashCounted (Decimal, nullable), status (enum: OPEN, CLOSED), notes (nullable)`
— уникальный частичный индекс `(openedByUserId) WHERE status='OPEN'`: один пользователь не может держать две открытые смены одновременно (та же идея, что уникальность `(roomId,date)` у `Availability`/`RatePlan`).

### Payment
`id, orderId (FK → Order, NOT NULL), method (enum: CASH, CARD, ROOM_CHARGE, OTHER), amount (Decimal 10,2), bookingId (FK → Booking, nullable — заполняется только при method=ROOM_CHARGE), recordedByUserId (FK → User), shiftId (FK → Shift, NOT NULL), createdAt`
— `shiftId` в задаче явно не упомянут в списке полей, но без него невозможен «отчёт по кассе» (Z-report), поэтому добавляю как необходимое дополнение к тому, что было прямо запрошено.

### Order ↔ Booking (folio) и судьба `paymentNote`

Два независимых денежных потока, которые сейчас физически разделены:

- **Оплата самого проживания** — `Booking.totalPrice` + ручной `Booking.paymentNote`, выставляется вручную персоналом при `PATCH /bookings/{id}` (`service/BookingService.java:74-90`). Это не трогаем.
- **Оплата POS-заказов** — новая таблица `Payment`.

Room-charge не пишет ничего в `Booking` — вместо этого создаётся `Payment{orderId, method=ROOM_CHARGE, bookingId, amount, shiftId}`. Т.е. «счёт номера» (folio) в моменте — это **вычисляемая, не хранимая** величина:

```
folio(booking) = booking.totalPrice + Σ Payment.amount
                 WHERE bookingId = booking.id AND method = 'ROOM_CHARGE'
```

Это тот же принцип, что уже используется в кодовой базе: сервер всегда пересчитывает сумму на лету, а не хранит и не доверяет денежному полю, которое можно рассинхронизировать (см. как `BookingWriter` игнорирует любую цену от клиента). Практическое следствие: на чек-ауте персонал, ставя `Booking.status = PAID` и вписывая `paymentNote`, обязан вручную свериться с накопленными room-charge `Payment`-записями — сейчас это **ручной шаг**, автоматической связи с существующим `PATCH /bookings/{id}` нет (см. риски, п.6.г).

**Не переносить `paymentNote` в `Payment` сейчас.** Задача явно запрещает трогать `Booking`, а сама модель `paymentNote` (одна строка текста на бронь) и `Payment` (структурированный ledger на заказ) решают разные задачи. Объединение в единую платёжную книгу — это отдельная будущая миграция (сделать `Payment.orderId` nullable и допустить payment-записи, привязанные только к `Booking`), которую сознательно не делаю сейчас, чтобы не увеличивать blast radius этой фичи.

## 2. API

По аналогии с `RoomController`/`BookingController`: contract-first через `openapi.yaml` → генерация `api/*Api.java` → реализация в `*Controller implements *Api`, сервис + маппер + репозиторий на каждую сущность.

**Menu** (`MenuController implements MenuApi`)
| Метод | Путь | Доступ |
|---|---|---|
| GET | `/menu` | staff |
| GET | `/menu/{id}` | staff |
| POST | `/menu` | MANAGER+ |
| PATCH | `/menu/{id}` | MANAGER+ (полная замена, как `RoomInput`) |
| DELETE | `/menu/{id}` | MANAGER+ (409, если есть `OrderItem` — тот же FK-паттерн, что у `Room`/`Booking`) |

**Tables**
| Метод | Путь | Доступ |
|---|---|---|
| GET/POST | `/tables` | staff / MANAGER+ |
| PATCH/DELETE | `/tables/{id}` | MANAGER+ (409 при открытых заказах) |

**Orders**
| Метод | Путь | Доступ |
|---|---|---|
| GET | `/orders?status=&zone=&tableId=&bookingId=` | staff |
| GET | `/orders/{id}` | staff |
| POST | `/orders` | WAITER+ (открыть заказ: table/zone, опц. bookingId/guestName) |
| PATCH | `/orders/{id}` | WAITER+ (note, смена стола, статус OPEN→SENT, только пока не PAID) |
| POST | `/orders/{id}/items` | WAITER+ (добавить позиции; повторяемое поле, как `uploadRoomImages`) |
| PATCH/DELETE | `/orders/{id}/items/{itemId}` | WAITER+ (только пока заказ OPEN) |
| POST | `/orders/{id}/close` | **CASHIER+** — тело `{ method, amount, bookingId? }`; создаёт `Payment`, требует у вызывающего открытую `Shift`, ставит `status=PAID` |
| POST | `/orders/{id}/cancel` | WAITER+ |

**Shifts**
| Метод | Путь | Доступ |
|---|---|---|
| POST | `/shifts/open` | CASHIER+ (409, если уже есть открытая смена) |
| POST | `/shifts/{id}/close` | CASHIER+ (409, если под сменой ещё есть OPEN/SENT заказы — тот же паттерн защиты, что и при удалении Room с бронями) |
| GET | `/shifts/{id}` | CASHIER+/MANAGER+ (сводка: суммы по методам оплаты, ожидаемый нал vs `closingCashCounted`) |
| GET | `/shifts/{id}/export` | MANAGER+ — CSV построчно по `Payment`, точный аналог `GET /bookings/export` (`BookingService.buildCsv`) |

Новые схемы в `openapi.yaml`: `MenuItem/MenuItemInput`, `Table/TableInput`, `Order/OrderCreateInput/OrderStatus`, `OrderItem/OrderItemInput`, `OrderCloseInput`, `Payment/PaymentMethod`, `Shift/ShiftOpenInput/ShiftCloseInput/ShiftSummary`; переиспользуются существующие `ErrorMessage`/`ValidationError`/`OkTrue`/`OkUpdated`/`responses.Unauthorized`/`Forbidden`.

## 3. Роли

Текущих `ADMIN`/`MANAGER` (`model/Role.java`, БД-enum `"Role"`) недостаточно: официант не должен видеть `/users/**` или управлять ценами номеров, а роль «может закрыть чек и открыть смену» — это отдельная ответственность от «может закрыть заказ и добавить блюдо».

Предлагаю добавить в тот же enum `Role` (не отдельную таблицу прав — при одной учётке = одна роль это лишняя сложность для маленького отеля, в духе уже встречающегося в коде комментария "small hotel"):

- **WAITER** — открывает заказы, добавляет/убирает позиции, видит меню/столы. Не может закрывать заказ с оплатой и не имеет доступа к сменам/кассовым отчётам.
- **CASHIER** — всё, что может WAITER, плюс закрытие заказа с оплатой, открытие/закрытие своей смены, просмотр кассового отчёта.

`ADMIN`/`MANAGER` должны автоматически иметь все права `CASHIER`/`WAITER` (они и так полноправный back-office). Простой `hasRole()` в `SecurityConfig.java:34-43` этого не даёт — нужна ролевая иерархия:

```
ADMIN > MANAGER > CASHIER > WAITER
```

Технически — bean `RoleHierarchy` в `SecurityConfig`, а не правка `JwtAuthFilter` (сейчас он выдаёт ровно один `SimpleGrantedAuthority` на принципала, `security/JwtAuthFilter.java:31` — это остаётся как есть, иерархию разворачивает `AuthorizationManager`/`GrantedAuthoritiesMapper` на этапе проверки доступа). Важно **не сломать** текущее ограничение `/users/**` строго под `ADMIN` — оно не должно превратиться в «ADMIN и выше», так как выше ADMIN ничего нет, но при неверном направлении иерархии легко случайно дать MANAGER доступ к управлению пользователями (см. риск 6.з).

БД: `Role` — нативный Postgres enum (`@JdbcTypeCode(SqlTypes.NAMED_ENUM)`), новые значения добавляются `ALTER TYPE "Role" ADD VALUE`. Java-модель `model/Role.java` — генерируемый файл (`@Generated(..SpringCodegen..)`), править его руками не нужно — только `openapi.yaml`, дальше перегенерация.

## 4. Миграции

Это первая настоящая Flyway-миграция после инертного `V1__baseline.sql`. Разбиваю на два файла — Postgres не позволяет использовать в DML только что добавленное enum-значение в той же транзакции, где его добавили (а Flyway по умолчанию — одна миграция = одна транзакция):

**`V2__add_pos_roles.sql`**
```sql
ALTER TYPE "Role" ADD VALUE 'CASHIER';
ALTER TYPE "Role" ADD VALUE 'WAITER';
```

**`V3__pos_module.sql`** (иллюстративный набросок, не итоговый файл)
```sql
CREATE TYPE "OrderStatus" AS ENUM ('OPEN','SENT','PAID','CANCELLED');
CREATE TYPE "PaymentMethod" AS ENUM ('CASH','CARD','ROOM_CHARGE','OTHER');
CREATE TYPE "ShiftStatus" AS ENUM ('OPEN','CLOSED');

CREATE TABLE "MenuItem" (...);
CREATE TABLE "Table" (...);
CREATE TABLE "Shift" (
  ...,
  "openedByUserId" text NOT NULL REFERENCES "User"(id),
  ...
);
CREATE UNIQUE INDEX "Shift_openedByUserId_open_key"
  ON "Shift" ("openedByUserId") WHERE status = 'OPEN';

CREATE TABLE "Order" (
  ...,
  "tableId"    text REFERENCES "Table"(id),
  "bookingId"  text REFERENCES "Booking"(id),
  "openedByUserId" text NOT NULL REFERENCES "User"(id)
);
CREATE TABLE "OrderItem" (
  ...,
  "orderId" text NOT NULL REFERENCES "Order"(id) ON DELETE CASCADE,
  "menuItemId" text NOT NULL REFERENCES "MenuItem"(id)
);
CREATE TABLE "Payment" (
  ...,
  "orderId"   text NOT NULL REFERENCES "Order"(id),
  "bookingId" text REFERENCES "Booking"(id),
  "shiftId"   text NOT NULL REFERENCES "Shift"(id),
  "recordedByUserId" text NOT NULL REFERENCES "User"(id)
);
```
Названия таблиц/колонок — PascalCase/camelCase в кавычках, как унаследованная от Prisma схема (`globally_quoted_identifiers=true`, `PhysicalNamingStrategyStandardImpl` в `application.properties:34-35` — новые Java-поля маппятся 1:1 без snake_case). `"User"`, `"Room"`, `"Booking"`, `"Availability"`, `"RatePlan"` в миграции не трогаются вообще, кроме одного аддитивного `ALTER TYPE "Role"`.

## 5. Риски интеграции с текущей архитектурой

**а) SERIALIZABLE в `BookingWriter`.** Изоляция `SERIALIZABLE` там нужна конкретно для гонки «двух броней на одни даты» и осознанно платит за это частыми abort'ами (`BookingService.isSerializationFailure`, SQLSTATE 40001). Заказы POS не конкурируют за общий ресурс так же — не переиспользовать этот паттерн для `Order`/`OrderItem`/`Payment`, достаточно обычного `@Transactional` (READ_COMMITTED). Если бездумно скопировать SERIALIZABLE на запись Payment в busy-час ресторана, получим лишние 409 и просадку throughput там, где в этом нет необходимости.

**б) Гонка на room-charge.** Между чтением `Booking.status` и записью `Payment{bookingId, ROOM_CHARGE}` нет транзакционной защиты от одновременной отмены брони ресепшеном — в худшем случае получаем начисление на уже отменённую бронь. Не критично (это не блокирует ресурс, как даты номера), но требует сервисной проверки `booking.status != CANCELLED` в момент оплаты; полной гарантии без более строгой изоляции это не даёт — сверка на чек-ауте (п.2) всё равно остаётся необходимым ручным шагом.

**в) In-memory `LoginRateLimiter`.** Уже сегодня документированное ограничение «однонодовый деплой» (`security/LoginRateLimiter.java:12-16`). POS добавляет новый источник логин-трафика — терминалы официантов/кассиров в начале каждой смены. Пока это один инстанс — не проблема; но именно нагрузка POS в пиковые часы обслуживания — вероятный триггер, который заставит вынести лимитер во внешнее хранилище (Redis), если решат горизонтально масштабировать бэкенд.

**г) Отсутствие реального биллинга.** `Payment.method = CARD` — это запись «кассир утверждает, что карта прошла», а не подтверждение от процессинга (в системе нет интеграции с платёжным терминалом/эквайрингом, ровно как сегодня `Booking.status = PAID` — это утверждение персонала, а не факт от платёжного шлюза). `Payment` — это учётный журнал по доверию к персоналу, а не источник истины для аудита платежей; трактовать его иначе — риск.

**д) Раздвоение источника «сколько гость должен».** `Booking.totalPrice` и `Σ Payment(bookingId, ROOM_CHARGE)` физически разнесены без единой агрегирующей колонки. Если на чек-ауте (или в будущем отчёте) кто-то напишет запрос, читающий только `Booking.totalPrice`, room-charges молча потеряются. Нужен один общепринятый путь чтения folio (сервисный метод), а не ad hoc SQL в разных местах — сейчас это только предупреждение на будущее, реализовывать не требуется.

**е) `EmailService.sendNewBookingEmail` рассылает всем `User.findAll()`** (`service/EmailService.java:46`) без фильтра по роли. Если `WAITER`/`CASHIER` заведутся в той же таблице `User`, они начнут получать письма о новых бронях номеров, что им не нужно и не относится к их работе — при внедрении POS-ролей обязательно нужно завести фильтр получателей по роли (например, только `ADMIN`/`MANAGER`), иначе это тихая регрессия для существующей фичи уведомлений.

**ж) Наименование таблицы `Table`.** Технически легитимно (все идентификаторы квотируются), но `"Table"` как имя сущности рядом с зарезервированным SQL-словом путает читаемость кода и логов — стоит рассмотреть имя класса/таблицы `POSTable` или `ServiceTable` уже на этапе проектирования, а не постфактум.

**з) Ролевая иерархия и privilege escalation.** Введение `RoleHierarchy` (`ADMIN > MANAGER > CASHIER > WAITER`) — самое чувствительное место с точки зрения безопасности: направление цепочки должно быть проверено так, чтобы `/users/**` остался строго под `ADMIN`, а `MANAGER` не получил случайно то, что сегодня зарезервировано только под `ADMIN`. Ошибка конфигурации иерархии — это тихая эскалация привилегий, а не просто баг функциональности.
