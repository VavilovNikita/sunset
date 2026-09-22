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
-- Name: btree_gist; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS btree_gist WITH SCHEMA public;


--
-- Name: EXTENSION btree_gist; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON EXTENSION btree_gist IS 'support for indexing common datatypes in GiST';


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
    'BOOKING_RELOCATION_UNDONE',
    'BOOKING_REPRICED',
    'BOOKING_CHECKED_IN',
    'BOOKING_CHECKED_OUT',
    'BOOKING_NO_SHOW_MARKED',
    'ROOM_UNIT_HOUSEKEEPING_CHANGED',
    'BOOKING_FOLIO_PAYMENT_RECORDED',
    'PROPERTY_MAP_IMAGE_UPDATED',
    'ROOM_UNIT_POSITION_UPDATED',
    'PRINT_JOB_DISMISSED',
    'USER_FUNCTIONS_CHANGED',
    'MAINTENANCE_TASK_CREATED',
    'MAINTENANCE_TASK_STATUS_CHANGED',
    'MAINTENANCE_TASK_BLOCKED',
    'MAINTENANCE_TASK_BLOCK_LIFTED',
    'SPA_APPOINTMENT_CREATED',
    'SPA_APPOINTMENT_STATUS_CHANGED',
    'POS_TABLE_POSITION_UPDATED',
    'GUEST_CREATED',
    'GUEST_UPDATED',
    'GUEST_DELETED',
    'BOOKING_GUEST_LINKED',
    'SPA_MAP_IMAGE_UPDATED',
    'BOOKING_SEGMENT_ROOM_CHANGED',
    'BOOKING_ROOMS_SWAPPED',
    'SPA_APPOINTMENT_RESCHEDULED',
    'SPA_APPOINTMENT_TREATMENT_ADDED',
    'SPA_APPOINTMENT_TREATMENT_REMOVED',
    'SPA_APPOINTMENT_TABLES_SWAPPED',
    'SHIFT_CODE_CREATED',
    'EMPLOYEE_PATTERN_CHANGED',
    'ROSTER_ENTRY_CREATED',
    'ROSTER_ENTRY_MOVED',
    'ROSTER_ENTRY_REASSIGNED',
    'ROSTER_ENTRIES_SWAPPED',
    'ROSTER_ENTRY_DELETED',
    'ROSTER_ENTRY_LOCKED_CHANGED',
    'ROSTER_MONTH_GENERATED',
    'STAFF_AREA_COVERAGE_RULE_CHANGED',
    'ATTENDANCE_PUNCH_RECORDED',
    'ATTENDANCE_PUNCH_CORRECTED',
    'EMPLOYEE_PAY_RATE_CHANGED',
    'ROSTER_ACTUALS_EXPORTED',
    'USER_CREDENTIALS_GRANTED',
    'USER_OVERTIME_ELIGIBILITY_CHANGED',
    'USER_ENROLLMENT_NUMBER_CHANGED',
    'ROSTER_MONTH_IMPORTED',
    'USER_STAFF_AREA_CHANGED',
    'SHIFT_CODE_KIND_CHANGED',
    'SHIFT_CODE_DISPLAY_COLOR_CHANGED',
    'ROSTER_GRID_EXPORTED'
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
    'ROOM_UNIT',
    'PROPERTY_MAP',
    'PRINT_JOB',
    'MAINTENANCE_TASK',
    'TABLE',
    'SPA_APPOINTMENT',
    'GUEST',
    'SPA_MAP',
    'SHIFT_CODE',
    'ROSTER_ENTRY',
    'STAFF_AREA_COVERAGE_RULE',
    'ATTENDANCE_PUNCH',
    'EMPLOYEE_PAY_RATE'
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
-- Name: FillColor; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public."FillColor" AS ENUM (
    'YELLOW',
    'BLUE'
);


--
-- Name: FolioPaymentMethod; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public."FolioPaymentMethod" AS ENUM (
    'CASH',
    'CARD',
    'OTHER'
);


--
-- Name: HousekeepingStatus; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public."HousekeepingStatus" AS ENUM (
    'DIRTY',
    'CLEAN'
);


--
-- Name: MaintenanceTaskStatus; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public."MaintenanceTaskStatus" AS ENUM (
    'OPEN',
    'IN_PROGRESS',
    'DONE'
);


--
-- Name: MenuDepartment; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public."MenuDepartment" AS ENUM (
    'KITCHEN',
    'BAR',
    'SPA'
);


