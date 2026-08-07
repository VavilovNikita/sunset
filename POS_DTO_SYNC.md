# POS: GET /shifts/current + сверка DTO с фронтендом

## 1. GET /shifts/current — добавлено

```
GET /shifts/current   (роль CASHIER+)
→ 200 Shift   (собственная OPEN смена вызывающего — поиск по openedByUserId=<principal> AND status=OPEN)
→ 404         (открытой смены нет)
```

Заведено на существующий матчер `/shifts/**` → `hasRole(CASHIER)` (изменений в `SecurityConfig` не потребовалось — литерал `/shifts/current` в Spring MVC ранжируется как более специфичный, чем `/shifts/{id}`, коллизии нет).

Тесты (`ShiftCurrentTests`, все проходят на реальной dev-БД, откат через `@Transactional`):
- есть открытая смена → возвращается она;
- открытой смены нет → 404;
- есть только закрытая смена → 404;
- открытая смена другого пользователя → не видна (404), несмотря на то что запись физически существует в таблице.

## 2. Схемы дословно (как в openapi.yaml, без изменений)

```yaml
MenuItem:
  type: object
  required: [id, name, description, category, price, isAvailable, createdAt]
  properties:
    id:
      type: string
    name:
      type: string
    description:
      type: string
    category:
      type: string
    price:
      type: string
      description: Decimal(10,2) rendered as a string, e.g. `"250.00"` — same convention as `Room.basePrice`.
    isAvailable:
      type: boolean
    createdAt:
      type: string
      format: date-time

MenuItemInput:
  type: object
  description: Full replacement on PATCH — no partial update (same convention as `RoomInput`).
  required: [name, description, category, price]
  properties:
    name:
      type: string
      minLength: 2
      maxLength: 120
    description:
      type: string
      minLength: 1
      maxLength: 2000
    category:
      type: string
      minLength: 1
      maxLength: 60
    price:
      type: number
      minimum: 0
      exclusiveMinimum: true
    isAvailable:
      type: boolean
      default: true

Table:
  type: object
  required: [id, zone, label, capacity, isActive]
  properties:
    id:
      type: string
    zone:
      $ref: "#/components/schemas/Zone"
    label:
      type: string
    capacity:
      type: integer
    isActive:
      type: boolean

TableInput:
  type: object
  description: Full replacement on PATCH — no partial update.
  required: [zone, label, capacity]
  properties:
    zone:
      $ref: "#/components/schemas/Zone"
    label:
      type: string
      minLength: 1
      maxLength: 60
    capacity:
      type: integer
      minimum: 1
      maximum: 50
    isActive:
      type: boolean
      default: true

OrderItem:
  type: object
  required: [id, orderId, menuItemId, quantity, unitPrice, note, createdAt]
  properties:
    id:
      type: string
    orderId:
      type: string
    menuItemId:
      type: string
    quantity:
      type: integer
    unitPrice:
      type: string
      description: Decimal(10,2) rendered as a string, snapshotted at insert time.
    note:
      type: string
      nullable: true
    createdAt:
      type: string
      format: date-time

Order:
  type: object
  required: [id, tableId, bookingId, guestName, status, openedByUserId, total, note, items, createdAt, updatedAt]
  properties:
    id:
      type: string
    tableId:
      type: string
      nullable: true
    bookingId:
      type: string
      nullable: true
    guestName:
      type: string
      nullable: true
    status:
      $ref: "#/components/schemas/OrderStatus"
    openedByUserId:
      type: string
    total:
      type: string
      description: Decimal(10,2) rendered as a string, e.g. `"1250.00"`.
    note:
      type: string
      nullable: true
    items:
      type: array
      items:
        $ref: "#/components/schemas/OrderItem"
    createdAt:
      type: string
      format: date-time
    updatedAt:
      type: string
      format: date-time

Shift:
  type: object
  required: [id, openedByUserId, openedAt, closedByUserId, closedAt, openingCashFloat, closingCashCounted, status, notes]
  properties:
    id:
      type: string
    openedByUserId:
      type: string
    openedAt:
      type: string
      format: date-time
    closedByUserId:
      type: string
      nullable: true
    closedAt:
      type: string
      format: date-time
      nullable: true
    openingCashFloat:
      type: string
      nullable: true
      description: Decimal(10,2) rendered as a string.
    closingCashCounted:
      type: string
      nullable: true
      description: Decimal(10,2) rendered as a string.
    status:
      $ref: "#/components/schemas/ShiftStatus"
    notes:
      type: string
      nullable: true

ShiftTotals:
  type: object
  required: [cash, card, roomCharge, other, paymentCount]
  properties:
    cash:
      type: string
      description: Decimal(10,2) rendered as a string.
    card:
      type: string
      description: Decimal(10,2) rendered as a string.
    roomCharge:
      type: string
      description: Decimal(10,2) rendered as a string.
    other:
      type: string
      description: Decimal(10,2) rendered as a string.
    paymentCount:
      type: integer

ShiftSummary:
  type: object
  required: [id, openedByUserId, openedAt, closedByUserId, closedAt, openingCashFloat, closingCashCounted, status, notes, totals]
  properties:
    id: { type: string }
    openedByUserId: { type: string }
    openedAt: { type: string, format: date-time }
    closedByUserId: { type: string, nullable: true }
    closedAt: { type: string, format: date-time, nullable: true }
    openingCashFloat: { type: string, nullable: true }
    closingCashCounted: { type: string, nullable: true }
    status: { $ref: "#/components/schemas/ShiftStatus" }
    notes: { type: string, nullable: true }
    totals: { $ref: "#/components/schemas/ShiftTotals" }

Payment:
  type: object
  required: [id, orderId, method, amount, bookingId, recordedByUserId, shiftId, createdAt]
  properties:
    id:
      type: string
    orderId:
      type: string
    method:
      $ref: "#/components/schemas/PaymentMethod"
    amount:
      type: string
      description: Decimal(10,2) rendered as a string.
    bookingId:
      type: string
      nullable: true
      description: Set only when `method` is `ROOM_CHARGE`.
    recordedByUserId:
      type: string
    shiftId:
      type: string
    createdAt:
      type: string
      format: date-time
```

