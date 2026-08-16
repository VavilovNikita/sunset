package com.sunsetbeach.service;

import com.sunsetbeach.model.PrinterCodepage;
import java.nio.charset.Charset;

/**
 * Maps the printer-configurable {@link PrinterCodepage} to the two things ESC/POS actually
 * needs: the Java {@link Charset} text is encoded into, and the raw "ESC t n" code-page table
 * index sent to the printer so it interprets those same bytes with the matching glyph table.
 * Kept out of the generated {@code PrinterCodepage} enum (regenerated from openapi.yaml, so
 * custom methods on it wouldn't survive a regen) and out of {@link EscPosBuilder} so the
 * mapping lives in exactly one place.
 */
public final class PrinterCodepages {

    private PrinterCodepages() {
    }

    /**
     * {@code String.getBytes(Charset)} (how this is actually used, in EscPosBuilder) replaces
     * any character outside the chosen code page's repertoire with that charset's default
     * replacement byte (typically {@code ?}) rather than throwing - a receipt with an
     * occasional '?' in a name is a minor cosmetic issue, a print job that throws and never
     * gets queued at all is not. Full arbitrary-Unicode support (e.g. Chinese guest names on a
     * Latin-only printer) would need raster/bitmap printing instead of a single-byte code page,
     * which is out of scope here.
     */
    public static Charset charset(PrinterCodepage codepage) {
        return switch (codepage) {
            case PC437 -> Charset.forName("Cp437");
            case TIS620 -> Charset.forName("windows-874");
        };
    }

    /**
     * There is no single universal ESC/POS table index for a given code page across vendors -
     * 0 = PC437 is the one near-universal constant, but Thai table numbering genuinely differs
     * between Epson's own firmware and the generic ESC/POS-clone controllers common in
     * budget/imported thermal printers. 21 is Epson's documented "Thai Character Code 42"
     * table; if a specific installed printer uses a different number for Thai, this is the one
     * constant to change.
     */
    public static int escPosTableIndex(PrinterCodepage codepage) {
        return switch (codepage) {
            case PC437 -> 0;
            case TIS620 -> 21;
        };
    }
}
