--
-- PostgreSQL database dump
--


-- Dumped from database version 16.14
-- Dumped by pg_dump version 16.14

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: AuditAction; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public."AuditAction" AS ENUM (
    'BOOKING_CREATED',
    'BOOKING_STATUS_CHANGED',
    'BOOKING_PAYMENT_NOTE_CHANGED',
    'BOOKING_SCHEDULE_CHANGED',
    'BOOKING_ROOM_ASSIGNED',
    'BOOKINGS_EXPORTED',
    'ROOM_PRICE_CHANGED',
    'RATE_OVERRIDE_CHANGED',
    'ORDER_CLOSED',
    'ORDER_CANCELLED',
    'ROOM_CHARGE_POSTED',
    'SHIFT_OPENED',
    'SHIFT_CLOSED',
    'SHIFT_EXPORTED',
    'USER_CREATED',
    'USER_ROLE_CHANGED',
    'USER_ACTIVE_CHANGED',
    'USER_PASSWORD_RESET',
    'ROOM_UNIT_CREATED',
    'ROOM_UNIT_UPDATED',
    'ROOM_UNIT_DELETED',
    'ROOM_UNIT_BLOCK_CREATED',
    'ROOM_UNIT_BLOCK_DELETED',
    'BOOKING_RELOCATED',
    'BOOKING_RELOCATION_UNDONE'
);


--
-- Name: AuditEntityType; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public."AuditEntityType" AS ENUM (
    'BOOKING',
    'ROOM',
    'ORDER',
    'SHIFT',
    'USER',
    'ROOM_UNIT'
);


--
-- Name: BookingSource; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public."BookingSource" AS ENUM (
    'PUBLIC',
    'STAFF'
);


--
-- Name: BookingStatus; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public."BookingStatus" AS ENUM (
    'NEW',
    'CONFIRMED',
    'PAID',
    'CANCELLED'
);


--
-- Name: MenuDepartment; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public."MenuDepartment" AS ENUM (
    'KITCHEN',
    'BAR'
);


--
-- Name: OrderStatus; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public."OrderStatus" AS ENUM (
    'OPEN',
    'SENT',
    'PAID',
    'CANCELLED'
);


--
-- Name: PaymentMethod; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public."PaymentMethod" AS ENUM (
    'CASH',
    'CARD',
    'ROOM_CHARGE',
    'OTHER'
);


--
-- Name: PrintDocumentType; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public."PrintDocumentType" AS ENUM (
    'KITCHEN_TICKET',
    'PREBILL',
    'GUEST_RECEIPT',
    'Z_REPORT',
    'TEST_PAGE',
    'BAR_TICKET'
);


--
-- Name: PrintJobStatus; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public."PrintJobStatus" AS ENUM (
    'PENDING',
    'SENT',
    'FAILED'
);


--
-- Name: PrinterCodepage; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public."PrinterCodepage" AS ENUM (
    'PC437',
    'TIS620'
);


--
-- Name: PrinterDepartment; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public."PrinterDepartment" AS ENUM (
    'KITCHEN',
    'BAR',
    'CASHIER'
);


--
-- Name: Role; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public."Role" AS ENUM (
    'ADMIN',
    'MANAGER',
    'CASHIER',
    'WAITER'
);


--
-- Name: ShiftStatus; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public."ShiftStatus" AS ENUM (
    'OPEN',
    'CLOSED'
);


--
-- Name: Zone; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public."Zone" AS ENUM (
    'RESTAURANT',
    'BAR',
    'SPA',
    'POOL',
    'ROOM_SERVICE'
);


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: AuditLog; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."AuditLog" (
    id text NOT NULL,
    "actorUserId" text NOT NULL,
    "actorEmail" text NOT NULL,
    "actorRole" public."Role" NOT NULL,
    action public."AuditAction" NOT NULL,
    "entityType" public."AuditEntityType" NOT NULL,
    "entityId" text,
    summary text NOT NULL,
    "createdAt" timestamp(3) without time zone DEFAULT now() NOT NULL
);


--
-- Name: Booking; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."Booking" (
    id text NOT NULL,
    "roomId" text NOT NULL,
    "guestName" text NOT NULL,
    "guestEmail" text,
    "guestPhone" text,
    "checkIn" date NOT NULL,
    "checkOut" date NOT NULL,
    "totalPrice" numeric(10,2) NOT NULL,
    status public."BookingStatus" DEFAULT 'NEW'::public."BookingStatus" NOT NULL,
    "paymentNote" text,
    "createdAt" timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "roomUnitId" text,
    source public."BookingSource" DEFAULT 'PUBLIC'::public."BookingSource" NOT NULL,
    "expiryReminderSent" boolean DEFAULT false NOT NULL
);


