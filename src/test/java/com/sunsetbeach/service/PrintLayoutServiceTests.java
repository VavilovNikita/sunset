package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.AbstractIntegrationTest;
import com.sunsetbeach.entity.AuditLogEntity;
import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.error.ValidationException;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.PrintAttemptResult;
import com.sunsetbeach.model.PrintBodyFont;
import com.sunsetbeach.model.PrintLayout;
import com.sunsetbeach.model.PrintLayoutDocument;
import com.sunsetbeach.model.PrintLayoutInput;
import com.sunsetbeach.model.PrintLayoutPreview;
import com.sunsetbeach.model.PrintLayouts;
import com.sunsetbeach.model.PrintPaperWidth;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.repository.AuditLogRepository;
import com.sunsetbeach.security.StaffPrincipal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.transaction.annotation.Transactional;

/** Same shape as {@link VatSettingsServiceTests}: the transaction rolls the rows back, the REQUIRES_NEW audit rows are removed afterwards. */
@SpringBootTest
@Transactional
class PrintLayoutServiceTests extends AbstractIntegrationTest {

    private static final String ACTOR_EMAIL = "print-layout-test@example.com";

    @Autowired
    private PrintLayoutService service;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @BeforeEach
    void authenticateAsAdmin() {
        StaffPrincipal principal = new StaffPrincipal("admin-actor", ACTOR_EMAIL, Role.ADMIN);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @AfterTransaction
    void deleteAuditRows() {
        auditLogRepository.deleteAll(auditRows());
    }

    private List<AuditLogEntity> auditRows() {
        return auditLogRepository.findAll().stream()
                .filter(e -> e.getAction() == AuditAction.RECEIPT_SETTINGS_UPDATED || e.getAction() == AuditAction.RECEIPT_LOGO_CHANGED)
                .filter(e -> ACTOR_EMAIL.equals(e.getActorEmail()))
                .toList();
    }

    private PrintLayout layout(PrintLayoutDocument doc) {
        return service.list().getLayouts().stream().filter(l -> l.getDocumentType() == doc).findFirst().orElseThrow();
    }

    private PrintLayoutInput current(PrintLayoutDocument doc) {
        PrintLayout s = layout(doc);
        return new PrintLayoutInput(s.getPaperWidth(), s.getBodyFont(), s.getTitle(), s.getTitleSize(), s.getHeaderText(),
                s.getHeaderBold(), s.getFooterText(), s.getShowLogo(), s.getLogoWidthPercent(), s.getShowTime(), s.getItemSize(),
                s.getShowLocation(), s.getLocationSize(), s.getShowOrderNumber(), s.getShowWaiter(), s.getShowItemNotes(),
                s.getShowVatLine(), s.getShowPaymentMethod(), s.getTotalSize(), s.getTotalBold());
    }

    @Test
    void seededRows_areEachDocumentAsItAlwaysPrinted() {
        PrintLayouts all = service.list();

        assertThat(all.getHasLogo()).isFalse();
        assertThat(all.getLayouts()).extracting(PrintLayout::getDocumentType)
                .containsExactly(PrintLayoutDocument.RECEIPT, PrintLayoutDocument.PREBILL, PrintLayoutDocument.KITCHEN_TICKET,
                        PrintLayoutDocument.BAR_TICKET, PrintLayoutDocument.Z_REPORT);
        assertThat(all.getLayouts()).extracting(PrintLayout::getTitle)
                .containsExactly("GUEST RECEIPT", "PRE-BILL", "KITCHEN TICKET", "BAR TICKET", "Z-REPORT");
        assertThat(all.getLayouts()).allSatisfy(l -> {
            assertThat(l.getPaperWidth()).isEqualTo(PrintPaperWidth.MM80);
            assertThat(l.getBodyFont()).isEqualTo(PrintBodyFont.A);
            assertThat(l.getHeaderText()).isEmpty();
            assertThat(l.getFooterText()).isEmpty();
            assertThat(l.getShowLogo()).isFalse();
            assertThat(l.getItemSize()).isEqualTo(1);
            assertThat(l.getTitleSize()).isEqualTo(1);
        });
    }

    @Test
    void update_changesOnlyThatDocument_storesNormalisedText_andAuditsDocumentAndFieldNames() {
        PrintLayoutInput in = current(PrintLayoutDocument.KITCHEN_TICKET)
                .title("  KITCHEN  ").headerText("Line 1\r\n\r\n  Line 2  \n").bodyFont(PrintBodyFont.B).itemSize(2);

        PrintLayout saved = service.update(PrintLayoutDocument.KITCHEN_TICKET, in);

        assertThat(saved.getDocumentType()).isEqualTo(PrintLayoutDocument.KITCHEN_TICKET);
        assertThat(saved.getTitle()).isEqualTo("KITCHEN");
        assertThat(saved.getHeaderText()).isEqualTo("Line 1\nLine 2");
        assertThat(layout(PrintLayoutDocument.BAR_TICKET).getTitle()).isEqualTo("BAR TICKET");
        assertThat(layout(PrintLayoutDocument.BAR_TICKET).getItemSize()).isEqualTo(1);
        assertThat(auditRows()).singleElement().satisfies(row -> {
            assertThat(row.getAction()).isEqualTo(AuditAction.RECEIPT_SETTINGS_UPDATED);
            assertThat(row.getSummary()).contains("KITCHEN_TICKET", "title", "headerText", "bodyFont", "itemSize")
                    .doesNotContain("Line 1", "footerText", "paperWidth");
        });
    }

    @Test
    void update_withNothingChanged_recordsNoAuditRow() {
        service.update(PrintLayoutDocument.RECEIPT, current(PrintLayoutDocument.RECEIPT));

        assertThat(auditRows()).isEmpty();
    }

    @Test
    void update_rejectsBadValues_andStoresNothing() {
        PrintLayoutDocument d = PrintLayoutDocument.RECEIPT;
        assertThatThrownBy(() -> service.update(d, current(d).title("   "))).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> service.update(d, current(d).title("X".repeat(25)))).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> service.update(d, current(d).title("A\nB"))).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> service.update(d, current(d).headerText("1\n2\n3\n4\n5\n6\n7"))).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> service.update(d, current(d).footerText("X".repeat(49)))).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> service.update(d, current(d).logoWidthPercent(10))).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> service.update(d, current(d).logoWidthPercent(101))).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> service.update(d, current(d).itemSize(3))).isInstanceOf(ValidationException.class);

        assertThat(layout(d).getTitle()).isEqualTo("GUEST RECEIPT");
        assertThat(auditRows()).isEmpty();
    }

    @Test
    void preview_rendersTheUnsavedLayoutOfThatDocument_andSavesNothing() {
        PrintLayoutPreview kitchen = service.preview(PrintLayoutDocument.KITCHEN_TICKET,
                current(PrintLayoutDocument.KITCHEN_TICKET).paperWidth(PrintPaperWidth.MM58).title("HELLO"));

        assertThat(kitchen.getColumns()).isEqualTo(32);
        assertThat(kitchen.getLines().get(0).getText()).isEqualTo("HELLO");
        assertThat(kitchen.getLines().stream().map(l -> l.getText())).anyMatch(t -> t.contains("Pad Thai"))
                .anyMatch(t -> t.trim().startsWith("note: no peanuts")).anyMatch(t -> t.contains("sample, not a real order"));
        assertThat(layout(PrintLayoutDocument.KITCHEN_TICKET).getTitle()).isEqualTo("KITCHEN TICKET");

        PrintLayoutPreview receipt = service.preview(PrintLayoutDocument.RECEIPT, current(PrintLayoutDocument.RECEIPT));
        assertThat(receipt.getLines().stream().map(l -> l.getText())).anyMatch(t -> t.startsWith("TOTAL") && t.endsWith("410.00"))
                .anyMatch(t -> t.startsWith("Payment: Cash"));
        PrintLayoutPreview prebill = service.preview(PrintLayoutDocument.PREBILL, current(PrintLayoutDocument.PREBILL));
        assertThat(prebill.getLines().stream().map(l -> l.getText())).noneMatch(t -> t.startsWith("Payment"));

        PrintLayoutPreview z = service.preview(PrintLayoutDocument.Z_REPORT, current(PrintLayoutDocument.Z_REPORT));
        assertThat(z.getLines().stream().map(l -> l.getText())).anyMatch(t -> t.startsWith("Discrepancy")).anyMatch(t -> t.startsWith("Folio cash"));
    }

    @Test
    void logo_isSharedAndGuestFacingOnly_uploadShowPreviewRemove() throws Exception {
        byte[] png = PrintRendererTests.png(300, 120, 0x000000);

        assertThat(service.setLogo(new MockMultipartFile("file", "logo.png", "image/png", png)).getHasLogo()).isTrue();
        // Uploading doesn't turn it on...
        assertThat(layout(PrintLayoutDocument.RECEIPT).getShowLogo()).isFalse();
        assertThat(service.preview(PrintLayoutDocument.RECEIPT, current(PrintLayoutDocument.RECEIPT)).getLogoPngBase64()).isNull();
        // ...showing it does - on the receipt, and not on a ticket even if asked.
        service.update(PrintLayoutDocument.RECEIPT, current(PrintLayoutDocument.RECEIPT).showLogo(true).logoWidthPercent(50));
        service.update(PrintLayoutDocument.KITCHEN_TICKET, current(PrintLayoutDocument.KITCHEN_TICKET).showLogo(true));
        PrintLayoutPreview receipt = service.preview(PrintLayoutDocument.RECEIPT, current(PrintLayoutDocument.RECEIPT));
        assertThat(receipt.getLogoPngBase64()).isNotBlank();
        assertThat(receipt.getLogoWidthDots()).isEqualTo(288);
        assertThat(service.preview(PrintLayoutDocument.KITCHEN_TICKET, current(PrintLayoutDocument.KITCHEN_TICKET)).getLogoPngBase64()).isNull();

        PrintLayouts removed = service.removeLogo();
        assertThat(removed.getHasLogo()).isFalse();
        // A missing logo can't stay "shown" anywhere.
        assertThat(removed.getLayouts()).allSatisfy(l -> assertThat(l.getShowLogo()).isFalse());
        assertThat(auditRows().stream().filter(r -> r.getAction() == AuditAction.RECEIPT_LOGO_CHANGED)).hasSize(2);
        // Removing again is a no-op, not an error and not a second audit row.
        service.removeLogo();
        assertThat(auditRows().stream().filter(r -> r.getAction() == AuditAction.RECEIPT_LOGO_CHANGED)).hasSize(2);
    }

    @Test
    void logo_rejectsNonImages_andWebP() {
        assertThatThrownBy(() -> service.setLogo(new MockMultipartFile("file", "x.png", "image/png", "not an image".getBytes())))
                .isInstanceOf(BadRequestException.class);
        byte[] webp = new byte[] {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P', 'V', 'P', '8', ' ', 0, 0, 0, 0};
        assertThatThrownBy(() -> service.setLogo(new MockMultipartFile("file", "x.webp", "image/webp", webp)))
                .isInstanceOf(BadRequestException.class);
        assertThat(service.list().getHasLogo()).isFalse();
    }

    @Test
    void testPrint_withoutAPrinterForThatDepartment_saysNothingWasAttempted() {
        for (PrintLayoutDocument doc : PrintLayoutDocument.values()) {
            PrintAttemptResult result = service.testPrint(doc);
            // The test database has no printers configured unless another test left some - @Transactional rolls those back.
            assertThat(result.getAttempted()).as(doc.getValue()).isFalse();
        }
    }
}
