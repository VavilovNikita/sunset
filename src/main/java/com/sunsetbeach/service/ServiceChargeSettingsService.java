package com.sunsetbeach.service;

import com.sunsetbeach.entity.ServiceChargeSettingsEntity;
import com.sunsetbeach.error.ValidationException;
import com.sunsetbeach.mapper.PriceFormat;
import com.sunsetbeach.mapper.TimestampFormat;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.ServiceChargeSettings;
import com.sunsetbeach.model.ServiceChargeSettingsUpdateInput;
import com.sunsetbeach.repository.ServiceChargeSettingsRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code GET/PUT /settings/service-charge} - the one row V161 seeds (see {@link ServiceChargeSettingsEntity}).
 * ADMIN-only at the route level (SecurityConfig), like every {@code /settings/**} route. Read by
 * {@link BookingTrackerService} on every request, never cached, so a change applies to the next
 * report without a restart. The rate is a deduction for that analysis only - nothing is charged.
 */
@Service
public class ServiceChargeSettingsService {

    private final ServiceChargeSettingsRepository repository;
    private final AuditLogService auditLogService;

    public ServiceChargeSettingsService(ServiceChargeSettingsRepository repository, AuditLogService auditLogService) {
        this.repository = repository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public ServiceChargeSettings get() {
        return toDto(getEntity());
    }

    /** The current rate, percent - e.g. 7.00. */
    @Transactional(readOnly = true)
    public BigDecimal currentRate() {
        return getEntity().getServiceChargeRate();
    }

    /** The row always exists - V161 seeds it and nothing deletes it - so a missing one is a broken database, not a 404. */
    private ServiceChargeSettingsEntity getEntity() {
        return repository.findById(ServiceChargeSettingsEntity.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException("ServiceChargeSettings row is missing - V161 seeds it"));
    }

    @Transactional
    public ServiceChargeSettings update(ServiceChargeSettingsUpdateInput input) {
        BigDecimal rate = input.getServiceChargeRate();
        // numeric(5,2) would silently round a third decimal; refuse it instead of storing a
        // different rate than the one the admin typed.
        if (rate.stripTrailingZeros().scale() > 2) {
            throw ValidationException.field("serviceChargeRate", "At most two decimals");
        }
        rate = rate.setScale(2, RoundingMode.UNNECESSARY);

        ServiceChargeSettingsEntity entity = getEntity();
        BigDecimal previous = entity.getServiceChargeRate();
        entity.setServiceChargeRate(rate);
        ServiceChargeSettingsEntity saved = repository.saveAndFlush(entity);

        // An unchanged PUT records nothing - same as LifecycleEmailSettingsService#update.
        if (previous.compareTo(rate) != 0) {
            auditLogService.record(
                    AuditAction.SERVICE_CHARGE_SETTINGS_UPDATED,
                    AuditEntityType.SETTINGS,
                    null,
                    "Service charge rate " + PriceFormat.asDecimalString(previous) + "% -> " + PriceFormat.asDecimalString(rate) + "%");
        }
        return toDto(saved);
    }

    private static ServiceChargeSettings toDto(ServiceChargeSettingsEntity entity) {
        return new ServiceChargeSettings(PriceFormat.asDecimalString(entity.getServiceChargeRate()), TimestampFormat.toUtc(entity.getUpdatedAt()));
    }
}
