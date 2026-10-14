package com.sunsetbeach.util;

/**
 * Trimming for names and labels. {@link String#strip()} only removes what {@link Character#isWhitespace}
 * accepts, which leaves a no-break space (U+00A0), the other Unicode space separators ({@code \p{Z}}) and
 * invisible zero-width characters in place - a "name" made only of those looked non-empty and passed
 * {@code minLength}. Every {@code x-trim} property in the generated models goes through {@link #strip}.
 */
public final class NameText {

    private NameText() {
    }

    /** Whitespace, any Unicode space/line/paragraph separator, or an invisible zero-width/format character. */
    static boolean isBlankChar(int c) {
        if (Character.isWhitespace(c) || Character.isSpaceChar(c)) {
            return true;
        }
        return c == 0x200B || c == 0x200C || c == 0x200D || c == 0x2060 || c == 0xFEFF
                || c == 0x00AD || c == 0x180E || c == 0x200E || c == 0x200F;
    }

    public static String strip(String value) {
        if (value == null) {
            return null;
        }
        int start = 0;
        int end = value.length();
        while (start < end && isBlankChar(value.charAt(start))) {
            start++;
        }
        while (end > start && isBlankChar(value.charAt(end - 1))) {
            end--;
        }
        return value.substring(start, end);
    }

    /** Trimmed, inner runs of blanks collapsed to one plain space - the form used to compare names. */
    public static String collapse(String value) {
        if (value == null) {
            return null;
        }
        String s = strip(value);
        StringBuilder out = new StringBuilder(s.length());
        boolean inBlank = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (isBlankChar(c)) {
                if (!inBlank) {
                    out.append(' ');
                }
                inBlank = true;
            } else {
                out.append(c);
                inBlank = false;
            }
        }
        return out.toString();
    }
}
