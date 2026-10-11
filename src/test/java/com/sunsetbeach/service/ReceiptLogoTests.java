package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.error.BadRequestException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class ReceiptLogoTests {

    @Test
    void normalize_scalesDownToThePrinterMaximum_keepingProportions() throws Exception {
        BufferedImage stored = ImageIO.read(new ByteArrayInputStream(ReceiptLogo.normalize(PrintRendererTests.png(2000, 1000, 0x336699))));

        assertThat(stored.getWidth()).isEqualTo(576);
        assertThat(stored.getHeight()).isEqualTo(288);
    }

    @Test
    void normalize_neverUpscalesASmallLogo() throws Exception {
        BufferedImage stored = ImageIO.read(new ByteArrayInputStream(ReceiptLogo.normalize(PrintRendererTests.png(120, 60, 0x000000))));

        assertThat(stored.getWidth()).isEqualTo(120);
    }

    /** A transparent PNG's pixels are black with alpha 0; flattened onto paper-white they must print as nothing. */
    @Test
    void transparentPixels_flattenToWhite_andPrintNoDots() throws Exception {
        BufferedImage img = new BufferedImage(100, 50, BufferedImage.TYPE_INT_ARGB); // all transparent
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);

        ReceiptLogo.Bitmap bitmap = ReceiptLogo.toBitmap(ReceiptLogo.normalize(out.toByteArray()), 576, 100);

        assertThat(bitmap.packed()).containsOnly((byte) 0);
    }

    @Test
    void blackLogo_printsEveryDot() throws Exception {
        ReceiptLogo.Bitmap bitmap = ReceiptLogo.toBitmap(ReceiptLogo.normalize(PrintRendererTests.png(100, 50, 0x000000)), 384, 50);

        assertThat(bitmap.widthDots()).isEqualTo(192);
        assertThat(bitmap.packed()).containsOnly((byte) 0xFF);
    }

    @Test
    void widthIsAMultipleOfEight_andHeightFollowsTheAspectRatio() throws Exception {
        ReceiptLogo.Bitmap bitmap = ReceiptLogo.toBitmap(ReceiptLogo.normalize(PrintRendererTests.png(300, 150, 0x808080)), 576, 33);

        assertThat(bitmap.widthDots() % 8).isZero();
        assertThat(bitmap.heightDots()).isBetween(bitmap.widthDots() / 2 - 1, bitmap.widthDots() / 2 + 1);
        assertThat(bitmap.packed()).hasSize(bitmap.widthBytes() * bitmap.heightDots());
    }

    @Test
    void midGray_isDitheredToAMixOfBlackAndWhite() throws Exception {
        ReceiptLogo.Bitmap bitmap = ReceiptLogo.toBitmap(ReceiptLogo.normalize(PrintRendererTests.png(200, 100, 0x808080)), 576, 50);

        long black = 0;
        for (byte b : bitmap.packed()) black += Integer.bitCount(b & 0xFF);
        double share = (double) black / (bitmap.widthDots() * bitmap.heightDots());
        assertThat(share).isBetween(0.35, 0.65);
    }

    @Test
    void notAnImage_isABadRequest() {
        assertThatThrownBy(() -> ReceiptLogo.normalize(new byte[] {1, 2, 3})).isInstanceOf(BadRequestException.class);
    }
}