--
-- Name: BookingSegment; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."BookingSegment" (
    id text NOT NULL,
    "bookingId" text NOT NULL,
    "roomId" text NOT NULL,
    "roomUnitId" text,
    "checkIn" date NOT NULL,
    "checkOut" date NOT NULL,
    "totalPrice" numeric(10,2) NOT NULL,
    "createdAt" timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT "BookingSegment_date_range_check" CHECK (("checkIn" < "checkOut"))
);


--
-- Name: MenuItem; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."MenuItem" (
    id text NOT NULL,
    name text NOT NULL,
    description text NOT NULL,
    category text NOT NULL,
    price numeric(10,2) NOT NULL,
    "isAvailable" boolean DEFAULT true NOT NULL,
    "createdAt" timestamp(3) without time zone DEFAULT now() NOT NULL,
    department public."MenuDepartment" DEFAULT 'KITCHEN'::public."MenuDepartment" NOT NULL
);


--
-- Name: Order; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."Order" (
    id text NOT NULL,
    "tableId" text,
    "bookingId" text,
    "guestName" text,
    status public."OrderStatus" DEFAULT 'OPEN'::public."OrderStatus" NOT NULL,
    "openedByUserId" text NOT NULL,
    total numeric(10,2) DEFAULT 0 NOT NULL,
    note text,
    "createdAt" timestamp(3) without time zone DEFAULT now() NOT NULL,
    "updatedAt" timestamp(3) without time zone DEFAULT now() NOT NULL
);


--
-- Name: OrderItem; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."OrderItem" (
    id text NOT NULL,
    "orderId" text NOT NULL,
    "menuItemId" text NOT NULL,
    quantity integer NOT NULL,
    "unitPrice" numeric(10,2) NOT NULL,
    note text,
    "createdAt" timestamp(3) without time zone DEFAULT now() NOT NULL,
    "sentAt" timestamp without time zone
);


--
-- Name: Payment; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."Payment" (
    id text NOT NULL,
    "orderId" text NOT NULL,
    method public."PaymentMethod" NOT NULL,
    amount numeric(10,2) NOT NULL,
    "bookingId" text,
    "recordedByUserId" text NOT NULL,
    "shiftId" text NOT NULL,
    "createdAt" timestamp(3) without time zone DEFAULT now() NOT NULL
);


--
-- Name: PosTable; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."PosTable" (
    id text NOT NULL,
    zone public."Zone" NOT NULL,
    label text NOT NULL,
    capacity integer NOT NULL,
    "isActive" boolean DEFAULT true NOT NULL
);


--
-- Name: PrintJob; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."PrintJob" (
    id text NOT NULL,
    "printerId" text NOT NULL,
    "documentType" public."PrintDocumentType" NOT NULL,
    summary text NOT NULL,
    payload bytea NOT NULL,
    status public."PrintJobStatus" DEFAULT 'PENDING'::public."PrintJobStatus" NOT NULL,
    attempts integer DEFAULT 0 NOT NULL,
    "lastError" text,
    "createdAt" timestamp(3) without time zone DEFAULT now() NOT NULL,
    "updatedAt" timestamp(3) without time zone DEFAULT now() NOT NULL
);


--
-- Name: Printer; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."Printer" (
    id text NOT NULL,
    name text NOT NULL,
    department public."PrinterDepartment" NOT NULL,
    host text NOT NULL,
    port integer DEFAULT 9100 NOT NULL,
    codepage public."PrinterCodepage" DEFAULT 'PC437'::public."PrinterCodepage" NOT NULL,
    "isActive" boolean DEFAULT true NOT NULL,
    "createdAt" timestamp(3) without time zone DEFAULT now() NOT NULL
);


