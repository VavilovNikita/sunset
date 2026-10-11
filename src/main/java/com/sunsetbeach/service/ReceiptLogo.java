package com.sunsetbeach.service;

import com.sunsetbeach.error.BadRequestException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import javax.imageio.ImageIO;

/**
 * The receipt logo in its two forms: what is stored (a gray-valued PNG on white, at most 576x400 -
 * the most any receipt printer here can use, see {@link #normalize}) and what is printed (a pure
 * black-and-white bitmap at the paper's width times the chosen percentage, {@link #toBitmap}).
 * Dithering happens at print time, not upload time, so changing the paper width or the logo's
 * size never re-dithers an already-dithered picture.
 */
public final class ReceiptLogo {

    public static final int MAX_STORED_WIDTH = 576;
    public static final int MAX_STORED_HEIGHT = 400;
    private static final int MAX_PRINT_HEIGHT = 400;

    private ReceiptLogo() {}

    /** One bit per dot, row-major, MSB first, a set bit meaning black - the layout GS v 0 expects. */
    public record Bitmap(int widthDots, int heightDots, byte[] packed) {

        public int widthBytes() {
            return (widthDots + 7) / 8;
        }

        /** The same dots as a PNG, for the settings page's preview. */
        public String toPngBase64() {
            BufferedImage image = new BufferedImage(widthDots, heightDots, BufferedImage.TYPE_BYTE_BINARY);
            int stride = widthBytes();
            for (int y = 0; y < heightDots; y++) {
                for (int x = 0; x < widthDots; x++) {
                    boolean black = (packed[y * stride + x / 8] & (0x80 >> (x % 8))) != 0;
                    image.setRGB(x, y, black ? 0x000000 : 0xFFFFFF);
                }
            }
            return Base64.getEncoder().encodeToString(encodePng(image));
        }
    }

    /** Flattens onto white (transparent PNGs would otherwise print as a black box), converts to gray and scales down to the stored maximum. */
    public static byte[] normalize(byte[] uploaded) {
        BufferedImage source;
        try {
            source = ImageIO.read(new ByteArrayInputStream(uploaded));
        } catch (IOException | RuntimeException e) {
            throw new BadRequestException("The image could not be read");
        }
        if (source == null) {
            throw new BadRequestException("The image could not be read - use a PNG or JPEG");
        }
        double scale = Math.min(1.0, Math.min((double) MAX_STORED_WIDTH / source.getWidth(), (double) MAX_STORED_HEIGHT / source.getHeight()));
        int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
        return encodePng(flattenGray(source, width, height));
    }

    /** {@code percent} of {@code paperDots} wide (a multiple of 8, as raster rows are bytes), height in proportion, never taller than 400 dots. */
    public static Bitmap toBitmap(byte[] storedPng, int paperDots, int percent) {
        BufferedImage stored;
        try {
            stored = ImageIO.read(new ByteArrayInputStream(storedPng));
        } catch (IOException e) {
            throw new IllegalStateException("Stored receipt logo is unreadable", e);
        }
        int width = Math.max(8, (paperDots * percent / 100) / 8 * 8);
        int height = Math.max(1, (int) Math.round((double) stored.getHeight() * width / stored.getWidth()));
        if (height > MAX_PRINT_HEIGHT) {
            height = MAX_PRINT_HEIGHT;
            width = Math.max(8, (int) Math.round((double) stored.getWidth() * height / stored.getHeight()) / 8 * 8);
            height = Math.max(1, (int) Math.round((double) stored.getHeight() * width / stored.getWidth()));
        }
        BufferedImage gray = flattenGray(stored, width, height);

        // Floyd-Steinberg on 0..255 luminance; error is carried to the right and the row below.
        float[][] lum = new float[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                lum[y][x] = gray.getRGB(x, y) & 0xFF;
            }
        }
        int stride = (width + 7) / 8;
        byte[] packed = new byte[stride * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float old = lum[y][x];
                float quantised = old < 128f ? 0f : 255f;
                float err = old - quantised;
                if (quantised == 0f) {
                    packed[y * stride + x / 8] |= (byte) (0x80 >> (x % 8));
                }
                if (x + 1 < width) lum[y][x + 1] += err * 7f / 16f;
                if (y + 1 < height) {
                    if (x > 0) lum[y + 1][x - 1] += err * 3f / 16f;
                    lum[y + 1][x] += err * 5f / 16f;
                    if (x + 1 < width) lum[y + 1][x + 1] += err / 16f;
                }
            }
        }
        return new Bitmap(width, height, packed);
    }

    /**
     * Scaled onto white, then reduced to luminance by hand. Drawing straight into a
     * {@code TYPE_BYTE_GRAY} image makes Java convert through a linear gray colour space, which
     * shifts every mid-tone (sRGB 50% gray comes out far lighter) - a photo would print washed out.
     */
    /** Returns an RGB image whose every pixel has r = g = b (kept RGB, not a gray image type, so reading it back never goes through Java's gray colour conversion). */
    private static BufferedImage flattenGray(BufferedImage source, int width, int height) {
        Image scaled = source.getScaledInstance(width, height, Image.SCALE_AREA_AVERAGING);
        BufferedImage rgb = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, width, height);
            g.drawImage(scaled, 0, 0, null);
        } finally {
            g.dispose();
        }
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int p = rgb.getRGB(x, y);
                int lum = (int) Math.round(0.299 * ((p >> 16) & 0xFF) + 0.587 * ((p >> 8) & 0xFF) + 0.114 * (p & 0xFF));
                rgb.setRGB(x, y, (lum << 16) | (lum << 8) | lum);
            }
        }
        return rgb;
    }

    private static byte[] encodePng(BufferedImage image) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("PNG encoding failed", e);
        }
    }
}
