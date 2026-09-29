package com.sunsetbeach.service;

import com.sunsetbeach.entity.VatSettingsEntity;
import com.sunsetbeach.error.ValidationException;
import com.sunsetbeach.mapper.PriceFormat;
import com.sunsetbeach.mapper.TimestampFormat;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.VatSettings;
import com.sunsetbeach.model.VatSettingsUpdateInput;
import com.sunsetbeach.repository.VatSettingsRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code GET/PUT /settings/vat} - the one row V118 seeds (see {@link VatSettingsEntity}).
 * ADMIN-only at the route level (SecurityConfig), like every {@code /settings/**} route. Read by
 * {@link ReportService#revenueStatistic} on every request, never cached, so a change applies to
 * the next report without a restart.
 */
@Service
public class VatSettingsService {

    private final VatSettingsRepository repository;
    private final AuditLogService auditLogService;

    public VatSettingsService(VatSettingsRepository repository, AuditLogService auditLogService) {
        this.repository = repository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public VatSettings get() {
        return toDto(getEntity());
    }

    /** The current rate, percent - e.g. 7.00. */
    @Transactional(readOnly = true)
    public BigDecimal currentRate() {
        return getEntity().getVatRate();
    }

    /** The row always exists - V118 seeds it and nothing deletes it - so a missing one is a broken database, not a 404. */
    private VatSettingsEntity getEntity() {
        return repository.findById(VatSettingsEntity.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException("VatSettings row is missing - V118 seeds it"));
    }

    @Transactional
    public VatSettings update(VatSettingsUpdateInput input) {
        BigDecimal rate = input.getVatRate();
        // numeric(5,2) would silently round a third decimal; refuse it instead of storing a
        // different rate than the one the admin typed.
        if (rate.stripTrailingZeros().scale() > 2) {
            throw ValidationException.field("vatRate", "At most two decimals");
        }
        rate = rate.setScale(2, RoundingMode.UNNECESSARY);

        VatSettingsEntity entity = getEntity();
        BigDecimal previous = entity.getVatRate();
        entity.setVatRate(rate);
        VatSettingsEntity saved = repository.saveAndFlush(entity);

        // An unchanged PUT records nothing - same as LifecycleEmailSettingsService#update.
        if (previous.compareTo(rate) != 0) {
            auditLogService.record(
                    AuditAction.VAT_SETTINGS_UPDATED,
                    AuditEntityType.SETTINGS,
                    null,
                    "VAT rate " + PriceFormat.asDecimalString(previous) + "% -> " + PriceFormat.asDecimalString(rate) + "%");
        }
        return toDto(saved);
    }

    private static VatSettings toDto(VatSettingsEntity entity) {
        return new VatSettings(PriceFormat.asDecimalString(entity.getVatRate()), TimestampFormat.toUtc(entity.getUpdatedAt()));
    }
}
