package com.sunsetbeach.service;

import com.sunsetbeach.model.PrinterCodepage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;

/**
 * Minimal ESC/POS byte-stream builder - just enough control codes to lay out the four
 * documents this app prints (kitchen/bar tickets, pre-bills, guest receipts, Z-reports) plus a
 * test page: init, code-page selection, bold, centering, and a paper cut. Not a general ESC/POS
 * library - there is no image/barcode/QR support because nothing here needs it.
 */
public final class EscPosBuilder {

    private static final int LINE_WIDTH = 42;

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final Charset charset;

    public EscPosBuilder(PrinterCodepage codepage) {
        this.charset = PrinterCodepages.charset(codepage);
        out.writeBytes(new byte[] {0x1B, 0x40}); // ESC @ - initialize
        out.writeBytes(new byte[] {0x1B, 0x74, (byte) PrinterCodepages.escPosTableIndex(codepage)}); // ESC t n
    }

    public EscPosBuilder bold(boolean on) {
        out.writeBytes(new byte[] {0x1B, 0x45, (byte) (on ? 1 : 0)}); // ESC E n
        return this;
    }

    public EscPosBuilder center(boolean on) {
        out.writeBytes(new byte[] {0x1B, 0x61, (byte) (on ? 1 : 0)}); // ESC a n (0=left, 1=center)
        return this;
    }

    public EscPosBuilder text(String s) {
        out.writeBytes(s.getBytes(charset));
        return this;
    }

    public EscPosBuilder line(String s) {
        return text(s).newline();
    }

    public EscPosBuilder newline() {
        out.write('\n');
        return this;
    }

    public EscPosBuilder feed(int lines) {
        for (int i = 0; i < lines; i++) {
            newline();
        }
        return this;
    }

    public EscPosBuilder divider() {
        return line("-".repeat(LINE_WIDTH));
    }

    /** Right-pads/truncates so two columns (label, value) line up on a fixed-width receipt. */
    public EscPosBuilder twoColumn(String left, String right) {
        int rightWidth = right.length();
        int leftWidth = Math.max(1, LINE_WIDTH - rightWidth);
        String leftPadded = left.length() > leftWidth ? left.substring(0, leftWidth) : left;
        StringBuilder row = new StringBuilder(leftPadded);
        while (row.length() < LINE_WIDTH - rightWidth) {
            row.append(' ');
        }
        row.append(right);
        return line(row.toString());
    }

    /** Feeds a few blank lines then does a partial cut - the trailing feed keeps the cut from slicing through the last printed line. */
    public byte[] cutAndBuild() {
        feed(3);
        out.writeBytes(new byte[] {0x1D, 0x56, 0x01}); // GS V 1 - partial cut
        return out.toByteArray();
    }
}
