package com.sunsetbeach.notification;

import static com.sunsetbeach.notification.NotificationMessages.Key.*;

import com.sunsetbeach.notification.NotificationMessages.Key;

/**
 * The texts of the staff bot's role screens (an engineer's tasks, a housekeeper's blocks, a therapist's procedures,
 * "who is on shift", the managers' summary) and of the two morning messages. Beside {@link StaffBotTexts} for the
 * same reason that class sits beside {@link NotificationMessages}: to keep each readable. No money and no guest
 * names in any of them - a room label and a number of guests are fine.
 *
 * <p><b>The Thai texts are a first translation and need a native speaker's review.</b>
 */
final class StaffBotScreenTexts {

    private StaffBotScreenTexts() {
    }

    private static void add(Key key, String en, String th, String ru) {
        NotificationMessages.add(key, en, th, ru);
    }

    static void register() {
        // ---- menu ----------------------------------------------------------------------------------
        add(MENU_TASKS, "Tasks", "งานซ่อมบำรุง", "Задачи");
        add(MENU_BLOCKS, "Room blocks", "ห้องที่ถูกปิด", "Блоки комнат");
        add(MENU_PROCEDURES, "My procedures", "หัตถการของฉัน", "Мои процедуры");
        add(MENU_ONSHIFT, "Who is on shift", "ใครอยู่ในกะ", "Кто на смене");
        add(MENU_SUMMARY, "Summary", "สรุปประจำวัน", "Сводка");

        // ---- the engineer's tasks -----------------------------------------------------------------------
        add(TASKS_TITLE, "Open maintenance tasks:", "งานซ่อมบำรุงที่ยังเปิดอยู่:", "Открытые задачи обслуживания:");
        add(TASKS_NONE, "There are no open tasks.", "ไม่มีงานที่ค้างอยู่", "Открытых задач нет.");
        add(TASKS_MORE, "...and {count} more in the admin screen.", "...และอีก {count} งานในหน้าผู้ดูแล", "...и ещё {count} в админке.");
        add(TASK_LINE,
                "Room {room}: {description}\nOpen {days} d, {state}. {block}",
                "ห้อง {room}: {description}\nเปิดมา {days} วัน สถานะ: {state} {block}",
                "Комната {room}: {description}\nОткрыта {days} дн., {state}. {block}");
        add(TASK_CLOSED_LINE, "Room {room}: {description}\nDone.", "ห้อง {room}: {description}\nเสร็จแล้ว", "Комната {room}: {description}\nГотово.");
        add(TASK_STATE_OPEN, "open", "เปิด", "открыта");
        add(TASK_STATE_IN_PROGRESS, "in progress", "กำลังดำเนินการ", "в работе");
        add(TASK_STATE_DONE, "done", "เสร็จแล้ว", "выполнена");
        add(TASK_BLOCK_UNTIL, "Room blocked until {to}.", "ห้องถูกปิดถึง {to}", "Комната в блоке до {to}.");
        add(TASK_NO_BLOCK, "No block.", "ไม่มีการปิดห้อง", "Блока нет.");
        add(BTN_TASK_START, "Start", "เริ่มงาน", "Начать");
        add(BTN_TASK_DONE, "Done", "เสร็จแล้ว", "Готово");
        add(TOAST_TASK_STARTED, "Started.", "เริ่มงานแล้ว", "Начата.");
        add(TOAST_TASK_DONE, "Done.", "เสร็จแล้ว", "Готово.");
        add(TOAST_TASK_ALREADY, "Already {status}.", "สถานะปัจจุบัน: {status}", "Уже: {status}.");

        // ---- the housekeeper's blocks ---------------------------------------------------------------------
        add(BLOCKS_TITLE, "Room blocks you answer for (now and the next {days} days):",
                "ห้องที่คุณรับผิดชอบและถูกปิด (ตอนนี้และ {days} วันข้างหน้า):", "Блоки, за которые вы отвечаете (сейчас и ближайшие {days} дн.):");
        add(BLOCKS_NONE, "There are no such blocks in the next {days} days.", "ไม่มีการปิดห้องใน {days} วันข้างหน้า", "В ближайшие {days} дн. таких блоков нет.");
        add(BLOCK_LINE, "Room {room}: {from} - {to}{reason}", "ห้อง {room}: {from} - {to}{reason}", "Комната {room}: {from} - {to}{reason}");

        // ---- the therapist's procedures --------------------------------------------------------------------
        add(PROC_TITLE, "Your procedures:", "หัตถการของคุณ:", "Ваши процедуры:");
        add(PROC_NONE, "You have no procedures in this period.", "คุณไม่มีหัตถการในช่วงนี้", "В этот период процедур у вас нет.");
        add(PROC_DAY_NONE, "{date}: no procedures.", "{date}: ไม่มีหัตถการ", "{date}: процедур нет.");
        add(PROC_LINE, "{time}, {minutes} min: {treatments} - table {table}{room}", "{time} {minutes} นาที: {treatments} - โต๊ะ {table}{room}",
                "{time}, {minutes} мин: {treatments} - стол {table}{room}");
        add(PROC_ROOM, ", room {room}", " ห้อง {room}", ", комната {room}");
        add(THERAPIST_MORNING, "Good morning! Procedures today: {count}, the first at {time}.",
                "สวัสดีตอนเช้า! วันนี้คุณมีหัตถการ {count} รายการ รายการแรกเวลา {time}", "Доброе утро! Сегодня процедур: {count}, первая в {time}.");
        add(SETTINGS_THERAPIST, "Morning message about your procedures: {morning}", "ข้อความตอนเช้าเรื่องหัตถการของคุณ: {morning}",
                "Утреннее сообщение о ваших процедурах: {morning}");
        add(BTN_MORNING_ON, "Turn the morning message on", "เปิดข้อความตอนเช้า", "Включить утреннее сообщение");
        add(BTN_MORNING_OFF, "Turn the morning message off", "ปิดข้อความตอนเช้า", "Выключить утреннее сообщение");
        add(MORNING_CHANGED, "Morning message: {morning}.", "ข้อความตอนเช้า: {morning}", "Утреннее сообщение: {morning}.");

        // ---- who is on shift -----------------------------------------------------------------------------
        add(ONSHIFT_TITLE, "Who is on shift, {date}", "ใครอยู่ในกะ {date}", "Кто на смене, {date}");
        add(ONSHIFT_TERMINAL_SILENT,
                "! The attendance terminal is not responding ({names}). Late and absent people can't be told apart from people who simply were not scanned, so only the roster is shown.",
                "! เครื่องสแกนนิ้วไม่ตอบสนอง ({names}) แยกคนมาสายหรือขาดงานจากคนที่ยังไม่ได้สแกนไม่ได้ จึงแสดงเฉพาะตารางงาน",
                "! Терминал посещаемости не отвечает ({names}). Опоздавших и отсутствующих нельзя отличить от тех, кого просто не отметили, поэтому показан только график.");
        add(ONSHIFT_TERMINAL_NONE,
                "! No attendance terminal is active, so nobody's scans can be seen. Only the roster is shown.",
                "! ไม่มีเครื่องสแกนนิ้วที่เปิดใช้งาน จึงมองไม่เห็นการสแกนของใคร แสดงเฉพาะตารางงาน",
                "! Нет активного терминала посещаемости, отметок никого не видно. Показан только график.");
        add(ONSHIFT_NOBODY, "Nobody is on the roster or has clocked in today.", "วันนี้ไม่มีใครอยู่ในตารางงานหรือสแกนเข้างาน", "Сегодня в графике и на отметках никого нет.");
        add(ONSHIFT_AREA_NONE, "No department", "ไม่มีแผนก", "Без отдела");
        add(ONSHIFT_ROSTER, "On the roster: {names}", "ตามตารางงาน: {names}", "По графику: {names}");
        add(GROUP_LATE, "Late: {names}", "มาสาย: {names}", "Опаздывают: {names}");
        add(GROUP_MISSED, "Absent: {names}", "ขาดงาน: {names}", "Отсутствуют: {names}");
        add(GROUP_ON_SHIFT, "On shift: {names}", "อยู่ในกะ: {names}", "На смене: {names}");
        add(GROUP_COMING, "Still to come: {names}", "ยังไม่มา: {names}", "Ещё придут: {names}");
        add(GROUP_FINISHED, "Finished: {names}", "เลิกกะแล้ว: {names}", "Закончили: {names}");

        // ---- the summary ---------------------------------------------------------------------------------
        add(SUM_TITLE, "Summary for {date}", "สรุปประจำวันที่ {date}", "Сводка на {date}");
        add(SUM_ARRIVALS, "Arrivals: {expected} still expected ({total} booked for today).",
                "ผู้เข้าพัก: ยังรอ {expected} ({total} จองสำหรับวันนี้)", "Заезды: ещё ждём {expected} (на сегодня {total}).");
        add(SUM_DEPARTURES, "Departures: {due} still to leave today ({total} due out today).",
                "ผู้ออกจากห้อง: ยังไม่ออก {due} ({total} ครบกำหนดวันนี้)", "Выезды: ещё выезжают {due} (на сегодня {total}).");
        add(SUM_OVERDUE, "Overdue departures (still in the house past check-out): {overdue}.",
                "เกินกำหนดออก (ยังพักอยู่หลังวันเช็คเอาท์): {overdue}", "Просроченные выезды (всё ещё в отеле после даты выезда): {overdue}.");
        add(SUM_INHOUSE, "In the house: {rooms} rooms, {guests} guests.", "พักอยู่ในโรงแรม: {rooms} ห้อง {guests} คน", "Проживают: комнат {rooms}, гостей {guests}.");
        add(SUM_OCCUPANCY, "Occupancy today: {percent}. Rooms still free tonight: {free}.",
                "อัตราเข้าพักวันนี้: {percent} ห้องที่ยังว่างคืนนี้: {free}", "Занятость сегодня: {percent}. Свободных комнат на ночь: {free}.");
        add(SUM_ROOMS, "Rooms blocked: {blocked}. Rooms waiting to be cleaned: {dirty}.",
                "ห้องที่ถูกปิด: {blocked} ห้องที่รอทำความสะอาด: {dirty}", "Комнат в блоке: {blocked}. Неубранных комнат: {dirty}.");
        add(SUM_STAFF, "Staff: {rostered} on the roster today, {onShift} on shift now, {absent} absent.",
                "พนักงาน: ตามตารางวันนี้ {rostered} อยู่ในกะตอนนี้ {onShift} ขาดงาน {absent}", "Персонал: по графику сегодня {rostered}, на смене сейчас {onShift}, отсутствуют {absent}.");
        add(SUM_STAFF_SILENT, "Staff: {rostered} on the roster today. The attendance terminal is not responding, so who is on shift and who is absent is not shown.",
                "พนักงาน: ตามตารางวันนี้ {rostered} เครื่องสแกนนิ้วไม่ตอบสนอง จึงไม่แสดงว่าใครอยู่ในกะหรือขาดงาน",
                "Персонал: по графику сегодня {rostered}. Терминал посещаемости не отвечает, поэтому кто на смене и кто отсутствует - не показано.");
        add(SUM_REQUESTS, "Shift requests waiting for a decision: {count}.", "คำขอเปลี่ยนกะที่รอการตัดสินใจ: {count}", "Заявок на смены, ждущих решения: {count}.");
        add(SUM_NOTHING, "No blocks are switched on for the summary.", "ไม่ได้เปิดส่วนใดของสรุป", "В настройках сводки ничего не включено.");
        add(MORNING_SUMMARY, "{summary}", "{summary}", "{summary}");
    }
}
