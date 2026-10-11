package com.sunsetbeach.service;

import com.sunsetbeach.model.PrinterCodepage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;

/**
 * Minimal ESC/POS byte-stream builder - just enough control codes to lay out the four
 * documents this app prints (kitchen/bar tickets, pre-bills, guest receipts, Z-reports) plus a
 * test page: init, code-page selection, bold, centering, and a paper cut. Not a general ESC/POS
 * library - no barcode/QR support because nothing here needs it. The guest receipt additionally
 * uses font selection, 2x magnification and a raster logo ({@link ReceiptRenderer}).
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

    /** ESC M n - the printer's built-in Font A (the default) or the smaller Font B. */
    public EscPosBuilder fontB(boolean on) {
        out.writeBytes(new byte[] {0x1B, 0x4D, (byte) (on ? 1 : 0)});
        return this;
    }

    /** GS ! n - glyph magnification: 1 is normal, 2 doubles both width and height. */
    public EscPosBuilder size(int magnification) {
        out.writeBytes(new byte[] {0x1D, 0x21, (byte) (magnification >= 2 ? 0x11 : 0x00)});
        return this;
    }

    /**
     * GS v 0 - a 1-bit raster image, {@code widthBytes} * 8 dots wide, row-major, MSB first, a
     * set bit printing black. Positioned by the current alignment like text.
     */
    public EscPosBuilder raster(int widthBytes, int heightDots, byte[] packed) {
        out.writeBytes(new byte[] {
            0x1D, 0x76, 0x30, 0x00,
            (byte) (widthBytes & 0xFF), (byte) (widthBytes >> 8),
            (byte) (heightDots & 0xFF), (byte) (heightDots >> 8)
        });
        out.writeBytes(packed);
        return this;
    }

    public EscPosBuilder center(boolean on) {
        out.writeBytes(new byte[] {0x1B, 0x61, (byte) (on ? 1 : 0)}); // ESC a n (0=left, 1=center)
        return this;
    }

    public EscPosBuilder text(String s) {
        out.writeBytes(asciiPunctuation(s).getBytes(charset));
        return this;
    }

    /**
     * The receipt code pages (PC437, TIS-620) have no em/en dash, curly quotes or ellipsis, and
     * encoding one gives a literal "?" on paper ("Restaurant ? T1"). Those come from our own
     * wording and from menu text typed in a word processor, so they are swapped for their ASCII
     * look-alikes before encoding. Anything else unencodable is still the charset's call.
     */
    static String asciiPunctuation(String s) {
        return s.replace('\u2014', '-').replace('\u2013', '-').replace('\u2018', '\'').replace('\u2019', '\'')
                .replace('\u201C', '"').replace('\u201D', '"').replace("\u2026", "...").replace('\u00A0', ' ');
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