--
-- Name: RatePlan; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."RatePlan" (
    id text NOT NULL,
    "roomId" text NOT NULL,
    date date NOT NULL,
    price numeric(10,2) NOT NULL,
    "createdAt" timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: Room; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."Room" (
    id text NOT NULL,
    name text NOT NULL,
    description text NOT NULL,
    capacity integer NOT NULL,
    "basePrice" numeric(10,2) NOT NULL,
    images text[] DEFAULT ARRAY[]::text[],
    "createdAt" timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: RoomUnit; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."RoomUnit" (
    id text NOT NULL,
    "roomId" text NOT NULL,
    label text NOT NULL,
    "isActive" boolean DEFAULT true NOT NULL,
    "createdAt" timestamp(3) without time zone DEFAULT now() NOT NULL
);


--
-- Name: RoomUnitBlock; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."RoomUnitBlock" (
    id text NOT NULL,
    "roomUnitId" text NOT NULL,
    "fromDate" date NOT NULL,
    "toDate" date NOT NULL,
    reason text NOT NULL,
    "createdAt" timestamp(3) without time zone DEFAULT now() NOT NULL,
    CONSTRAINT "RoomUnitBlock_date_range_check" CHECK (("fromDate" <= "toDate"))
);


--
-- Name: Shift; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."Shift" (
    id text NOT NULL,
    "openedByUserId" text NOT NULL,
    "openedAt" timestamp(3) without time zone DEFAULT now() NOT NULL,
    "closedByUserId" text,
    "closedAt" timestamp(3) without time zone,
    "openingCashFloat" numeric(10,2),
    "closingCashCounted" numeric(10,2),
    status public."ShiftStatus" DEFAULT 'OPEN'::public."ShiftStatus" NOT NULL,
    notes text
);


--
-- Name: User; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."User" (
    id text NOT NULL,
    email text NOT NULL,
    "passwordHash" text NOT NULL,
    role public."Role" DEFAULT 'MANAGER'::public."Role" NOT NULL,
    "createdAt" timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "isActive" boolean DEFAULT true NOT NULL,
    "tokenVersion" integer DEFAULT 0 NOT NULL
);


--
-- Name: _prisma_migrations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public._prisma_migrations (
    id character varying(36) NOT NULL,
    checksum character varying(64) NOT NULL,
    finished_at timestamp with time zone,
    migration_name character varying(255) NOT NULL,
    logs text,
    rolled_back_at timestamp with time zone,
    started_at timestamp with time zone DEFAULT now() NOT NULL,
    applied_steps_count integer DEFAULT 0 NOT NULL
);


--
-- Name: flyway_schema_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.flyway_schema_history (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);


--
-- Name: AuditLog AuditLog_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."AuditLog"
    ADD CONSTRAINT "AuditLog_pkey" PRIMARY KEY (id);


--
-- Name: BookingSegment BookingSegment_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."BookingSegment"
    ADD CONSTRAINT "BookingSegment_pkey" PRIMARY KEY (id);


--
-- Name: Booking Booking_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."Booking"
    ADD CONSTRAINT "Booking_pkey" PRIMARY KEY (id);


--
-- Name: MenuItem MenuItem_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."MenuItem"
    ADD CONSTRAINT "MenuItem_pkey" PRIMARY KEY (id);


--
-- Name: OrderItem OrderItem_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."OrderItem"
    ADD CONSTRAINT "OrderItem_pkey" PRIMARY KEY (id);


--
-- Name: Order Order_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."Order"
    ADD CONSTRAINT "Order_pkey" PRIMARY KEY (id);


--
-- Name: Payment Payment_orderId_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."Payment"
    ADD CONSTRAINT "Payment_orderId_key" UNIQUE ("orderId");


--
-- Name: Payment Payment_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."Payment"
    ADD CONSTRAINT "Payment_pkey" PRIMARY KEY (id);


--
-- Name: PosTable PosTable_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."PosTable"
    ADD CONSTRAINT "PosTable_pkey" PRIMARY KEY (id);


--
-- Name: PrintJob PrintJob_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."PrintJob"
    ADD CONSTRAINT "PrintJob_pkey" PRIMARY KEY (id);


--
-- Name: Printer Printer_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."Printer"
    ADD CONSTRAINT "Printer_pkey" PRIMARY KEY (id);


--
-- Name: RatePlan RatePlan_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."RatePlan"
    ADD CONSTRAINT "RatePlan_pkey" PRIMARY KEY (id);


--
-- Name: RoomUnitBlock RoomUnitBlock_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."RoomUnitBlock"
    ADD CONSTRAINT "RoomUnitBlock_pkey" PRIMARY KEY (id);


--
-- Name: RoomUnit RoomUnit_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."RoomUnit"
    ADD CONSTRAINT "RoomUnit_pkey" PRIMARY KEY (id);


--
-- Name: Room Room_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."Room"
    ADD CONSTRAINT "Room_pkey" PRIMARY KEY (id);


--
-- Name: Shift Shift_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."Shift"
    ADD CONSTRAINT "Shift_pkey" PRIMARY KEY (id);


--
-- Name: User User_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."User"
    ADD CONSTRAINT "User_pkey" PRIMARY KEY (id);


--
-- Name: _prisma_migrations _prisma_migrations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public._prisma_migrations
    ADD CONSTRAINT _prisma_migrations_pkey PRIMARY KEY (id);


--
-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);


--
-- Name: AuditLog_action_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "AuditLog_action_idx" ON public."AuditLog" USING btree (action);


--
-- Name: AuditLog_actorUserId_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "AuditLog_actorUserId_idx" ON public."AuditLog" USING btree ("actorUserId");


--
-- Name: AuditLog_createdAt_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "AuditLog_createdAt_idx" ON public."AuditLog" USING btree ("createdAt");


--
-- Name: AuditLog_entityType_entityId_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "AuditLog_entityType_entityId_idx" ON public."AuditLog" USING btree ("entityType", "entityId");


--
-- Name: BookingSegment_bookingId_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "BookingSegment_bookingId_idx" ON public."BookingSegment" USING btree ("bookingId");


--
-- Name: BookingSegment_roomId_checkIn_checkOut_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "BookingSegment_roomId_checkIn_checkOut_idx" ON public."BookingSegment" USING btree ("roomId", "checkIn", "checkOut");


--
-- Name: BookingSegment_roomUnitId_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "BookingSegment_roomUnitId_idx" ON public."BookingSegment" USING btree ("roomUnitId");


--
-- Name: Booking_roomId_checkIn_checkOut_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "Booking_roomId_checkIn_checkOut_idx" ON public."Booking" USING btree ("roomId", "checkIn", "checkOut");


--
-- Name: Booking_roomUnitId_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "Booking_roomUnitId_idx" ON public."Booking" USING btree ("roomUnitId");


--
-- Name: Booking_status_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "Booking_status_idx" ON public."Booking" USING btree (status);


--
-- Name: OrderItem_menuItemId_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "OrderItem_menuItemId_idx" ON public."OrderItem" USING btree ("menuItemId");


--
-- Name: OrderItem_orderId_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "OrderItem_orderId_idx" ON public."OrderItem" USING btree ("orderId");


--
-- Name: Order_bookingId_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "Order_bookingId_idx" ON public."Order" USING btree ("bookingId");


--
-- Name: Order_status_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "Order_status_idx" ON public."Order" USING btree (status);


--
-- Name: Order_tableId_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "Order_tableId_idx" ON public."Order" USING btree ("tableId");


--
-- Name: Payment_bookingId_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "Payment_bookingId_idx" ON public."Payment" USING btree ("bookingId");


--
-- Name: Payment_orderId_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "Payment_orderId_idx" ON public."Payment" USING btree ("orderId");


--
-- Name: Payment_shiftId_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "Payment_shiftId_idx" ON public."Payment" USING btree ("shiftId");


--
-- Name: PrintJob_printerId_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "PrintJob_printerId_idx" ON public."PrintJob" USING btree ("printerId");


--
-- Name: PrintJob_status_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "PrintJob_status_idx" ON public."PrintJob" USING btree (status);


--
-- Name: Printer_one_active_per_department; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX "Printer_one_active_per_department" ON public."Printer" USING btree (department) WHERE ("isActive" = true);


--
-- Name: RatePlan_roomId_date_key; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX "RatePlan_roomId_date_key" ON public."RatePlan" USING btree ("roomId", date);


--
-- Name: RoomUnitBlock_roomUnitId_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "RoomUnitBlock_roomUnitId_idx" ON public."RoomUnitBlock" USING btree ("roomUnitId");


--
-- Name: RoomUnit_label_key; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX "RoomUnit_label_key" ON public."RoomUnit" USING btree (label);


--
-- Name: RoomUnit_roomId_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "RoomUnit_roomId_idx" ON public."RoomUnit" USING btree ("roomId");


--
-- Name: Shift_one_open_per_user; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX "Shift_one_open_per_user" ON public."Shift" USING btree ("openedByUserId") WHERE (status = 'OPEN'::public."ShiftStatus");


--
-- Name: User_email_key; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX "User_email_key" ON public."User" USING btree (email);


--
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);