--
-- Name: OccupancyStatus; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public."OccupancyStatus" AS ENUM (
    'EXPECTED',
    'CHECKED_IN',
    'CHECKED_OUT',
    'NO_SHOW'
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
-- Name: PunchDirection; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public."PunchDirection" AS ENUM (
    'IN',
    'OUT'
);


--
-- Name: PunchSource; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public."PunchSource" AS ENUM (
    'MANUAL',
    'SCANNER'
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
-- Name: ShiftCodeKind; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public."ShiftCodeKind" AS ENUM (
    'MORNING',
    'SPLIT',
    'EVENING',
    'OPEN_SCHEDULE',
    'ABSENCE'
);


--
-- Name: ShiftStatus; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public."ShiftStatus" AS ENUM (
    'OPEN',
    'CLOSED'
);


--
-- Name: SpaAppointmentStatus; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public."SpaAppointmentStatus" AS ENUM (
    'BOOKED',
    'COMPLETED',
    'CANCELLED',
    'NO_SHOW'
);


--
-- Name: StaffArea; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public."StaffArea" AS ENUM (
    'ADMIN',
    'FRONT_OFFICE',
    'MAINTENANCE',
    'HOUSEKEEPING',
    'RESTAURANT',
    'KITCHEN'
);


--
-- Name: Weekday; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public."Weekday" AS ENUM (
    'MONDAY',
    'TUESDAY',
    'WEDNESDAY',
    'THURSDAY',
    'FRIDAY',
    'SATURDAY',
    'SUNDAY'
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
-- Name: AttendanceDevice; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."AttendanceDevice" (
    id text NOT NULL,
    name text NOT NULL,
    serial text NOT NULL,
    address text NOT NULL,
    port integer DEFAULT 4370 NOT NULL,
    timezone text NOT NULL,
    active boolean DEFAULT true NOT NULL,
    "lastSeenAt" timestamp(3) without time zone,
    "createdAt" timestamp(3) without time zone DEFAULT now() NOT NULL,
    "attendanceRecordSize" integer,
    "windowedReadUnsupported" boolean DEFAULT false NOT NULL
);


--
-- Name: AttendancePunch; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."AttendancePunch" (
    id text NOT NULL,
    "employeeUserId" text NOT NULL,
    "punchAt" timestamp(3) without time zone NOT NULL,
    direction public."PunchDirection" NOT NULL,
    source public."PunchSource" NOT NULL,
    "recordedByUserId" text,
    note text,
    "createdAt" timestamp(3) without time zone DEFAULT now() NOT NULL,
    "deviceId" text,
    "enrollmentNumber" integer,
    CONSTRAINT attendance_punch_manual_needs_recorder CHECK (((source <> 'MANUAL'::public."PunchSource") OR ("recordedByUserId" IS NOT NULL))),
    CONSTRAINT attendance_punch_scanner_needs_device CHECK (((source <> 'SCANNER'::public."PunchSource") OR (("deviceId" IS NOT NULL) AND ("enrollmentNumber" IS NOT NULL))))
);


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
    "expiryReminderSent" boolean DEFAULT false NOT NULL,
    "occupancyStatus" public."OccupancyStatus" DEFAULT 'EXPECTED'::public."OccupancyStatus" NOT NULL,
    "checkedInAt" timestamp(3) without time zone,
    "checkedOutAt" timestamp(3) without time zone,
    "guestId" text
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
-- Name: BookingSegmentNightlyRate; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."BookingSegmentNightlyRate" (
    id text NOT NULL,
    "segmentId" text NOT NULL,
    date date NOT NULL,
    price numeric(10,2) NOT NULL,
    "createdAt" timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: EmployeePattern; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."EmployeePattern" (
    "employeeUserId" text NOT NULL,
    "defaultShiftCodeId" text,
    "workDaysPerWeek" integer NOT NULL,
    "weeklyDayOff" public."Weekday" NOT NULL,
    "updatedByUserId" text NOT NULL,
    "updatedAt" timestamp(3) without time zone DEFAULT now() NOT NULL,
    CONSTRAINT "EmployeePattern_workDaysPerWeek_check" CHECK ((("workDaysPerWeek" >= 0) AND ("workDaysPerWeek" <= 7)))
);


--
-- Name: EmployeePayRate; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."EmployeePayRate" (
    id text NOT NULL,
    "employeeUserId" text NOT NULL,
    "dailyRate" numeric(10,2) NOT NULL,
    "effectiveFrom" date NOT NULL,
    "createdByUserId" text NOT NULL,
    "createdAt" timestamp(3) without time zone DEFAULT now() NOT NULL,
    CONSTRAINT "EmployeePayRate_dailyRate_check" CHECK (("dailyRate" >= (0)::numeric))
);


--
-- Name: FolioPayment; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."FolioPayment" (
    id text NOT NULL,
    "bookingId" text NOT NULL,
    method public."FolioPaymentMethod" NOT NULL,
    amount numeric(10,2) NOT NULL,
    "recordedByUserId" text NOT NULL,
    "createdAt" timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: Guest; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."Guest" (
    id text NOT NULL,
    name text NOT NULL,
    email text,
    phone text,
    notes text,
    "createdAt" timestamp(3) without time zone DEFAULT now() NOT NULL,
    "updatedAt" timestamp(3) without time zone DEFAULT now() NOT NULL
);


--
-- Name: GuestAccount; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."GuestAccount" (
    id text NOT NULL,
    email text NOT NULL,
    "passwordHash" text NOT NULL,
    name text,
    "emailVerifiedAt" timestamp(3) without time zone,
    "emailVerificationToken" text,
    "emailVerificationExpiresAt" timestamp(3) without time zone,
    "tokenVersion" integer DEFAULT 0 NOT NULL,
    "createdAt" timestamp(3) without time zone DEFAULT now() NOT NULL,
    "updatedAt" timestamp(3) without time zone DEFAULT now() NOT NULL
);


--
-- Name: MaintenanceTask; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."MaintenanceTask" (
    id text NOT NULL,
    "roomUnitId" text NOT NULL,
    description text NOT NULL,
    status public."MaintenanceTaskStatus" DEFAULT 'OPEN'::public."MaintenanceTaskStatus" NOT NULL,
    "blockId" text,
    "reportedByUserId" text NOT NULL,
    photos text[] DEFAULT '{}'::text[] NOT NULL,
    "createdAt" timestamp(3) without time zone DEFAULT now() NOT NULL,
    "closedAt" timestamp(3) without time zone
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
    department public."MenuDepartment" DEFAULT 'KITCHEN'::public."MenuDepartment" NOT NULL,
    "durationMinutes" integer,
    CONSTRAINT menu_item_duration_positive CHECK ((("durationMinutes" IS NULL) OR ("durationMinutes" > 0)))
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
    "openedByUserId" text,
    total numeric(10,2) DEFAULT 0 NOT NULL,
    note text,
    "createdAt" timestamp(3) without time zone DEFAULT now() NOT NULL,
    "updatedAt" timestamp(3) without time zone DEFAULT now() NOT NULL,
    "guestAccessToken" text
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
    "isActive" boolean DEFAULT true NOT NULL,
    "positionX" numeric(5,4),
    "positionY" numeric(5,4),
    CONSTRAINT pos_table_position_pair CHECK ((("positionX" IS NULL) = ("positionY" IS NULL))),
    CONSTRAINT pos_table_position_range CHECK (((("positionX" IS NULL) OR (("positionX" >= (0)::numeric) AND ("positionX" <= (1)::numeric))) AND (("positionY" IS NULL) OR (("positionY" >= (0)::numeric) AND ("positionY" <= (1)::numeric)))))
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
    "updatedAt" timestamp(3) without time zone DEFAULT now() NOT NULL,
    "dismissedAt" timestamp(3) without time zone,
    "dismissedByUserId" text,
    "dismissNote" text
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
-- Name: PropertyMap; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."PropertyMap" (
    id text NOT NULL,
    "imagePath" text NOT NULL,
    "updatedByUserId" text NOT NULL,
    "updatedAt" timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
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
    "createdAt" timestamp(3) without time zone DEFAULT now() NOT NULL,
    "housekeepingStatus" public."HousekeepingStatus" DEFAULT 'CLEAN'::public."HousekeepingStatus" NOT NULL,
    "positionX" numeric(5,4),
    "positionY" numeric(5,4),
    CONSTRAINT room_unit_position_pair CHECK ((("positionX" IS NULL) = ("positionY" IS NULL))),
    CONSTRAINT room_unit_position_range CHECK (((("positionX" IS NULL) OR (("positionX" >= (0)::numeric) AND ("positionX" <= (1)::numeric))) AND (("positionY" IS NULL) OR (("positionY" >= (0)::numeric) AND ("positionY" <= (1)::numeric)))))
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
    "createdByUserId" text,
    CONSTRAINT "RoomUnitBlock_date_range_check" CHECK (("fromDate" <= "toDate"))
);


--
-- Name: RosterEntry; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."RosterEntry" (
    id text NOT NULL,
    "employeeUserId" text NOT NULL,
    date date NOT NULL,
    "shiftCodeId" text NOT NULL,
    note text,
    locked boolean DEFAULT false NOT NULL,
    "createdByUserId" text NOT NULL,
    "createdAt" timestamp(3) without time zone DEFAULT now() NOT NULL,
    "updatedAt" timestamp(3) without time zone DEFAULT now() NOT NULL
);


--
-- Name: RosterImportNameMapping; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."RosterImportNameMapping" (
    id text NOT NULL,
    "rawName" text NOT NULL,
    "employeeUserId" text NOT NULL,
    "createdByUserId" text NOT NULL,
    "createdAt" timestamp(3) without time zone DEFAULT now() NOT NULL
);


--
-- Name: RosterImportShiftColorMapping; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."RosterImportShiftColorMapping" (
    id text NOT NULL,
    "rawCode" text NOT NULL,
    "fillColor" public."FillColor" NOT NULL,
    "resolvedCode" text NOT NULL,
    "createdByUserId" text NOT NULL,
    "createdAt" timestamp(3) without time zone DEFAULT now() NOT NULL
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
-- Name: ShiftCode; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."ShiftCode" (
    id text NOT NULL,
    "staffArea" public."StaffArea",
    code text NOT NULL,
    "startTime1" time without time zone,
    "endTime1" time without time zone,
    "startTime2" time without time zone,
    "endTime2" time without time zone,
    "countsAsWorked" boolean NOT NULL,
    "isPaid" boolean NOT NULL,
    "effectiveFrom" date NOT NULL,
    active boolean DEFAULT true NOT NULL,
    "createdByUserId" text NOT NULL,
    "createdAt" timestamp(3) without time zone DEFAULT now() NOT NULL,
    kind public."ShiftCodeKind",
    "displayColor" text,
    CONSTRAINT shift_code_interval1_order CHECK ((("startTime1" IS NULL) OR ("endTime1" > "startTime1"))),
    CONSTRAINT shift_code_interval1_pair CHECK ((("startTime1" IS NULL) = ("endTime1" IS NULL))),
    CONSTRAINT shift_code_interval2_needs_interval1 CHECK ((("startTime2" IS NULL) OR ("startTime1" IS NOT NULL))),
    CONSTRAINT shift_code_interval2_order CHECK ((("startTime2" IS NULL) OR ("endTime2" > "startTime2"))),
    CONSTRAINT shift_code_interval2_pair CHECK ((("startTime2" IS NULL) = ("endTime2" IS NULL)))
);


--
-- Name: SpaAppointment; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."SpaAppointment" (
    id text NOT NULL,
    "bookingId" text NOT NULL,
    "tableId" text NOT NULL,
    "therapistUserId" text NOT NULL,
    date date NOT NULL,
    "startTime" time without time zone NOT NULL,
    "durationMinutes" integer NOT NULL,
    status public."SpaAppointmentStatus" DEFAULT 'BOOKED'::public."SpaAppointmentStatus" NOT NULL,
    "orderId" text,
    "createdByUserId" text NOT NULL,
    "cancelledByUserId" text,
    "cancelReason" text,
    "createdAt" timestamp(3) without time zone DEFAULT now() NOT NULL,
    "updatedAt" timestamp(3) without time zone DEFAULT now() NOT NULL,
    CONSTRAINT "SpaAppointment_durationMinutes_check" CHECK (("durationMinutes" > 0))
);


--
-- Name: SpaAppointmentTreatment; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."SpaAppointmentTreatment" (
    id text NOT NULL,
    "spaAppointmentId" text NOT NULL,
    "treatmentMenuItemId" text NOT NULL,
    "durationMinutes" integer NOT NULL,
    "createdAt" timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT "SpaAppointmentTreatment_durationMinutes_check" CHECK (("durationMinutes" > 0))
);


--
-- Name: SpaMap; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."SpaMap" (
    id text NOT NULL,
    "imagePath" text NOT NULL,
    "updatedByUserId" text NOT NULL,
    "updatedAt" timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: StaffAreaCoverageRule; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."StaffAreaCoverageRule" (
    "staffArea" public."StaffArea" NOT NULL,
    "minimumWorking" integer NOT NULL,
    "updatedByUserId" text NOT NULL,
    "updatedAt" timestamp(3) without time zone DEFAULT now() NOT NULL,
    CONSTRAINT "StaffAreaCoverageRule_minimumWorking_check" CHECK (("minimumWorking" >= 0))
);


--
-- Name: User; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."User" (
    id text NOT NULL,
    email text,
    "passwordHash" text,
    role public."Role" DEFAULT 'MANAGER'::public."Role" NOT NULL,
    "createdAt" timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "isActive" boolean DEFAULT true NOT NULL,
    "tokenVersion" integer DEFAULT 0 NOT NULL,
    "jobFunctions" text[] DEFAULT '{}'::text[] NOT NULL,
    name text NOT NULL,
    "overtimeEligible" boolean DEFAULT true NOT NULL,
    "enrollmentNumber" integer,
    "staffArea" public."StaffArea",
    CONSTRAINT user_credentials_paired CHECK (((email IS NULL) = ("passwordHash" IS NULL))),
    CONSTRAINT user_job_functions_valid CHECK (("jobFunctions" <@ ARRAY['ENGINEER'::text, 'HOUSEKEEPER'::text, 'THERAPIST'::text]))
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
-- Name: AttendanceDevice AttendanceDevice_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."AttendanceDevice"
    ADD CONSTRAINT "AttendanceDevice_pkey" PRIMARY KEY (id);


--
-- Name: AttendancePunch AttendancePunch_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."AttendancePunch"
    ADD CONSTRAINT "AttendancePunch_pkey" PRIMARY KEY (id);


--
-- Name: AuditLog AuditLog_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."AuditLog"
    ADD CONSTRAINT "AuditLog_pkey" PRIMARY KEY (id);


--
-- Name: BookingSegmentNightlyRate BookingSegmentNightlyRate_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."BookingSegmentNightlyRate"
    ADD CONSTRAINT "BookingSegmentNightlyRate_pkey" PRIMARY KEY (id);


--
-- Name: BookingSegmentNightlyRate BookingSegmentNightlyRate_segment_date_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."BookingSegmentNightlyRate"
    ADD CONSTRAINT "BookingSegmentNightlyRate_segment_date_key" UNIQUE ("segmentId", date);


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
-- Name: EmployeePattern EmployeePattern_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."EmployeePattern"
    ADD CONSTRAINT "EmployeePattern_pkey" PRIMARY KEY ("employeeUserId");


--
-- Name: EmployeePayRate EmployeePayRate_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."EmployeePayRate"
    ADD CONSTRAINT "EmployeePayRate_pkey" PRIMARY KEY (id);


--
-- Name: FolioPayment FolioPayment_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."FolioPayment"
    ADD CONSTRAINT "FolioPayment_pkey" PRIMARY KEY (id);


--
-- Name: GuestAccount GuestAccount_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."GuestAccount"
    ADD CONSTRAINT "GuestAccount_pkey" PRIMARY KEY (id);


--
-- Name: Guest Guest_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."Guest"
    ADD CONSTRAINT "Guest_pkey" PRIMARY KEY (id);


--
-- Name: MaintenanceTask MaintenanceTask_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."MaintenanceTask"
    ADD CONSTRAINT "MaintenanceTask_pkey" PRIMARY KEY (id);


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
-- Name: PropertyMap PropertyMap_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."PropertyMap"
    ADD CONSTRAINT "PropertyMap_pkey" PRIMARY KEY (id);


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
-- Name: RosterEntry RosterEntry_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."RosterEntry"
    ADD CONSTRAINT "RosterEntry_pkey" PRIMARY KEY (id);


--
-- Name: RosterImportNameMapping RosterImportNameMapping_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."RosterImportNameMapping"
    ADD CONSTRAINT "RosterImportNameMapping_pkey" PRIMARY KEY (id);


--
-- Name: RosterImportShiftColorMapping RosterImportShiftColorMapping_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."RosterImportShiftColorMapping"
    ADD CONSTRAINT "RosterImportShiftColorMapping_pkey" PRIMARY KEY (id);


--
-- Name: ShiftCode ShiftCode_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."ShiftCode"
    ADD CONSTRAINT "ShiftCode_pkey" PRIMARY KEY (id);


--
-- Name: Shift Shift_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."Shift"
    ADD CONSTRAINT "Shift_pkey" PRIMARY KEY (id);


--
-- Name: SpaAppointmentTreatment SpaAppointmentTreatment_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."SpaAppointmentTreatment"
    ADD CONSTRAINT "SpaAppointmentTreatment_pkey" PRIMARY KEY (id);


--
-- Name: SpaAppointment SpaAppointment_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."SpaAppointment"
    ADD CONSTRAINT "SpaAppointment_pkey" PRIMARY KEY (id);


--
-- Name: SpaMap SpaMap_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."SpaMap"
    ADD CONSTRAINT "SpaMap_pkey" PRIMARY KEY (id);


--
-- Name: StaffAreaCoverageRule StaffAreaCoverageRule_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."StaffAreaCoverageRule"
    ADD CONSTRAINT "StaffAreaCoverageRule_pkey" PRIMARY KEY ("staffArea");


--
-- Name: User User_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."User"
    ADD CONSTRAINT "User_pkey" PRIMARY KEY (id);


--
-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);


--
-- Name: SpaAppointment spa_appointment_no_table_overlap; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."SpaAppointment"
    ADD CONSTRAINT spa_appointment_no_table_overlap EXCLUDE USING gist ("tableId" WITH =, tsrange((date + "startTime"), ((date + "startTime") + (("durationMinutes")::double precision * '00:01:00'::interval))) WITH &&) WHERE ((status = ANY (ARRAY['BOOKED'::public."SpaAppointmentStatus", 'COMPLETED'::public."SpaAppointmentStatus"]))) DEFERRABLE;


--
-- Name: SpaAppointment spa_appointment_no_therapist_overlap; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."SpaAppointment"
    ADD CONSTRAINT spa_appointment_no_therapist_overlap EXCLUDE USING gist ("therapistUserId" WITH =, tsrange((date + "startTime"), ((date + "startTime") + (("durationMinutes")::double precision * '00:01:00'::interval))) WITH &&) WHERE ((status = ANY (ARRAY['BOOKED'::public."SpaAppointmentStatus", 'COMPLETED'::public."SpaAppointmentStatus"]))) DEFERRABLE;


--
-- Name: AttendanceDevice_serial_key; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX "AttendanceDevice_serial_key" ON public."AttendanceDevice" USING btree (serial);


--
-- Name: AttendancePunch_device_enrollment_punchAt_key; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX "AttendancePunch_device_enrollment_punchAt_key" ON public."AttendancePunch" USING btree ("deviceId", "enrollmentNumber", "punchAt");


--
-- Name: AttendancePunch_employeeUserId_punchAt_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "AttendancePunch_employeeUserId_punchAt_idx" ON public."AttendancePunch" USING btree ("employeeUserId", "punchAt");


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
-- Name: BookingSegmentNightlyRate_segmentId_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "BookingSegmentNightlyRate_segmentId_idx" ON public."BookingSegmentNightlyRate" USING btree ("segmentId");


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
-- Name: Booking_guestId_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "Booking_guestId_idx" ON public."Booking" USING btree ("guestId");


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
-- Name: EmployeePayRate_employeeUserId_effectiveFrom_key; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX "EmployeePayRate_employeeUserId_effectiveFrom_key" ON public."EmployeePayRate" USING btree ("employeeUserId", "effectiveFrom");


--
-- Name: FolioPayment_bookingId_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "FolioPayment_bookingId_idx" ON public."FolioPayment" USING btree ("bookingId");


--
-- Name: GuestAccount_email_key; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX "GuestAccount_email_key" ON public."GuestAccount" USING btree (email);


--
-- Name: MaintenanceTask_blockId_key; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX "MaintenanceTask_blockId_key" ON public."MaintenanceTask" USING btree ("blockId");


--
-- Name: MaintenanceTask_roomUnitId_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "MaintenanceTask_roomUnitId_idx" ON public."MaintenanceTask" USING btree ("roomUnitId");


--
-- Name: MaintenanceTask_status_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "MaintenanceTask_status_idx" ON public."MaintenanceTask" USING btree (status);


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
-- Name: Order_guestAccessToken_key; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX "Order_guestAccessToken_key" ON public."Order" USING btree ("guestAccessToken");


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
-- Name: RosterEntry_date_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "RosterEntry_date_idx" ON public."RosterEntry" USING btree (date);


--
-- Name: RosterEntry_employeeUserId_date_key; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX "RosterEntry_employeeUserId_date_key" ON public."RosterEntry" USING btree ("employeeUserId", date);


--
-- Name: RosterImportNameMapping_rawName_key; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX "RosterImportNameMapping_rawName_key" ON public."RosterImportNameMapping" USING btree ("rawName");


--
-- Name: RosterImportShiftColorMapping_code_fill_key; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX "RosterImportShiftColorMapping_code_fill_key" ON public."RosterImportShiftColorMapping" USING btree ("rawCode", "fillColor");


--
-- Name: ShiftCode_shared_code_effectiveFrom_key; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX "ShiftCode_shared_code_effectiveFrom_key" ON public."ShiftCode" USING btree (code, "effectiveFrom") WHERE ("staffArea" IS NULL);


--
-- Name: ShiftCode_staffArea_code_effectiveFrom_key; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX "ShiftCode_staffArea_code_effectiveFrom_key" ON public."ShiftCode" USING btree ("staffArea", code, "effectiveFrom");


--
-- Name: ShiftCode_staffArea_code_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "ShiftCode_staffArea_code_idx" ON public."ShiftCode" USING btree ("staffArea", code);


--
-- Name: Shift_one_open_per_user; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX "Shift_one_open_per_user" ON public."Shift" USING btree ("openedByUserId") WHERE (status = 'OPEN'::public."ShiftStatus");


--
-- Name: SpaAppointmentTreatment_spaAppointmentId_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "SpaAppointmentTreatment_spaAppointmentId_idx" ON public."SpaAppointmentTreatment" USING btree ("spaAppointmentId");


--
-- Name: SpaAppointment_bookingId_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "SpaAppointment_bookingId_idx" ON public."SpaAppointment" USING btree ("bookingId");


--
-- Name: SpaAppointment_date_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "SpaAppointment_date_idx" ON public."SpaAppointment" USING btree (date);


--
-- Name: SpaAppointment_therapistUserId_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "SpaAppointment_therapistUserId_idx" ON public."SpaAppointment" USING btree ("therapistUserId");


--
-- Name: User_email_key; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX "User_email_key" ON public."User" USING btree (email);


--
-- Name: User_enrollmentNumber_key; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX "User_enrollmentNumber_key" ON public."User" USING btree ("enrollmentNumber");


--
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);


--
-- Name: AttendancePunch AttendancePunch_deviceId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."AttendancePunch"
    ADD CONSTRAINT "AttendancePunch_deviceId_fkey" FOREIGN KEY ("deviceId") REFERENCES public."AttendanceDevice"(id);


--
-- Name: AttendancePunch AttendancePunch_employeeUserId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."AttendancePunch"
    ADD CONSTRAINT "AttendancePunch_employeeUserId_fkey" FOREIGN KEY ("employeeUserId") REFERENCES public."User"(id);


--
-- Name: AttendancePunch AttendancePunch_recordedByUserId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."AttendancePunch"
    ADD CONSTRAINT "AttendancePunch_recordedByUserId_fkey" FOREIGN KEY ("recordedByUserId") REFERENCES public."User"(id);


--
-- Name: BookingSegmentNightlyRate BookingSegmentNightlyRate_segmentId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."BookingSegmentNightlyRate"
    ADD CONSTRAINT "BookingSegmentNightlyRate_segmentId_fkey" FOREIGN KEY ("segmentId") REFERENCES public."BookingSegment"(id) ON DELETE CASCADE;


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
-- Name: Booking Booking_guestId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."Booking"
    ADD CONSTRAINT "Booking_guestId_fkey" FOREIGN KEY ("guestId") REFERENCES public."Guest"(id);


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
-- Name: EmployeePattern EmployeePattern_defaultShiftCodeId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."EmployeePattern"
    ADD CONSTRAINT "EmployeePattern_defaultShiftCodeId_fkey" FOREIGN KEY ("defaultShiftCodeId") REFERENCES public."ShiftCode"(id);


--
-- Name: EmployeePattern EmployeePattern_employeeUserId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."EmployeePattern"
    ADD CONSTRAINT "EmployeePattern_employeeUserId_fkey" FOREIGN KEY ("employeeUserId") REFERENCES public."User"(id);


--
-- Name: EmployeePattern EmployeePattern_updatedByUserId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."EmployeePattern"
    ADD CONSTRAINT "EmployeePattern_updatedByUserId_fkey" FOREIGN KEY ("updatedByUserId") REFERENCES public."User"(id);


--
-- Name: EmployeePayRate EmployeePayRate_createdByUserId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."EmployeePayRate"
    ADD CONSTRAINT "EmployeePayRate_createdByUserId_fkey" FOREIGN KEY ("createdByUserId") REFERENCES public."User"(id);


--
-- Name: EmployeePayRate EmployeePayRate_employeeUserId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."EmployeePayRate"
    ADD CONSTRAINT "EmployeePayRate_employeeUserId_fkey" FOREIGN KEY ("employeeUserId") REFERENCES public."User"(id);


--
-- Name: FolioPayment FolioPayment_bookingId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."FolioPayment"
    ADD CONSTRAINT "FolioPayment_bookingId_fkey" FOREIGN KEY ("bookingId") REFERENCES public."Booking"(id);


--
-- Name: FolioPayment FolioPayment_recordedByUserId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."FolioPayment"
    ADD CONSTRAINT "FolioPayment_recordedByUserId_fkey" FOREIGN KEY ("recordedByUserId") REFERENCES public."User"(id);


--
-- Name: MaintenanceTask MaintenanceTask_blockId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."MaintenanceTask"
    ADD CONSTRAINT "MaintenanceTask_blockId_fkey" FOREIGN KEY ("blockId") REFERENCES public."RoomUnitBlock"(id) ON DELETE SET NULL;


--
-- Name: MaintenanceTask MaintenanceTask_reportedByUserId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."MaintenanceTask"
    ADD CONSTRAINT "MaintenanceTask_reportedByUserId_fkey" FOREIGN KEY ("reportedByUserId") REFERENCES public."User"(id);


--
-- Name: MaintenanceTask MaintenanceTask_roomUnitId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."MaintenanceTask"
    ADD CONSTRAINT "MaintenanceTask_roomUnitId_fkey" FOREIGN KEY ("roomUnitId") REFERENCES public."RoomUnit"(id);


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
-- Name: PrintJob PrintJob_dismissedByUserId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."PrintJob"
    ADD CONSTRAINT "PrintJob_dismissedByUserId_fkey" FOREIGN KEY ("dismissedByUserId") REFERENCES public."User"(id);


--
-- Name: PrintJob PrintJob_printerId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."PrintJob"
    ADD CONSTRAINT "PrintJob_printerId_fkey" FOREIGN KEY ("printerId") REFERENCES public."Printer"(id);


--
-- Name: PropertyMap PropertyMap_updatedByUserId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."PropertyMap"
    ADD CONSTRAINT "PropertyMap_updatedByUserId_fkey" FOREIGN KEY ("updatedByUserId") REFERENCES public."User"(id);


--
-- Name: RatePlan RatePlan_roomId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."RatePlan"
    ADD CONSTRAINT "RatePlan_roomId_fkey" FOREIGN KEY ("roomId") REFERENCES public."Room"(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: RoomUnitBlock RoomUnitBlock_createdByUserId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."RoomUnitBlock"
    ADD CONSTRAINT "RoomUnitBlock_createdByUserId_fkey" FOREIGN KEY ("createdByUserId") REFERENCES public."User"(id);


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
-- Name: RosterEntry RosterEntry_createdByUserId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."RosterEntry"
    ADD CONSTRAINT "RosterEntry_createdByUserId_fkey" FOREIGN KEY ("createdByUserId") REFERENCES public."User"(id);


--
-- Name: RosterEntry RosterEntry_employeeUserId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."RosterEntry"
    ADD CONSTRAINT "RosterEntry_employeeUserId_fkey" FOREIGN KEY ("employeeUserId") REFERENCES public."User"(id);


--
-- Name: RosterEntry RosterEntry_shiftCodeId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."RosterEntry"
    ADD CONSTRAINT "RosterEntry_shiftCodeId_fkey" FOREIGN KEY ("shiftCodeId") REFERENCES public."ShiftCode"(id);


--
-- Name: RosterImportNameMapping RosterImportNameMapping_createdByUserId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."RosterImportNameMapping"
    ADD CONSTRAINT "RosterImportNameMapping_createdByUserId_fkey" FOREIGN KEY ("createdByUserId") REFERENCES public."User"(id);


--
-- Name: RosterImportNameMapping RosterImportNameMapping_employeeUserId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."RosterImportNameMapping"
    ADD CONSTRAINT "RosterImportNameMapping_employeeUserId_fkey" FOREIGN KEY ("employeeUserId") REFERENCES public."User"(id);


--
-- Name: RosterImportShiftColorMapping RosterImportShiftColorMapping_createdByUserId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."RosterImportShiftColorMapping"
    ADD CONSTRAINT "RosterImportShiftColorMapping_createdByUserId_fkey" FOREIGN KEY ("createdByUserId") REFERENCES public."User"(id);


--
-- Name: ShiftCode ShiftCode_createdByUserId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."ShiftCode"
    ADD CONSTRAINT "ShiftCode_createdByUserId_fkey" FOREIGN KEY ("createdByUserId") REFERENCES public."User"(id);


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
-- Name: SpaAppointmentTreatment SpaAppointmentTreatment_spaAppointmentId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."SpaAppointmentTreatment"
    ADD CONSTRAINT "SpaAppointmentTreatment_spaAppointmentId_fkey" FOREIGN KEY ("spaAppointmentId") REFERENCES public."SpaAppointment"(id);


--
-- Name: SpaAppointmentTreatment SpaAppointmentTreatment_treatmentMenuItemId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."SpaAppointmentTreatment"
    ADD CONSTRAINT "SpaAppointmentTreatment_treatmentMenuItemId_fkey" FOREIGN KEY ("treatmentMenuItemId") REFERENCES public."MenuItem"(id);


--
-- Name: SpaAppointment SpaAppointment_bookingId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."SpaAppointment"
    ADD CONSTRAINT "SpaAppointment_bookingId_fkey" FOREIGN KEY ("bookingId") REFERENCES public."Booking"(id);


--
-- Name: SpaAppointment SpaAppointment_cancelledByUserId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."SpaAppointment"
    ADD CONSTRAINT "SpaAppointment_cancelledByUserId_fkey" FOREIGN KEY ("cancelledByUserId") REFERENCES public."User"(id);


--
-- Name: SpaAppointment SpaAppointment_createdByUserId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."SpaAppointment"
    ADD CONSTRAINT "SpaAppointment_createdByUserId_fkey" FOREIGN KEY ("createdByUserId") REFERENCES public."User"(id);


--
-- Name: SpaAppointment SpaAppointment_orderId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."SpaAppointment"
    ADD CONSTRAINT "SpaAppointment_orderId_fkey" FOREIGN KEY ("orderId") REFERENCES public."Order"(id);


--
-- Name: SpaAppointment SpaAppointment_tableId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."SpaAppointment"
    ADD CONSTRAINT "SpaAppointment_tableId_fkey" FOREIGN KEY ("tableId") REFERENCES public."PosTable"(id);


--
-- Name: SpaAppointment SpaAppointment_therapistUserId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."SpaAppointment"
    ADD CONSTRAINT "SpaAppointment_therapistUserId_fkey" FOREIGN KEY ("therapistUserId") REFERENCES public."User"(id);


--
-- Name: SpaMap SpaMap_updatedByUserId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."SpaMap"
    ADD CONSTRAINT "SpaMap_updatedByUserId_fkey" FOREIGN KEY ("updatedByUserId") REFERENCES public."User"(id);


--
-- Name: StaffAreaCoverageRule StaffAreaCoverageRule_updatedByUserId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."StaffAreaCoverageRule"
    ADD CONSTRAINT "StaffAreaCoverageRule_updatedByUserId_fkey" FOREIGN KEY ("updatedByUserId") REFERENCES public."User"(id);


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

INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (1, '1', 'baseline', 'SQL', 'V1__baseline.sql', 925507334, 'sunsetbeach', '2026-09-22 19:14:36.530829', 7, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (2, '1.1', 'prisma baseline schema', 'SQL', 'V1_1__prisma_baseline_schema.sql', 1682395768, 'sunsetbeach', '2026-09-22 19:14:36.567549', 133, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (3, '2', 'add pos roles', 'SQL', 'V2__add_pos_roles.sql', -289401340, 'sunsetbeach', '2026-09-22 19:14:36.728108', 7, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (4, '3', 'pos module', 'SQL', 'V3__pos_module.sql', -402720331, 'sunsetbeach', '2026-09-22 19:14:36.754451', 170, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (5, '4', 'room quantity and availability capacity', 'SQL', 'V4__room_quantity_and_availability_capacity.sql', -1793132906, 'sunsetbeach', '2026-09-22 19:14:36.950124', 54, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (6, '5', 'printers and print jobs', 'SQL', 'V5__printers_and_print_jobs.sql', -1063600715, 'sunsetbeach', '2026-09-22 19:14:37.134503', 77, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (7, '6', 'add bar ticket document type', 'SQL', 'V6__add_bar_ticket_document_type.sql', 372073248, 'sunsetbeach', '2026-09-22 19:14:37.247388', 7, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (8, '7', 'backfill bar ticket document type', 'SQL', 'V7__backfill_bar_ticket_document_type.sql', 603488721, 'sunsetbeach', '2026-09-22 19:14:37.284927', 8, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (9, '8', 'physical room units', 'SQL', 'V8__physical_room_units.sql', 379262078, 'sunsetbeach', '2026-09-22 19:14:37.310318', 57, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (10, '9', 'backfill room units from quantity', 'SQL', 'V9__backfill_room_units_from_quantity.sql', -1789704355, 'sunsetbeach', '2026-09-22 19:14:37.396985', 8, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (11, '10', 'migrate availability blocks to room units', 'SQL', 'V10__migrate_availability_blocks_to_room_units.sql', -1495654860, 'sunsetbeach', '2026-09-22 19:14:37.430969', 7, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (12, '11', 'booking room unit and cleanup', 'SQL', 'V11__booking_room_unit_and_cleanup.sql', 749934238, 'sunsetbeach', '2026-09-22 19:14:37.474756', 25, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (13, '12', 'booking source', 'SQL', 'V12__booking_source.sql', 1606721862, 'sunsetbeach', '2026-09-22 19:14:37.527579', 9, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (14, '13', 'user active and token version', 'SQL', 'V13__user_active_and_token_version.sql', -156468621, 'sunsetbeach', '2026-09-22 19:14:37.55397', 6, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (15, '14', 'unique payment per order', 'SQL', 'V14__unique_payment_per_order.sql', 564520482, 'sunsetbeach', '2026-09-22 19:14:37.57799', 11, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (16, '15', 'booking expiry reminder', 'SQL', 'V15__booking_expiry_reminder.sql', -1589014794, 'sunsetbeach', '2026-09-22 19:14:37.606162', 7, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (17, '16', 'audit log', 'SQL', 'V16__audit_log.sql', 61930974, 'sunsetbeach', '2026-09-22 19:14:37.63234', 53, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (18, '17', 'nullable walkin guest contact', 'SQL', 'V17__nullable_walkin_guest_contact.sql', -1902839019, 'sunsetbeach', '2026-09-22 19:14:37.704589', 4, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (19, '18', 'booking segments', 'SQL', 'V18__booking_segments.sql', 953234474, 'sunsetbeach', '2026-09-22 19:14:37.72395', 43, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (20, '19', 'booking relocation audit actions', 'SQL', 'V19__booking_relocation_audit_actions.sql', 1588711728, 'sunsetbeach', '2026-09-22 19:14:37.785998', 6, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (21, '20', 'order item sent at', 'SQL', 'V20__order_item_sent_at.sql', 1763124512, 'sunsetbeach', '2026-09-22 19:14:37.808448', 3, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (22, '21', 'booking segment nightly rates', 'SQL', 'V21__booking_segment_nightly_rates.sql', -650249535, 'sunsetbeach', '2026-09-22 19:14:37.826794', 32, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (23, '22', 'booking repriced audit action', 'SQL', 'V22__booking_repriced_audit_action.sql', -2050445357, 'sunsetbeach', '2026-09-22 19:14:37.8763', 4, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (24, '23', 'booking occupancy', 'SQL', 'V23__booking_occupancy.sql', -1959641827, 'sunsetbeach', '2026-09-22 19:14:37.901984', 8, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (25, '24', 'room unit housekeeping status', 'SQL', 'V24__room_unit_housekeeping_status.sql', 1534230026, 'sunsetbeach', '2026-09-22 19:14:37.949937', 4, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (26, '25', 'occupancy and housekeeping audit actions', 'SQL', 'V25__occupancy_and_housekeeping_audit_actions.sql', -905251936, 'sunsetbeach', '2026-09-22 19:14:37.973813', 13, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (27, '26', 'booking folio payments', 'SQL', 'V26__booking_folio_payments.sql', 764998775, 'sunsetbeach', '2026-09-22 19:14:38.006436', 26, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (28, '27', 'folio payment audit action', 'SQL', 'V27__folio_payment_audit_action.sql', -1211553845, 'sunsetbeach', '2026-09-22 19:14:38.051662', 3, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (29, '28', 'room unit position', 'SQL', 'V28__room_unit_position.sql', -975424585, 'sunsetbeach', '2026-09-22 19:14:38.091289', 10, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (30, '29', 'property map', 'SQL', 'V29__property_map.sql', 204069288, 'sunsetbeach', '2026-09-22 19:14:38.116761', 17, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (31, '30', 'property map audit actions', 'SQL', 'V30__property_map_audit_actions.sql', 1978002116, 'sunsetbeach', '2026-09-22 19:14:38.147227', 4, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (32, '31', 'print job dismiss', 'SQL', 'V31__print_job_dismiss.sql', -1672853618, 'sunsetbeach', '2026-09-22 19:14:38.165861', 6, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (33, '32', 'print job dismissed audit action', 'SQL', 'V32__print_job_dismissed_audit_action.sql', 1791858240, 'sunsetbeach', '2026-09-22 19:14:38.186296', 6, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (34, '33', 'user job functions', 'SQL', 'V33__user_job_functions.sql', -695229030, 'sunsetbeach', '2026-09-22 19:14:38.206688', 5, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (35, '34', 'user functions changed audit action', 'SQL', 'V34__user_functions_changed_audit_action.sql', -1070375068, 'sunsetbeach', '2026-09-22 19:14:38.227106', 3, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (36, '35', 'maintenance tasks', 'SQL', 'V35__maintenance_tasks.sql', 1130672293, 'sunsetbeach', '2026-09-22 19:14:38.243908', 42, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (37, '36', 'maintenance task audit actions', 'SQL', 'V36__maintenance_task_audit_actions.sql', -764126344, 'sunsetbeach', '2026-09-22 19:14:38.300643', 10, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (38, '37', 'job function therapist', 'SQL', 'V37__job_function_therapist.sql', -805795074, 'sunsetbeach', '2026-09-22 19:14:38.330292', 7, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (39, '38', 'menu department spa', 'SQL', 'V38__menu_department_spa.sql', 1794174787, 'sunsetbeach', '2026-09-22 19:14:38.356854', 5, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (40, '39', 'menu item duration', 'SQL', 'V39__menu_item_duration.sql', 18789482, 'sunsetbeach', '2026-09-22 19:14:38.378527', 6, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (41, '40', 'pos table position', 'SQL', 'V40__pos_table_position.sql', 164979000, 'sunsetbeach', '2026-09-22 19:14:38.406853', 9, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (42, '41', 'spa appointment', 'SQL', 'V41__spa_appointment.sql', 1481976566, 'sunsetbeach', '2026-09-22 19:14:38.435863', 102, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (43, '42', 'spa and table audit actions', 'SQL', 'V42__spa_and_table_audit_actions.sql', -1692965902, 'sunsetbeach', '2026-09-22 19:14:38.568919', 8, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (44, '43', 'spa appointment completed still occupies slot', 'SQL', 'V43__spa_appointment_completed_still_occupies_slot.sql', 1983467581, 'sunsetbeach', '2026-09-22 19:14:38.59734', 10, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (45, '44', 'guest', 'SQL', 'V44__guest.sql', 520726807, 'sunsetbeach', '2026-09-22 19:14:38.631143', 20, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (46, '45', 'booking guest link', 'SQL', 'V45__booking_guest_link.sql', 1847285923, 'sunsetbeach', '2026-09-22 19:14:38.676646', 11, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (47, '46', 'guest audit actions', 'SQL', 'V46__guest_audit_actions.sql', -1106058738, 'sunsetbeach', '2026-09-22 19:14:38.710657', 11, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (48, '47', 'spa map', 'SQL', 'V47__spa_map.sql', 103633260, 'sunsetbeach', '2026-09-22 19:14:38.741135', 26, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (49, '48', 'spa map audit action', 'SQL', 'V48__spa_map_audit_action.sql', 1987277571, 'sunsetbeach', '2026-09-22 19:14:38.785754', 6, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (50, '49', 'segment room unit and spa reschedule audit actions', 'SQL', 'V49__segment_room_unit_and_spa_reschedule_audit_actions.sql', 1860920270, 'sunsetbeach', '2026-09-22 19:14:38.811616', 7, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (51, '50', 'spa appointment treatment', 'SQL', 'V50__spa_appointment_treatment.sql', 15551835, 'sunsetbeach', '2026-09-22 19:14:38.835784', 26, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (52, '51', 'spa appointment treatment backfill', 'SQL', 'V51__spa_appointment_treatment_backfill.sql', -1190020797, 'sunsetbeach', '2026-09-22 19:14:38.879746', 4, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (53, '52', 'spa appointment treatment audit actions', 'SQL', 'V52__spa_appointment_treatment_audit_actions.sql', -1381702356, 'sunsetbeach', '2026-09-22 19:14:38.903463', 8, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (54, '53', 'spa appointment drop treatment column', 'SQL', 'V53__spa_appointment_drop_treatment_column.sql', -656555796, 'sunsetbeach', '2026-09-22 19:14:38.930953', 15, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (55, '54', 'room unit block created by', 'SQL', 'V54__room_unit_block_created_by.sql', 896545354, 'sunsetbeach', '2026-09-22 19:14:38.963669', 5, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (56, '55', 'spa appointment tables swapped audit action', 'SQL', 'V55__spa_appointment_tables_swapped_audit_action.sql', 2120343003, 'sunsetbeach', '2026-09-22 19:14:38.982107', 4, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (57, '56', 'spa appointment deferrable overlap constraints', 'SQL', 'V56__spa_appointment_deferrable_overlap_constraints.sql', 1945775173, 'sunsetbeach', '2026-09-22 19:14:39.000712', 9, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (58, '57', 'staff area and shift code', 'SQL', 'V57__staff_area_and_shift_code.sql', -20296544, 'sunsetbeach', '2026-09-22 19:14:39.027053', 33, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (59, '58', 'weekday and employee pattern', 'SQL', 'V58__weekday_and_employee_pattern.sql', -1704913361, 'sunsetbeach', '2026-09-22 19:14:39.075558', 20, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (60, '59', 'shift code pattern audit actions', 'SQL', 'V59__shift_code_pattern_audit_actions.sql', -1808968993, 'sunsetbeach', '2026-09-22 19:14:39.113232', 4, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (61, '60', 'roster entry', 'SQL', 'V60__roster_entry.sql', -1436131160, 'sunsetbeach', '2026-09-22 19:14:39.131215', 31, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (62, '61', 'roster entry audit actions', 'SQL', 'V61__roster_entry_audit_actions.sql', -1062274017, 'sunsetbeach', '2026-09-22 19:14:39.17983', 16, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (63, '62', 'staff area coverage rule', 'SQL', 'V62__staff_area_coverage_rule.sql', 1130367395, 'sunsetbeach', '2026-09-22 19:14:39.215143', 18, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (64, '63', 'coverage rule audit actions', 'SQL', 'V63__coverage_rule_audit_actions.sql', 1539891248, 'sunsetbeach', '2026-09-22 19:14:39.250548', 4, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (65, '64', 'punch enums and attendance punch', 'SQL', 'V64__punch_enums_and_attendance_punch.sql', 104636352, 'sunsetbeach', '2026-09-22 19:14:39.271465', 29, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (66, '65', 'attendance audit actions', 'SQL', 'V65__attendance_audit_actions.sql', 1116804630, 'sunsetbeach', '2026-09-22 19:14:39.318858', 5, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (67, '66', 'employee pay rate', 'SQL', 'V66__employee_pay_rate.sql', -556832852, 'sunsetbeach', '2026-09-22 19:14:39.340347', 26, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (68, '67', 'export and pay rate audit actions', 'SQL', 'V67__export_and_pay_rate_audit_actions.sql', -522770886, 'sunsetbeach', '2026-09-22 19:14:39.380076', 28, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (69, '68', 'user name and optional credentials', 'SQL', 'V68__user_name_and_optional_credentials.sql', -941849959, 'sunsetbeach', '2026-09-22 19:14:39.42908', 13, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (70, '69', 'shift code optional staff area', 'SQL', 'V69__shift_code_optional_staff_area.sql', -824954638, 'sunsetbeach', '2026-09-22 19:14:39.464818', 13, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (71, '70', 'user credentials granted audit action', 'SQL', 'V70__user_credentials_granted_audit_action.sql', -1575425831, 'sunsetbeach', '2026-09-22 19:14:39.49659', 5, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (72, '71', 'user overtime eligible', 'SQL', 'V71__user_overtime_eligible.sql', 111453362, 'sunsetbeach', '2026-09-22 19:14:39.518018', 4, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (73, '72', 'user overtime eligibility changed audit action', 'SQL', 'V72__user_overtime_eligibility_changed_audit_action.sql', 550901561, 'sunsetbeach', '2026-09-22 19:14:39.538636', 4, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (74, '73', 'user enrollment number', 'SQL', 'V73__user_enrollment_number.sql', 67486180, 'sunsetbeach', '2026-09-22 19:14:39.559497', 13, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (75, '74', 'attendance device', 'SQL', 'V74__attendance_device.sql', -293362376, 'sunsetbeach', '2026-09-22 19:14:39.588438', 22, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (76, '75', 'attendance punch device columns', 'SQL', 'V75__attendance_punch_device_columns.sql', -492159247, 'sunsetbeach', '2026-09-22 19:14:39.624545', 15, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (77, '76', 'user enrollment number changed audit action', 'SQL', 'V76__user_enrollment_number_changed_audit_action.sql', 347725818, 'sunsetbeach', '2026-09-22 19:14:39.653193', 4, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (78, '77', 'attendance device record size', 'SQL', 'V77__attendance_device_record_size.sql', -131134641, 'sunsetbeach', '2026-09-22 19:14:39.671229', 5, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (79, '78', 'roster import mappings', 'SQL', 'V78__roster_import_mappings.sql', -562408015, 'sunsetbeach', '2026-09-22 19:14:39.693216', 49, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (80, '79', 'roster month imported audit action', 'SQL', 'V79__roster_month_imported_audit_action.sql', -340477066, 'sunsetbeach', '2026-09-22 19:14:39.759672', 9, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (81, '80', 'attendance device windowed read unsupported', 'SQL', 'V80__attendance_device_windowed_read_unsupported.sql', -831228527, 'sunsetbeach', '2026-09-22 19:14:39.782588', 3, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (82, '81', 'roster import color mapping global', 'SQL', 'V81__roster_import_color_mapping_global.sql', -820233814, 'sunsetbeach', '2026-09-22 19:14:39.79972', 21, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (83, '82', 'user staff area changed audit action', 'SQL', 'V82__user_staff_area_changed_audit_action.sql', -279979318, 'sunsetbeach', '2026-09-22 19:14:39.849243', 4, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (84, '83', 'user staff area', 'SQL', 'V83__user_staff_area.sql', -1546498180, 'sunsetbeach', '2026-09-22 19:14:39.87661', 17, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (85, '84', 'shift code kind changed audit action', 'SQL', 'V84__shift_code_kind_changed_audit_action.sql', 891067422, 'sunsetbeach', '2026-09-22 19:14:39.924765', 8, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (86, '85', 'shift code kind', 'SQL', 'V85__shift_code_kind.sql', -1935981334, 'sunsetbeach', '2026-09-22 19:14:39.963542', 13, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (87, '86', 'shift code display color and roster grid exported audit actions', 'SQL', 'V86__shift_code_display_color_and_roster_grid_exported_audit_actions.sql', -846453451, 'sunsetbeach', '2026-09-22 19:14:39.996062', 6, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (88, '87', 'shift code display color', 'SQL', 'V87__shift_code_display_color.sql', 568101412, 'sunsetbeach', '2026-09-22 19:14:40.021493', 5, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (89, '88', 'order guest access token', 'SQL', 'V88__order_guest_access_token.sql', 1694517085, 'sunsetbeach', '2026-09-22 19:14:40.043238', 13, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (90, '89', 'guest account', 'SQL', 'V89__guest_account.sql', 733205345, 'sunsetbeach', '2026-09-22 19:14:40.073821', 38, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (91, '90', 'order opened by user id nullable', 'SQL', 'V90__order_opened_by_user_id_nullable.sql', -58889771, 'sunsetbeach', '2026-09-22 19:14:40.131786', 5, true);


--
-- PostgreSQL database dump complete
--


