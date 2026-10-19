package com.sunsetbeach.notification;

import com.sunsetbeach.model.NotificationLanguage;
import java.util.EnumMap;
import java.util.Map;

/**
 * Every text the notification centre writes, by key, in all three languages. Texts live in code, not
 * in the database: a wording change is a reviewed commit, and a stored notification keeps the words it
 * was sent with. A text holds {@code {placeholders}} filled from the params; a message carries a
 * person's name, a room label, a hotel time and a link - never money or guest data.
 *
 * <p><b>The Thai texts are a first translation and need a native speaker's review.</b>
 */
public final class NotificationMessages {

    public enum Key {
        LATE_SELF,
        LATE_STAFF,
        ABSENT,
        ARRIVED_AFTER_ABSENT,
        BLOCK_CREATED,
        BLOCK_LAST_DAY,
        BLOCK_EXPIRED_OPEN,
        TERMINAL_OFFLINE,
        TERMINAL_ONLINE,
        /** The bot's own replies (not notifications). */
        BOT_LINKED,
        BOT_LINK_INVALID,
        BOT_CHAT_TAKEN,
        BOT_UNLINKED,
        BOT_NOT_LINKED,
        BOT_HELP,
        /** Shift requests and the evening/roster notifications (texts in {@link StaffBotTexts}). */
        SHIFT_REQUEST_NEW, SHIFT_REQUEST_DECIDED_APPROVED, SHIFT_REQUEST_DECIDED_REJECTED, SHIFT_REQUEST_EXPIRED,
        SHIFT_REQUEST_REMINDER, SHIFT_EVE_REMINDER, ROSTER_CHANGED,
        /** Fragments a notification is built from. */
        FRAG_REASON, FRAG_COMMENT, FRAG_COVERAGE_BELOW, FRAG_COVERAGE_OK, FRAG_DAY_OFF, FRAG_WHAT_MOVE, FRAG_WHAT_DELETE,
        /** What a decided or expired request's message says after its buttons are gone. */
        OUTCOME_APPROVED, OUTCOME_REJECTED, OUTCOME_CANCELLED, OUTCOME_EXPIRED,
        EXP_SHIFT_STARTED, EXP_ENTRY_CHANGED, EXP_ENTRY_GONE, EXP_TARGET_TAKEN, EXP_EMPLOYEE_INACTIVE, EXP_LOCKED, EXP_APPLY_FAILED,
        /** The bot's menu, buttons and replies. */
        MENU_SHIFTS, MENU_DAY, MENU_MONTH, MENU_REQUESTS, MENU_SETTINGS, MENU_PENDING, MENU_VACATION,
        BOT_MENU, BOT_FEATURE_OFF, BOT_NOT_ALLOWED, BOT_BUTTON_EXPIRED, BOT_CANCELLED,
        BTN_APPROVE, BTN_REJECT, BTN_MOVE, BTN_DELETE, BTN_WITHDRAW, BTN_SKIP, BTN_CANCEL, BTN_EVE_ON, BTN_EVE_OFF, BTN_VAC_SET, BTN_VAC_CLEAR,
        SHIFTS_TITLE, SHIFTS_NONE, SHIFT_LINE, TAG_TODAY, TAG_TOMORROW, SHIFT_REQUEST_TAG,
        MOVE_PICK, MOVE_NO_DATES, ASK_REASON, ASK_REASON_OPTIONAL, REASON_TOO_LONG, REQUEST_SENT, REQUEST_WITHDRAWN,
        REJ_DISABLED, REJ_NOT_YOURS, REJ_LOCKED, REJ_TOO_LATE, REJ_TODAY, REJ_PAST, REJ_TARGET, REJ_TARGET_BUSY, REJ_PENDING,
        REJ_NOT_A_SHIFT, REJ_REASON_REQUIRED,
        DAY_TITLE, DAY_NO_SHIFT, DAY_SHIFT, DAY_STATE, DAY_PUNCHES, DAY_NO_PUNCHES, DAY_IN, DAY_OUT,
        STATE_NOT_YET_ARRIVED, STATE_ON_SHIFT, STATE_FINISHED, STATE_SCHEDULED, STATE_ARRIVING_SOON, STATE_LATE, STATE_BETWEEN_SHIFTS, STATE_MISSED,
        MONTH_SUMMARY,
        REQUESTS_TITLE, REQUESTS_NONE, REQUEST_LINE, STATUS_PENDING, STATUS_APPROVED, STATUS_REJECTED, STATUS_CANCELLED, STATUS_EXPIRED,
        SETTINGS_TEXT, WORD_ON, WORD_OFF, LANG_CHANGED, EVE_CHANGED,
        PENDING_TITLE, PENDING_NONE, PENDING_ITEM, TOAST_APPROVED, TOAST_REJECTED, TOAST_ALREADY,
        VAC_STATUS, VAC_NONE, VAC_ASK_FROM, VAC_ASK_TO, VAC_BAD_DATE, VAC_SAVED, VAC_CLEARED, VAC_NO_HR
    }

