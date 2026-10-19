package com.sunsetbeach.notification;

import static com.sunsetbeach.notification.NotificationMessages.Key.*;

import com.sunsetbeach.notification.NotificationMessages.Key;

/**
 * The staff bot's texts: the menu, its replies, buttons, and the notifications about shift requests and
 * schedule changes. Kept beside {@link NotificationMessages} (which owns the keys and rendering) only to
 * keep that class readable. Placeholders are {@code {name}}; a dynamic piece that depends on the reader's
 * language (a date with its weekday, a coverage sentence) arrives already worded as {@code {date}},
 * {@code {what}}, {@code {coverage}} and so on - see {@link BotText}. No money, no guest names, ever.
 *
 * <p><b>The Thai texts are a first translation and need a native speaker's review.</b>
 */
final class StaffBotTexts {

    private StaffBotTexts() {
    }

    private static void add(Key key, String en, String th, String ru) {
        NotificationMessages.add(key, en, th, ru);
    }

    static void register() {
        // ---- notifications -----------------------------------------------------------------------
        add(SHIFT_REQUEST_NEW,
                "{name} asks to {what}.{reasonLine}{coverageLines}\n{link}",
                "{name} ขอ{what}{reasonLine}{coverageLines}\n{link}",
                "{name} просит {what}.{reasonLine}{coverageLines}\n{link}");
        add(SHIFT_REQUEST_DECIDED_APPROVED,
                "Approved: your request to {what}. Decided by {by}.{commentLine}",
                "อนุมัติแล้ว: คำขอ{what} ตัดสินโดย {by}{commentLine}",
                "Одобрено: ваш запрос — {what}. Решение: {by}.{commentLine}");
        add(SHIFT_REQUEST_DECIDED_REJECTED,
                "Rejected: your request to {what}. Decided by {by}. Your schedule is unchanged.{commentLine}",
                "ไม่อนุมัติ: คำขอ{what} ตัดสินโดย {by} ตารางงานของคุณไม่เปลี่ยนแปลง{commentLine}",
                "Отклонено: ваш запрос — {what}. Решение: {by}. График не изменён.{commentLine}");
        add(SHIFT_REQUEST_EXPIRED,
                "Your request to {what} expired: {why} Nothing was changed.",
                "คำขอ{what}หมดอายุ: {why} ไม่มีการเปลี่ยนแปลง",
                "Ваш запрос ({what}) утратил силу: {why} Ничего не изменено.");
        add(SHIFT_REQUEST_REMINDER,
                "Still waiting for a decision: {name} asks to {what} (filed {hours} h ago).{reasonLine}{coverageLines}\n{link}",
                "ยังรอการตัดสินใจ: {name} ขอ{what} (ส่งมาแล้ว {hours} ชม.){reasonLine}{coverageLines}\n{link}",
                "Ждёт решения: {name} просит {what} (подано {hours} ч назад).{reasonLine}{coverageLines}\n{link}");
        add(SHIFT_EVE_REMINDER,
                "Reminder: tomorrow, {date}, you work {shift}.",
                "แจ้งเตือน: พรุ่งนี้ {date} คุณทำงานกะ {shift}",
                "Напоминание: завтра, {date}, у вас смена {shift}.");
        add(ROSTER_CHANGED,
                "Your schedule for {date} was changed: {before} -> {after}. Ask your manager if this is unexpected.",
                "ตารางงานของคุณวันที่ {date} ถูกเปลี่ยน: {before} -> {after} หากไม่คาดคิด กรุณาสอบถามผู้จัดการ",
                "Ваш график на {date} изменён: {before} -> {after}. Если это неожиданно, спросите менеджера.");

        // ---- fragments ------------------------------------------------------------------------------
        add(FRAG_REASON, "\nReason: {reason}", "\nเหตุผล: {reason}", "\nПричина: {reason}");
        add(FRAG_COMMENT, "\nComment: {comment}", "\nความเห็น: {comment}", "\nКомментарий: {comment}");
        add(FRAG_COVERAGE_BELOW,
                "\n! {area}: only {working} would work on {date} (minimum {minimum}).",
                "\n! {area}: จะเหลือคนทำงานเพียง {working} คนในวันที่ {date} (ขั้นต่ำ {minimum})",
                "\n! {area}: {date} останется только {working} (минимум {minimum}).");
        add(FRAG_COVERAGE_OK,
                "\n{area}: {working} would work on {date} (minimum {minimum}).",
                "\n{area}: จะมีคนทำงาน {working} คนในวันที่ {date} (ขั้นต่ำ {minimum})",
                "\n{area}: {date} будет работать {working} (минимум {minimum}).");
        add(FRAG_DAY_OFF, "day off", "วันหยุด", "выходной");
        add(FRAG_WHAT_MOVE,
                "move the shift on {date} ({shift}) to {target}",
                "ย้ายกะวันที่ {date} ({shift}) ไปเป็นวันที่ {target}",
                "перенести смену {date} ({shift}) на {target}");
        add(FRAG_WHAT_DELETE,
                "take {date} off (shift {shift})",
                "ขอหยุดวันที่ {date} (กะ {shift})",
                "взять выходной {date} (смена {shift})");

        // ---- what a request's message says once it is settled ------------------------------------
        add(OUTCOME_APPROVED, "Approved by {by} at {time}", "{by} อนุมัติเมื่อ {time}", "Одобрено: {by}, {time}");
        add(OUTCOME_REJECTED, "Rejected by {by} at {time}", "{by} ไม่อนุมัติเมื่อ {time}", "Отклонено: {by}, {time}");
        add(OUTCOME_CANCELLED, "Withdrawn at {time}", "ยกเลิกคำขอเมื่อ {time}", "Отозвано, {time}");
        add(OUTCOME_EXPIRED, "Expired at {time}: {why}", "หมดอายุเมื่อ {time}: {why}", "Утратило силу, {time}: {why}");
        add(EXP_SHIFT_STARTED, "The shift began before anyone decided.", "กะเริ่มแล้วโดยยังไม่มีการตัดสินใจ", "Смена началась, а решения не было.");
        add(EXP_ENTRY_CHANGED, "The shift was changed in the roster after the request was made.",
                "กะถูกเปลี่ยนในตารางงานหลังจากส่งคำขอ", "Смену изменили в графике после подачи запроса.");
        add(EXP_ENTRY_GONE, "The shift was removed from the roster after the request was made.",
                "กะถูกลบออกจากตารางงานหลังจากส่งคำขอ", "Смену удалили из графика после подачи запроса.");
        add(EXP_TARGET_TAKEN, "The requested day is no longer free.", "วันที่ขอย้ายไปไม่ว่างแล้ว", "Выбранный день уже занят.");
        add(EXP_EMPLOYEE_INACTIVE, "The employee is no longer active.", "พนักงานไม่ได้ทำงานอยู่แล้ว", "Сотрудник больше не активен.");
        add(EXP_LOCKED, "The shift was locked ('do not move') after the request was made.",
                "กะถูกล็อก (ห้ามเปลี่ยน) หลังจากส่งคำขอ", "Смену закрепили («не двигать») после подачи запроса.");
        add(EXP_APPLY_FAILED, "The roster refused the change.", "ตารางงานไม่รับการเปลี่ยนแปลง", "График не принял изменение.");

        // ---- menu, buttons, general replies ----------------------------------------------------------
        add(MENU_SHIFTS, "My shifts", "กะของฉัน", "Мои смены");
        add(MENU_DAY, "My day", "วันนี้ของฉัน", "Мой день");
        add(MENU_MONTH, "My month", "เดือนของฉัน", "Мой месяц");
        add(MENU_REQUESTS, "My requests", "คำขอของฉัน", "Мои заявки");
        add(MENU_SETTINGS, "Settings", "ตั้งค่า", "Настройки");
        add(MENU_PENDING, "Requests", "คำขอที่รออยู่", "Заявки");
        add(MENU_VACATION, "My leave", "วันลาของฉัน", "Мой отпуск");
        add(BOT_MENU, "Choose from the menu below.", "เลือกจากเมนูด้านล่าง", "Выберите в меню ниже.");
        add(BOT_FEATURE_OFF, "This function is turned off.", "ฟังก์ชันนี้ถูกปิดอยู่", "Эта функция отключена.");
        add(BOT_NOT_ALLOWED, "You can't do that.", "คุณไม่มีสิทธิ์ทำสิ่งนี้", "Это недоступно.");
        add(BOT_BUTTON_EXPIRED, "This button is no longer valid.", "ปุ่มนี้ใช้ไม่ได้แล้ว", "Эта кнопка больше не действует.");
        add(BOT_CANCELLED, "Cancelled.", "ยกเลิกแล้ว", "Отменено.");
        add(BTN_APPROVE, "Approve", "อนุมัติ", "Одобрить");
        add(BTN_REJECT, "Reject", "ไม่อนุมัติ", "Отклонить");
        add(BTN_MOVE, "Move {date}", "ย้าย {date}", "Перенести {date}");
        add(BTN_DELETE, "Day off {date}", "ขอหยุด {date}", "Выходной {date}");
        add(BTN_WITHDRAW, "Withdraw {date}", "ถอนคำขอ {date}", "Отозвать {date}");
        add(BTN_SKIP, "Skip", "ข้าม", "Пропустить");
        add(BTN_CANCEL, "Cancel", "ยกเลิก", "Отмена");
        add(BTN_EVE_ON, "Turn the evening reminder on", "เปิดการแจ้งเตือนตอนเย็น", "Включить вечернее напоминание");
        add(BTN_EVE_OFF, "Turn the evening reminder off", "ปิดการแจ้งเตือนตอนเย็น", "Выключить вечернее напоминание");
        add(BTN_VAC_SET, "Set leave", "ตั้งวันลา", "Указать отпуск");
        add(BTN_VAC_CLEAR, "Clear leave", "ล้างวันลา", "Снять отпуск");

        // ---- my shifts ---------------------------------------------------------------------------------
        add(SHIFTS_TITLE, "Your shifts for the next {days} days:", "กะของคุณใน {days} วันข้างหน้า:", "Ваши смены на ближайшие {days} дн.:");
        add(SHIFTS_NONE, "You have no shifts in the next {days} days.", "คุณไม่มีกะใน {days} วันข้างหน้า", "В ближайшие {days} дн. у вас нет смен.");
        add(SHIFT_LINE, "{date}{tag}: {shift}{area}{request}", "{date}{tag}: {shift}{area}{request}", "{date}{tag}: {shift}{area}{request}");
        add(TAG_TODAY, " (today)", " (วันนี้)", " (сегодня)");
        add(TAG_TOMORROW, " (tomorrow)", " (พรุ่งนี้)", " (завтра)");
        add(SHIFT_REQUEST_TAG, "  - request: {status}", "  - คำขอ: {status}", "  - заявка: {status}");

        // ---- filing a request -------------------------------------------------------------------------
        add(MOVE_PICK, "Pick the new day for your shift on {date}:", "เลือกวันใหม่สำหรับกะวันที่ {date}:", "Выберите новый день для смены {date}:");
        add(MOVE_NO_DATES, "There is no free day you can move this shift to.", "ไม่มีวันว่างที่ย้ายกะนี้ไปได้", "Нет свободного дня, на который можно перенести смену.");
        add(ASK_REASON, "Send the reason for your request (up to 300 characters), or /cancel.",
                "ส่งเหตุผลของคำขอ (ไม่เกิน 300 ตัวอักษร) หรือ /cancel", "Отправьте причину запроса (до 300 символов) или /cancel.");
        add(ASK_REASON_OPTIONAL, "Send the reason for your request (up to 300 characters), or press Skip.",
                "ส่งเหตุผลของคำขอ (ไม่เกิน 300 ตัวอักษร) หรือกดข้าม", "Отправьте причину запроса (до 300 символов) или нажмите «Пропустить».");
        add(REASON_TOO_LONG, "That is too long: up to 300 characters, please.", "ยาวเกินไป: ไม่เกิน 300 ตัวอักษร", "Слишком длинно: не более 300 символов.");
        add(REQUEST_SENT, "Request sent: {what}. You will get a message with the decision.",
                "ส่งคำขอแล้ว: {what} คุณจะได้รับข้อความแจ้งผล", "Заявка отправлена: {what}. Вы получите сообщение с решением.");
        add(REQUEST_WITHDRAWN, "Request withdrawn: {what}.", "ถอนคำขอแล้ว: {what}", "Заявка отозвана: {what}.");
        add(REJ_DISABLED, "Shift requests are turned off.", "ปิดการขอเปลี่ยนกะอยู่", "Заявки на смены отключены.");
        add(REJ_NOT_YOURS, "That shift is not yours.", "กะนี้ไม่ใช่ของคุณ", "Это не ваша смена.");
        add(REJ_LOCKED, "This shift is locked by your manager and can't be changed here.",
                "กะนี้ถูกผู้จัดการล็อก เปลี่ยนที่นี่ไม่ได้", "Эта смена закреплена менеджером, здесь её не изменить.");
        add(REJ_TOO_LATE, "Too late: a request must be made at least {hours} h before the shift starts.",
                "สายเกินไป: ต้องส่งคำขออย่างน้อย {hours} ชม. ก่อนกะเริ่ม", "Слишком поздно: заявку нужно подать не позднее чем за {hours} ч до начала смены.");
        add(REJ_TODAY, "Today's shift can't be changed here.", "เปลี่ยนกะของวันนี้ที่นี่ไม่ได้", "Сегодняшнюю смену здесь изменить нельзя.");
        add(REJ_PAST, "A shift that has passed can't be changed.", "กะที่ผ่านไปแล้วเปลี่ยนไม่ได้", "Прошедшую смену изменить нельзя.");
        add(REJ_TARGET, "Pick a day within the next {days} days.", "เลือกวันภายใน {days} วันข้างหน้า", "Выберите день в ближайшие {days} дн.");
        add(REJ_TARGET_BUSY, "You already have a shift on that day.", "คุณมีกะในวันนั้นอยู่แล้ว", "В этот день у вас уже есть смена.");
        add(REJ_PENDING, "There is already a request for this shift.", "มีคำขอสำหรับกะนี้อยู่แล้ว", "По этой смене уже есть заявка.");
        add(REJ_NOT_A_SHIFT, "Only working shifts can be changed.", "เปลี่ยนได้เฉพาะกะทำงาน", "Можно менять только рабочие смены.");
        add(REJ_REASON_REQUIRED, "Please give a reason.", "กรุณาระบุเหตุผล", "Укажите причину.");

        // ---- my day / my month ---------------------------------------------------------------------------
        add(DAY_TITLE, "Today, {date}", "วันนี้ {date}", "Сегодня, {date}");
        add(DAY_NO_SHIFT, "You have no shift today.", "วันนี้คุณไม่มีกะ", "Сегодня у вас нет смены.");
        add(DAY_SHIFT, "Shift: {shift}", "กะ: {shift}", "Смена: {shift}");
        add(DAY_STATE, "Status: {state}", "สถานะ: {state}", "Статус: {state}");
        add(DAY_PUNCHES, "Your scans today:", "การสแกนของคุณวันนี้:", "Ваши отметки сегодня:");
        add(DAY_NO_PUNCHES, "No scans yet.", "ยังไม่มีการสแกน", "Отметок пока нет.");
        add(DAY_IN, "in", "เข้า", "приход");
        add(DAY_OUT, "out", "ออก", "уход");
        add(STATE_NOT_YET_ARRIVED, "not here yet", "ยังไม่มา", "ещё не пришли");
        add(STATE_ON_SHIFT, "on shift", "กำลังทำงาน", "на смене");
        add(STATE_FINISHED, "finished", "เสร็จกะแล้ว", "смена закончена");
        add(STATE_SCHEDULED, "scheduled", "ตามตาราง", "по графику");
        add(STATE_ARRIVING_SOON, "starting soon", "ใกล้ถึงเวลา", "скоро начало");
        add(STATE_LATE, "late - no scan yet", "สาย - ยังไม่สแกน", "опоздание - отметки нет");
        add(STATE_BETWEEN_SHIFTS, "between the two parts of your shift", "ช่วงพักระหว่างกะ", "перерыв между частями смены");
        add(STATE_MISSED, "missed - no scan for the shift", "ขาด - ไม่มีการสแกนในกะ", "пропуск - отметки за смену нет");
        add(MONTH_SUMMARY,
                "{month}\nPlanned: {plannedDays} days, {plannedHours} h\nWorked: {workedDays} days, {workedHours} h",
                "{month}\nตามแผน: {plannedDays} วัน {plannedHours} ชม.\nทำงานจริง: {workedDays} วัน {workedHours} ชม.",
                "{month}\nПо плану: {plannedDays} дн., {plannedHours} ч\nФактически: {workedDays} дн., {workedHours} ч");

        // ---- my requests ---------------------------------------------------------------------------------
        add(REQUESTS_TITLE, "Your latest requests:", "คำขอล่าสุดของคุณ:", "Ваши последние заявки:");
        add(REQUESTS_NONE, "You have not made any requests.", "คุณยังไม่ได้ส่งคำขอ", "Заявок пока нет.");
        add(REQUEST_LINE, "{what} - {status}", "{what} - {status}", "{what} - {status}");
        add(STATUS_PENDING, "waiting", "รอการตัดสินใจ", "ждёт решения");
        add(STATUS_APPROVED, "approved", "อนุมัติแล้ว", "одобрена");
        add(STATUS_REJECTED, "rejected", "ไม่อนุมัติ", "отклонена");
        add(STATUS_CANCELLED, "withdrawn", "ถอนแล้ว", "отозвана");
        add(STATUS_EXPIRED, "expired", "หมดอายุ", "утратила силу");

        // ---- settings -----------------------------------------------------------------------------------
        add(SETTINGS_TEXT, "Language: {language}\nEvening-before reminder: {eve}",
                "ภาษา: {language}\nแจ้งเตือนตอนเย็นก่อนวันทำงาน: {eve}", "Язык: {language}\nНапоминание накануне смены: {eve}");
        add(WORD_ON, "on", "เปิด", "включено");
        add(WORD_OFF, "off", "ปิด", "выключено");
        add(LANG_CHANGED, "Language changed.", "เปลี่ยนภาษาแล้ว", "Язык изменён.");
        add(EVE_CHANGED, "Evening-before reminder: {eve}.", "แจ้งเตือนตอนเย็นก่อนวันทำงาน: {eve}", "Напоминание накануне смены: {eve}.");

        // ---- the deciders' view ---------------------------------------------------------------------------
        add(PENDING_TITLE, "Requests waiting for a decision:", "คำขอที่รอการตัดสินใจ:", "Заявки, ожидающие решения:");
        add(PENDING_NONE, "No requests are waiting.", "ไม่มีคำขอที่รออยู่", "Ожидающих заявок нет.");
        add(PENDING_ITEM, "{name} asks to {what}.{reasonLine}{coverageLines}", "{name} ขอ{what}{reasonLine}{coverageLines}", "{name} просит {what}.{reasonLine}{coverageLines}");
        add(TOAST_APPROVED, "Approved.", "อนุมัติแล้ว", "Одобрено.");
        add(TOAST_REJECTED, "Rejected.", "ไม่อนุมัติแล้ว", "Отклонено.");
        add(TOAST_ALREADY, "Already {status}.", "สถานะปัจจุบัน: {status}", "Уже: {status}.");

        // ---- the HR holder's leave -------------------------------------------------------------------------
        add(VAC_STATUS, "Your leave: {from} - {to}. While it lasts requests go to the administrators.",
                "วันลาของคุณ: {from} - {to} ระหว่างนี้คำขอจะส่งให้ผู้ดูแลระบบ", "Ваш отпуск: {from} - {to}. Пока он идёт, заявки решают администраторы.");
        add(VAC_NONE, "No leave is set.", "ไม่ได้ตั้งวันลา", "Отпуск не указан.");
        add(VAC_ASK_FROM, "Send the first day of your leave as YYYY-MM-DD, or /cancel.",
                "ส่งวันแรกของวันลาในรูปแบบ YYYY-MM-DD หรือ /cancel", "Отправьте первый день отпуска в формате ГГГГ-ММ-ДД или /cancel.");
        add(VAC_ASK_TO, "Now the last day (YYYY-MM-DD).", "ตอนนี้ส่งวันสุดท้าย (YYYY-MM-DD)", "Теперь последний день (ГГГГ-ММ-ДД).");
        add(VAC_BAD_DATE, "That is not a date like 2026-11-03, or it is before the first day.",
                "ไม่ใช่วันที่รูปแบบ 2026-11-03 หรืออยู่ก่อนวันแรก", "Это не дата вида 2026-11-03 или она раньше первого дня.");
        add(VAC_SAVED, "Leave saved: {from} - {to}.", "บันทึกวันลาแล้ว: {from} - {to}", "Отпуск сохранён: {from} - {to}.");
        add(VAC_CLEARED, "Leave cleared.", "ล้างวันลาแล้ว", "Отпуск снят.");
        add(VAC_NO_HR, "Nobody holds the HR function.", "ไม่มีผู้ถือหน้าที่ HR", "Функция HR никому не назначена.");
    }
}