## 3. Расхождения / на что обратить внимание фронтенду

1. **`Payment` не возвращается напрямую из `POST /orders/{id}/close`.** Ответ этого эндпоинта — `Order` (со статусом `PAID`), не `Payment`. Поля созданного платежа (`id`/`shiftId`/`createdAt`) нигде в этом ответе не отражаются — если UI хочет показать «оплачено картой, ฿500» сразу после закрытия, брать нужно то, что сам только что отправил, а не то, что вернул сервер. `Payment` всплывает только через `GET /shifts/{id}` (`ShiftSummary.totals`, агрегировано, без построчных платежей), `GET /bookings/{id}/pos-orders` (частично: `orderId`/`amount`/`paidAt`/`items`, только ROOM_CHARGE) и CSV-экспорт смены.

2. **Decimal-поля — `string` на выходе, `number` на входе**, не единообразно:
   - Выход (string): `MenuItem.price`, `Order.total`, `OrderItem.unitPrice`, `Payment.amount`, `Shift.openingCashFloat`/`closingCashCounted`, `ShiftTotals.*`
   - Вход (number): `MenuItemInput.price`, `CloseOrderInput.amount`, `ShiftOpenInput.openingCashFloat`, `ShiftCloseInput.closingCashCounted`
   
   Это соответствует уже существующему соглашению `Room.basePrice`/`RoomInput.basePrice`, так что не баг — но одно и то же денежное значение имеет разный JSON-тип в зависимости от направления, легко перепутать в ручных фронтенд-типах.

3. **`GET /shifts/current` возвращает `Shift`, не `ShiftSummary`** — без `totals`. Если для бейджа нужны текущие суммы по смене, после получения `id` нужен отдельный `GET /shifts/{id}`.

4. **`Order` всегда содержит `items[]`**, и в списке (`GET /orders`), и в get-by-id (`GET /orders/{id}`). В исходном черновике контракта `items[]` был явно упомянут только для get-by-id — решено сделать единую форму для списка и деталей, чтобы не заводить отдельную `OrderSummary`. Не полагайтесь на то, что строки списка приходят без `items`.

5. **`OrderItem` содержит только `menuItemId`, без встроенного названия/цены за пределами `unitPrice`.** Чтобы отрисовать состав заказа с названиями блюд, нужен клиентский join с `GET /menu`. (В отличие от `BookingPosOrder.items[]` из folio-эндпоинта, который отдаёт уже разрешённое `name` — это отдельная, специально сделанная под отображение форма именно для того эндпоинта.)

6. Все имена полей — camelCase, snake_case нигде не встречается — несоответствий нет.
