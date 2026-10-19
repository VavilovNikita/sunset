package com.sunsetbeach.notification;

import com.sunsetbeach.model.NotificationLanguage;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Small language-dependent pieces shared by the bot's replies and the notifications about shifts: a date
 * with its weekday, a shift label, and the {@code key.EN / key.TH / key.RU} parameter trick
 * ({@code NotificationService#raise} picks the variant of the recipient's own language), which lets one
 * raise call serve recipients who read different languages.
 *
 * <p><b>The Thai names are a first translation and need a native speaker's review</b>, like the texts.
 */
public final class BotText {

    private static final String[] EN_DAYS = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
    private static final String[] TH_DAYS = {"จ.", "อ.", "พ.", "พฤ.", "ศ.", "ส.", "อา."};
    private static final String[] RU_DAYS = {"пн", "вт", "ср", "чт", "пт", "сб", "вс"};
    private static final String[] EN_MONTHS = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
    private static final String[] TH_MONTHS = {"ม.ค.", "ก.พ.", "มี.ค.", "เม.ย.", "พ.ค.", "มิ.ย.", "ก.ค.", "ส.ค.", "ก.ย.", "ต.ค.", "พ.ย.", "ธ.ค."};
    private static final String[] RU_MONTHS = {"янв", "фев", "мар", "апр", "мая", "июн", "июл", "авг", "сен", "окт", "ноя", "дек"};

    private static final String[] EN_MONTHS_LONG = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
    private static final String[] TH_MONTHS_LONG = {"มกราคม", "กุมภาพันธ์", "มีนาคม", "เมษายน", "พฤษภาคม", "มิถุนายน", "กรกฎาคม", "สิงหาคม", "กันยายน", "ตุลาคม", "พฤศจิกายน", "ธันวาคม"};
    private static final String[] RU_MONTHS_LONG = {"Январь", "Февраль", "Март", "Апрель", "Май", "Июнь", "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"};

    private BotText() {
    }

    /** {@code Mon 20 Oct} / {@code пн 20 окт} / {@code จ. 20 ต.ค.} - the hotel date a person reads, no year. */
    public static String date(LocalDate date, NotificationLanguage language) {
        int day = date.getDayOfWeek().getValue() - 1;
        int month = date.getMonthValue() - 1;
        return switch (language) {
            case EN -> EN_DAYS[day] + " " + date.getDayOfMonth() + " " + EN_MONTHS[month];
            case TH -> TH_DAYS[day] + " " + date.getDayOfMonth() + " " + TH_MONTHS[month];
            case RU -> RU_DAYS[day] + " " + date.getDayOfMonth() + " " + RU_MONTHS[month];
        };
    }

    /** {@code Oct 2026} / {@code окт 2026} / {@code ต.ค. 2026}. */
    public static String month(java.time.YearMonth month, NotificationLanguage language) {
        int m = month.getMonthValue() - 1;
        String name = switch (language) {
            case EN -> EN_MONTHS[m];
            case TH -> TH_MONTHS[m];
            case RU -> RU_MONTHS[m];
        };
        return name + " " + month.getYear();
    }

    /** {@code October 2026} / {@code Октябрь 2026} / {@code ตุลาคม 2026} - the calendar's heading. */
    public static String monthLong(java.time.YearMonth month, NotificationLanguage language) {
        int m = month.getMonthValue() - 1;
        String name = switch (language) {
            case EN -> EN_MONTHS_LONG[m];
            case TH -> TH_MONTHS_LONG[m];
            case RU -> RU_MONTHS_LONG[m];
        };
        return name + " " + month.getYear();
    }

    /** The short weekday name, Monday first ({@code dayOfWeek} is {@link java.time.DayOfWeek#getValue()}, 1..7). */
    public static String weekday(int dayOfWeek, NotificationLanguage language) {
        return switch (language) {
            case EN -> EN_DAYS[dayOfWeek - 1];
            case TH -> TH_DAYS[dayOfWeek - 1];
            case RU -> RU_DAYS[dayOfWeek - 1];
        };
    }

    /** {@code N1 09:00-18:00}, or just the code for a shift with no times. */
    public static String shift(String code, String times) {
        return times == null || times.isBlank() ? code : code + " " + times;
    }

    /** Puts {@code key.EN}, {@code key.TH} and {@code key.RU} into {@code params}. */
    public static void perLanguage(Map<String, String> params, String key, Function<NotificationLanguage, String> value) {
        for (NotificationLanguage language : NotificationLanguage.values()) {
            params.put(key + "." + language.getValue(), value.apply(language));
        }
    }

    /**
     * Replaces every {@code key.<LANG>} parameter by plain {@code key} for the given language and drops the
     * variants - what {@code NotificationService#raise} does for each recipient.
     */
    public static Map<String, String> forLanguage(Map<String, String> params, NotificationLanguage language) {
        Map<String, String> out = new HashMap<>();
        String suffix = "." + language.getValue();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            if (isVariant(key)) {
                if (key.endsWith(suffix)) {
                    out.put(key.substring(0, key.length() - suffix.length()), entry.getValue());
                }
            } else {
                out.putIfAbsent(key, entry.getValue());
            }
        }
        return out;
    }

    private static boolean isVariant(String key) {
        return key.endsWith(".EN") || key.endsWith(".TH") || key.endsWith(".RU");
    }
}