    private static final Map<Key, Map<NotificationLanguage, String>> TEXTS = new EnumMap<>(Key.class);

    static void add(Key key, String en, String th, String ru) {
        Map<NotificationLanguage, String> byLanguage = new EnumMap<>(NotificationLanguage.class);
        byLanguage.put(NotificationLanguage.EN, en);
        byLanguage.put(NotificationLanguage.TH, th);
        byLanguage.put(NotificationLanguage.RU, ru);
        TEXTS.put(key, byLanguage);
    }

    static {
        add(Key.LATE_SELF,
                "You are late: your shift started at {time}. Please clock in.\n{link}",
                "คุณมาสาย: กะของคุณเริ่มเวลา {time} กรุณาสแกนนิ้วเข้างาน\n{link}",
                "Вы опаздываете: смена началась в {time}. Отметьтесь на терминале.\n{link}");
        add(Key.LATE_STAFF,
                "{name} is late: the shift started at {time}, no clock-in yet.\n{link}",
                "{name} มาสาย: กะเริ่มเวลา {time} ยังไม่มีการสแกนเข้างาน\n{link}",
                "{name} опаздывает: смена началась в {time}, отметки пока нет.\n{link}");
        add(Key.ABSENT,
                "{name} has not shown up: the shift started at {time} and there is still no clock-in.\n{link}",
                "{name} ยังไม่มาทำงาน: กะเริ่มเวลา {time} และยังไม่มีการสแกนเข้างาน\n{link}",
                "{name}: нет отметки, смена началась в {time}, сотрудник так и не отметился.\n{link}");
        add(Key.ARRIVED_AFTER_ABSENT,
                "{name} has arrived at {time} ({minutes} min late).\n{link}",
                "{name} มาถึงเวลา {time} (สาย {minutes} นาที)\n{link}",
                "{name} пришёл в {time} (опоздание {minutes} мин).\n{link}");
        add(Key.BLOCK_CREATED,
                "Room {room} is blocked from {from} to {to}. Responsible: {function}.\n{link}",
                "ห้อง {room} ถูกปิดตั้งแต่ {from} ถึง {to} ผู้รับผิดชอบ: {function}\n{link}",
                "Комната {room} заблокирована с {from} по {to}. Ответственный: {function}.\n{link}");
        add(Key.BLOCK_LAST_DAY,
                "Room {room}: today is the last day of the block. Close the task or extend the block.\n{link}",
                "ห้อง {room}: วันนี้เป็นวันสุดท้ายของการปิดห้อง กรุณาปิดงานหรือขยายเวลา\n{link}",
                "Комната {room}: сегодня последний день блока. Закройте задачу или продлите блок.\n{link}");
        add(Key.BLOCK_EXPIRED_OPEN,
                "Room {room}: the block ended on {to} but the maintenance task is still not done. Close the task or extend the block.\n{link}",
                "ห้อง {room}: การปิดห้องสิ้นสุดเมื่อ {to} แต่งานซ่อมบำรุงยังไม่เสร็จ กรุณาปิดงานหรือขยายเวลา\n{link}",
                "Комната {room}: блок закончился {to}, но задача обслуживания всё ещё не закрыта. Закройте задачу или продлите блок.\n{link}");
        add(Key.TERMINAL_OFFLINE,
                "Attendance terminal \"{device}\" has not responded since {time}. Late and absence alerts are paused until it is back.\n{link}",
                "เครื่องสแกนนิ้ว \"{device}\" ไม่ตอบสนองตั้งแต่ {time} การแจ้งเตือนการมาสายและการขาดงานหยุดชั่วคราวจนกว่าจะกลับมาทำงาน\n{link}",
                "Терминал посещаемости «{device}» не отвечает с {time}. Уведомления об опозданиях и отсутствии приостановлены, пока он не вернётся.\n{link}");
        add(Key.TERMINAL_ONLINE,
                "Attendance terminal \"{device}\" is back online. Late and absence alerts are active again.\n{link}",
                "เครื่องสแกนนิ้ว \"{device}\" กลับมาออนไลน์แล้ว การแจ้งเตือนการมาสายและการขาดงานกลับมาทำงานตามปกติ\n{link}",
                "Терминал посещаемости «{device}» снова на связи. Уведомления об опозданиях и отсутствии снова работают.\n{link}");
        add(Key.BOT_LINKED,
                "Linked: {name}",
                "เชื่อมต่อแล้ว: {name}",
                "Привязано: {name}");
        add(Key.BOT_LINK_INVALID,
                "This link is invalid or has expired. Ask your manager for a new one.",
                "ลิงก์นี้ไม่ถูกต้องหรือหมดอายุแล้ว กรุณาขอลิงก์ใหม่จากผู้จัดการ",
                "Ссылка недействительна или устарела. Попросите у менеджера новую.");
        add(Key.BOT_CHAT_TAKEN,
                "This chat is already linked to another employee. Ask your manager to unlink it first.",
                "แชตนี้เชื่อมต่อกับพนักงานคนอื่นอยู่แล้ว กรุณาขอให้ผู้จัดการยกเลิกการเชื่อมต่อก่อน",
                "Этот чат уже привязан к другому сотруднику. Попросите менеджера сначала отвязать его.");
        add(Key.BOT_UNLINKED,
                "Unlinked. You will no longer get notifications here.",
                "ยกเลิกการเชื่อมต่อแล้ว คุณจะไม่ได้รับการแจ้งเตือนที่นี่อีก",
                "Отвязано. Уведомления сюда больше не придут.");
        add(Key.BOT_NOT_LINKED,
                "This chat is not linked to anyone. Open the link your manager gave you.",
                "แชตนี้ยังไม่ได้เชื่อมต่อกับใคร กรุณาเปิดลิงก์ที่ผู้จัดการให้",
                "Этот чат ни к кому не привязан. Откройте ссылку, которую дал менеджер.");
        add(Key.BOT_HELP,
                "To get notifications, open the link your manager gave you. Send /stop to stop them.",
                "หากต้องการรับการแจ้งเตือน กรุณาเปิดลิงก์ที่ผู้จัดการให้ ส่ง /stop เพื่อหยุดรับ",
                "Чтобы получать уведомления, откройте ссылку, которую дал менеджер. Команда /stop отключает их.");
        StaffBotTexts.register();
    }

