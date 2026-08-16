package com.sunsetbeach.service;

import com.sunsetbeach.model.PrinterCodepage;
import java.io.ByteArrayOutputStream;

/**
 * The read-only counterpart to {@link EscPosBuilder}: strips the handful of ESC/POS control
 * sequences {@link EscPosBuilder} ever emits (init, code-page select, bold, centering, partial
 * cut) back out of a queued {@code PrintJob.payload}, leaving the plain text a human can read -
 * what {@code GET /print-jobs/{id}/preview} returns. Knows exactly the same fixed set of control
 * codes {@link EscPosBuilder} writes, in the same way that builder isn't a general ESC/POS
 * library; a payload built by anything else wouldn't round-trip through this cleanly.
 */
public final class EscPosPreview {

    private EscPosPreview() {
    }

    public static String render(byte[] payload, PrinterCodepage codepage) {
        ByteArrayOutputStream text = new ByteArrayOutputStream();
        int i = 0;
        while (i < payload.length) {
            int b = payload[i] & 0xFF;
            if (b == 0x1B && i + 1 < payload.length) { // ESC
                int cmd = payload[i + 1] & 0xFF;
                if (cmd == 0x40) { // ESC @ - initialize (2 bytes)
                    i += 2;
                    continue;
                }
                if (cmd == 0x74 || cmd == 0x45 || cmd == 0x61) { // ESC t/E/a n - code page/bold/center (3 bytes)
                    i += 3;
                    continue;
                }
            }
            if (b == 0x1D && i + 1 < payload.length && (payload[i + 1] & 0xFF) == 0x56) { // GS V n - partial cut (3 bytes)
                i += 3;
                continue;
            }
            text.write(b);
            i++;
        }
        // The trailing feed()+cut leaves a few blank lines at the end - not useful in a preview.
        return new String(text.toByteArray(), PrinterCodepages.charset(codepage)).strip();
    }
}
