package com.sunsetbeach.service;

/**
 * Shared CSV row assembly for {@code GET /bookings/export} and {@code GET /shifts/{id}/export}
 * - one place so the two exports can't drift apart on quoting/escaping again.
 *
 * Every file opens with a UTF-8 BOM (so Excel doesn't mis-decode non-ASCII guest/menu names as
 * mojibake) followed by a literal {@code sep=,} line. That's Excel's own convention for pinning
 * the field delimiter regardless of the OS's regional "list separator" setting - a ru-RU (or any
 * other non-en-US) Windows install defaults that setting to `;`, which is why a plain
 * comma-delimited CSV opened by double-click there lands as one column. Switching the delimiter
 * to `;` instead would only flip which locale breaks, since a comma-locale Excel would then be
 * the one dumping everything into column A - `sep=,` sidesteps the guessing game entirely and
 * works on any locale. Non-Excel consumers that don't understand the directive can simply skip
 * line 1 (the BOM+`sep=,` line), same as they'd need to skip a header row anyway.
 */
public final class CsvBuilder {

    /** Literal U+FEFF (UTF-8 BOM) via escape - avoids depending on this file's own encoding. */
    private static final char BOM = '﻿';

    private final StringBuilder sb = new StringBuilder().append(BOM).append("sep=,\r\n");
    private boolean firstRow = true;

    public CsvBuilder row(String... fields) {
        if (!firstRow) {
            sb.append("\r\n");
        }
        firstRow = false;
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(escape(fields[i]));
        }
        return this;
    }

    @Override
    public String toString() {
        return sb.toString();
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        if (value.indexOf(',') >= 0 || value.indexOf('"') >= 0 || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
        return value;
    }
}