    private NotificationMessages() {
    }

    /** Fills {@code {placeholders}} from {@code params} (a missing one becomes empty) and trims what is left. */
    public static String render(Key key, NotificationLanguage language, Map<String, String> params) {
        String text = TEXTS.get(key).get(language);
        for (Map.Entry<String, String> param : params.entrySet()) {
            text = text.replace("{" + param.getKey() + "}", param.getValue() == null ? "" : param.getValue());
        }
        return text.replaceAll("\\{[a-zA-Z]+}", "").strip();
    }

    /** The label of a button for an action kind ({@link NotificationAction.Kinds}) in the reader's language. */
    public static String actionLabel(String kind, NotificationLanguage language) {
        Key key = switch (kind) {
            case NotificationAction.Kinds.SHIFT_REQUEST_APPROVE -> Key.BTN_APPROVE;
            case NotificationAction.Kinds.SHIFT_REQUEST_REJECT -> Key.BTN_REJECT;
            default -> null;
        };
        return key == null ? kind : render(key, language, Map.of());
    }

    /** The name of a job function in the reader's language. */
    public static String functionName(String function, NotificationLanguage language) {
        if ("HR".equals(function)) {
            return switch (language) {
                case EN -> "HR director";
                case TH -> "ผู้อำนวยการฝ่ายบุคคล";
                case RU -> "HR-директор";
            };
        }
        boolean housekeeper = "HOUSEKEEPER".equals(function);
        return switch (language) {
            case EN -> housekeeper ? "Housekeeper" : "Engineer";
            case TH -> housekeeper ? "แม่บ้าน" : "ช่างซ่อมบำรุง";
            case RU -> housekeeper ? "горничная" : "инженер";
        };
    }
}
