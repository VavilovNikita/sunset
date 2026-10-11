package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.AbstractIntegrationTest;
import com.sunsetbeach.entity.AuditLogEntity;
import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.error.ValidationException;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.PrintAttemptResult;
import com.sunsetbeach.model.ReceiptBodyFont;
import com.sunsetbeach.model.ReceiptPaperWidth;
import com.sunsetbeach.model.ReceiptPreview;
import com.sunsetbeach.model.ReceiptSettings;
import com.sunsetbeach.model.ReceiptSettingsInput;
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

/** Same shape as {@link VatSettingsServiceTests}: the transaction rolls the row back, the REQUIRES_NEW audit rows are removed afterwards. */
@SpringBootTest
@Transactional
class ReceiptSettingsServiceTests extends AbstractIntegrationTest {

    private static final String ACTOR_EMAIL = "receipt-settings-test@example.com";

    @Autowired
    private ReceiptSettingsService service;

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

    private ReceiptSettingsInput current() {
        ReceiptSettings s = service.get();
        return new ReceiptSettingsInput(s.getPaperWidth(), s.getBodyFont(), s.getReceiptTitle(), s.getPrebillTitle(),
                ReceiptSettingsInput.TitleSizeEnum.fromValue(s.getTitleSize().getValue()),
                ReceiptSettingsInput.TotalSizeEnum.fromValue(s.getTotalSize().getValue()),
                s.getTotalBold(), s.getHeaderText(), s.getHeaderBold(), s.getFooterText(), s.getShowOrderNumber(), s.getShowTime(),
                s.getShowVatLine(), s.getShowPaymentMethod(), s.getShowLogo(), s.getLogoWidthPercent());
    }

    @Test
    void seededRow_isTheReceiptAsItAlwaysPrinted() {
        ReceiptSettings s = service.get();

        assertThat(s.getPaperWidth()).isEqualTo(ReceiptPaperWidth.MM80);
        assertThat(s.getBodyFont()).isEqualTo(ReceiptBodyFont.A);
        assertThat(s.getReceiptTitle()).isEqualTo("GUEST RECEIPT");
        assertThat(s.getPrebillTitle()).isEqualTo("PRE-BILL");
        assertThat(s.getHeaderText()).isEmpty();
        assertThat(s.getShowLogo()).isFalse();
        assertThat(s.getHasLogo()).isFalse();
        assertThat(s.getShowVatLine() && s.getShowOrderNumber() && s.getShowTime() && s.getShowPaymentMethod()).isTrue();
    }

    @Test
    void update_storesNormalisedText_andAuditsTheChangedFieldNamesOnly() {
        ReceiptSettingsInput in = current().receiptTitle("  TAX INVOICE ").headerText("Sunset Beach\r\n\r\n  12 Beach Rd  \n").bodyFont(ReceiptBodyFont.B);

        ReceiptSettings saved = service.update(in);

        assertThat(saved.getReceiptTitle()).isEqualTo("TAX INVOICE");
        assertThat(saved.getHeaderText()).isEqualTo("Sunset Beach\n12 Beach Rd");
        assertThat(saved.getBodyFont()).isEqualTo(ReceiptBodyFont.B);
        assertThat(auditRows()).singleElement().satisfies(row -> {
            assertThat(row.getAction()).isEqualTo(AuditAction.RECEIPT_SETTINGS_UPDATED);
            assertThat(row.getSummary()).contains("receiptTitle", "headerText", "bodyFont").doesNotContain("Sunset Beach", "prebillTitle");
        });
    }

    @Test
    void update_withNothingChanged_recordsNoAuditRow() {
        service.update(current());

        assertThat(auditRows()).isEmpty();
    }

    @Test
    void update_rejectsBadValues_andStoresNothing() {
        assertThatThrownBy(() -> service.update(current().receiptTitle("   "))).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> service.update(current().prebillTitle("X".repeat(25)))).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> service.update(current().receiptTitle("A\nB"))).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> service.update(current().headerText("1\n2\n3\n4\n5\n6\n7"))).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> service.update(current().footerText("X".repeat(49)))).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> service.update(current().logoWidthPercent(10))).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> service.update(current().logoWidthPercent(101))).isInstanceOf(ValidationException.class);

        assertThat(service.get().getReceiptTitle()).isEqualTo("GUEST RECEIPT");
        assertThat(auditRows()).isEmpty();
    }

    @Test
    void preview_rendersTheUnsavedLayout_andSavesNothing() {
        ReceiptPreview preview = service.preview(current().paperWidth(ReceiptPaperWidth.MM58).receiptTitle("HELLO"));

        assertThat(preview.getColumns()).isEqualTo(32);
        assertThat(preview.getLines().get(0).getText()).isEqualTo("HELLO");
        assertThat(preview.getLines().stream().map(l -> l.getText())).anyMatch(t -> t.startsWith("TOTAL") && t.endsWith("410.00"));
        assertThat(preview.getLogoPngBase64()).isNull();
        assertThat(service.get().getReceiptTitle()).isEqualTo("GUEST RECEIPT");
    }

    @Test
    void logo_uploadShowPreviewRemove() throws Exception {
        byte[] png = ReceiptRendererTests.png(300, 120, 0x000000);

        assertThat(service.setLogo(new MockMultipartFile("file", "logo.png", "image/png", png)).getHasLogo()).isTrue();
        // Uploading doesn't turn it on...
        assertThat(service.get().getShowLogo()).isFalse();
        assertThat(service.preview(current()).getLogoPngBase64()).isNull();
        // ...showing it does.
        service.update(current().showLogo(true).logoWidthPercent(50));
        ReceiptPreview preview = service.preview(current().showLogo(true).logoWidthPercent(50));
        assertThat(preview.getLogoPngBase64()).isNotBlank();
        assertThat(preview.getLogoWidthDots()).isEqualTo(288);

        ReceiptSettings removed = service.removeLogo();
        assertThat(removed.getHasLogo()).isFalse();
        assertThat(removed.getShowLogo()).isFalse(); // a missing logo can't stay "shown"
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
        assertThat(service.get().getHasLogo()).isFalse();
    }

    @Test
    void testPrint_withoutACashierPrinter_saysNothingWasAttempted() {
        PrintAttemptResult result = service.testPrint();

        // The test database has no CASHIER printer configured unless another test left one - which @Transactional rolls back.
        assertThat(result.getAttempted()).isFalse();
    }
}