--
-- Name: BookingSegment BookingSegment_bookingId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."BookingSegment"
    ADD CONSTRAINT "BookingSegment_bookingId_fkey" FOREIGN KEY ("bookingId") REFERENCES public."Booking"(id) ON DELETE CASCADE;


--
-- Name: BookingSegment BookingSegment_roomId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."BookingSegment"
    ADD CONSTRAINT "BookingSegment_roomId_fkey" FOREIGN KEY ("roomId") REFERENCES public."Room"(id);


--
-- Name: BookingSegment BookingSegment_roomUnitId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."BookingSegment"
    ADD CONSTRAINT "BookingSegment_roomUnitId_fkey" FOREIGN KEY ("roomUnitId") REFERENCES public."RoomUnit"(id);


--
-- Name: Booking Booking_roomId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."Booking"
    ADD CONSTRAINT "Booking_roomId_fkey" FOREIGN KEY ("roomId") REFERENCES public."Room"(id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: Booking Booking_roomUnitId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."Booking"
    ADD CONSTRAINT "Booking_roomUnitId_fkey" FOREIGN KEY ("roomUnitId") REFERENCES public."RoomUnit"(id);


--
-- Name: OrderItem OrderItem_menuItemId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."OrderItem"
    ADD CONSTRAINT "OrderItem_menuItemId_fkey" FOREIGN KEY ("menuItemId") REFERENCES public."MenuItem"(id);


--
-- Name: OrderItem OrderItem_orderId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."OrderItem"
    ADD CONSTRAINT "OrderItem_orderId_fkey" FOREIGN KEY ("orderId") REFERENCES public."Order"(id) ON DELETE CASCADE;


--
-- Name: Order Order_bookingId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."Order"
    ADD CONSTRAINT "Order_bookingId_fkey" FOREIGN KEY ("bookingId") REFERENCES public."Booking"(id);


--
-- Name: Order Order_openedByUserId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."Order"
    ADD CONSTRAINT "Order_openedByUserId_fkey" FOREIGN KEY ("openedByUserId") REFERENCES public."User"(id);


--
-- Name: Order Order_tableId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."Order"
    ADD CONSTRAINT "Order_tableId_fkey" FOREIGN KEY ("tableId") REFERENCES public."PosTable"(id) ON DELETE SET NULL;


--
-- Name: Payment Payment_bookingId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."Payment"
    ADD CONSTRAINT "Payment_bookingId_fkey" FOREIGN KEY ("bookingId") REFERENCES public."Booking"(id);


--
-- Name: Payment Payment_orderId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."Payment"
    ADD CONSTRAINT "Payment_orderId_fkey" FOREIGN KEY ("orderId") REFERENCES public."Order"(id);


--
-- Name: Payment Payment_recordedByUserId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."Payment"
    ADD CONSTRAINT "Payment_recordedByUserId_fkey" FOREIGN KEY ("recordedByUserId") REFERENCES public."User"(id);


--
-- Name: Payment Payment_shiftId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."Payment"
    ADD CONSTRAINT "Payment_shiftId_fkey" FOREIGN KEY ("shiftId") REFERENCES public."Shift"(id);


--
-- Name: PrintJob PrintJob_printerId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."PrintJob"
    ADD CONSTRAINT "PrintJob_printerId_fkey" FOREIGN KEY ("printerId") REFERENCES public."Printer"(id);


--
-- Name: RatePlan RatePlan_roomId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."RatePlan"
    ADD CONSTRAINT "RatePlan_roomId_fkey" FOREIGN KEY ("roomId") REFERENCES public."Room"(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: RoomUnitBlock RoomUnitBlock_roomUnitId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."RoomUnitBlock"
    ADD CONSTRAINT "RoomUnitBlock_roomUnitId_fkey" FOREIGN KEY ("roomUnitId") REFERENCES public."RoomUnit"(id) ON DELETE CASCADE;


--
-- Name: RoomUnit RoomUnit_roomId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."RoomUnit"
    ADD CONSTRAINT "RoomUnit_roomId_fkey" FOREIGN KEY ("roomId") REFERENCES public."Room"(id);


--
-- Name: Shift Shift_closedByUserId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."Shift"
    ADD CONSTRAINT "Shift_closedByUserId_fkey" FOREIGN KEY ("closedByUserId") REFERENCES public."User"(id);


--
-- Name: Shift Shift_openedByUserId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."Shift"
    ADD CONSTRAINT "Shift_openedByUserId_fkey" FOREIGN KEY ("openedByUserId") REFERENCES public."User"(id);


--
-- PostgreSQL database dump complete
--


--
-- PostgreSQL database dump
--


-- Dumped from database version 16.14
-- Dumped by pg_dump version 16.14

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Data for Name: flyway_schema_history; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (1, '1', 'Pre-Flyway schema (created by Prisma migrations)', 'BASELINE', 'Pre-Flyway schema (created by Prisma migrations)', NULL, 'sunsetbeach', '2026-08-07 16:01:33.619061', 0, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (2, '2', 'add pos roles', 'SQL', 'V2__add_pos_roles.sql', -289401340, 'sunsetbeach', '2026-08-07 16:01:33.711571', 9, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (3, '3', 'pos module', 'SQL', 'V3__pos_module.sql', -402720331, 'sunsetbeach', '2026-08-07 16:01:33.75137', 193, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (4, '4', 'room quantity and availability capacity', 'SQL', 'V4__room_quantity_and_availability_capacity.sql', -1793132906, 'sunsetbeach', '2026-08-16 19:39:47.902926', 42, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (5, '5', 'printers and print jobs', 'SQL', 'V5__printers_and_print_jobs.sql', -1063600715, 'sunsetbeach', '2026-08-16 21:14:48.243659', 111, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (6, '6', 'add bar ticket document type', 'SQL', 'V6__add_bar_ticket_document_type.sql', 372073248, 'sunsetbeach', '2026-08-17 08:41:20.732231', 14, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (7, '7', 'backfill bar ticket document type', 'SQL', 'V7__backfill_bar_ticket_document_type.sql', 603488721, 'sunsetbeach', '2026-08-17 08:41:20.781842', 15, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (8, '8', 'physical room units', 'SQL', 'V8__physical_room_units.sql', 379262078, 'sunsetbeach', '2026-08-17 18:46:06.099272', 79, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (9, '9', 'backfill room units from quantity', 'SQL', 'V9__backfill_room_units_from_quantity.sql', -1789704355, 'sunsetbeach', '2026-08-17 18:46:06.220656', 11, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (10, '10', 'migrate availability blocks to room units', 'SQL', 'V10__migrate_availability_blocks_to_room_units.sql', -1495654860, 'sunsetbeach', '2026-08-17 18:46:06.253366', 9, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (11, '11', 'booking room unit and cleanup', 'SQL', 'V11__booking_room_unit_and_cleanup.sql', 749934238, 'sunsetbeach', '2026-08-17 18:46:06.286365', 25, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (12, '12', 'booking source', 'SQL', 'V12__booking_source.sql', 1606721862, 'sunsetbeach', '2026-08-29 20:51:02.91777', 29, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (13, '13', 'user active and token version', 'SQL', 'V13__user_active_and_token_version.sql', -156468621, 'sunsetbeach', '2026-08-29 20:51:02.99047', 11, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (14, '14', 'unique payment per order', 'SQL', 'V14__unique_payment_per_order.sql', 564520482, 'sunsetbeach', '2026-08-29 20:51:03.026577', 18, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (15, '15', 'booking expiry reminder', 'SQL', 'V15__booking_expiry_reminder.sql', -1589014794, 'sunsetbeach', '2026-08-29 21:31:32.920515', 17, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (16, '16', 'audit log', 'SQL', 'V16__audit_log.sql', 61930974, 'sunsetbeach', '2026-08-30 12:41:05.268485', 69, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (17, '17', 'nullable walkin guest contact', 'SQL', 'V17__nullable_walkin_guest_contact.sql', -1902839019, 'sunsetbeach', '2026-08-30 12:41:05.381613', 10, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (18, '18', 'booking segments', 'SQL', 'V18__booking_segments.sql', 953234474, 'sunsetbeach', '2026-08-30 18:22:28.58445', 54, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (19, '19', 'booking relocation audit actions', 'SQL', 'V19__booking_relocation_audit_actions.sql', 1588711728, 'sunsetbeach', '2026-08-30 18:22:28.677296', 8, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (20, '20', 'order item sent at', 'SQL', 'V20__order_item_sent_at.sql', 1763124512, 'sunsetbeach', '2026-08-31 21:00:28.401609', 22, true);


--
-- PostgreSQL database dump complete
--


