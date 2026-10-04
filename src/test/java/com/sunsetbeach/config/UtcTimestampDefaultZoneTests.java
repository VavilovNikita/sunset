package com.sunsetbeach.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.sunsetbeach.AbstractIntegrationTest;
import com.sunsetbeach.entity.AuditLogEntity;
import com.sunsetbeach.entity.GuestEntity;
import com.sunsetbeach.mapper.TimestampFormat;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.repository.AuditLogRepository;
import com.sunsetbeach.repository.GuestRepository;
import com.sunsetbeach.service.AuditLogService;
import jakarta.persistence.Entity;
import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

/**
 * Every stored timestamp is UTC wall-clock, whatever zone the JVM was started in. A backend
 * started on a Europe/Moscow machine used to write every new {@code createdAt}/{@code updatedAt}
 * three hours ahead - Hibernate's own {@code @CreationTimestamp} reads the JVM default zone - while
 * the whole suite stayed green, because CI and production both run in UTC. This runs the real
 * write paths with the default zone switched to Moscow and checks the stored value is still UTC.
 *
 * <p>The one deliberate exception to "tests never read the wall clock": the thing under test is
 * the real clock's zone handling, so the expected value is {@link Instant#now()} - an instant, not
 * a date, so nothing here depends on what time of day the suite runs at.
 *
 * <p>Switching the zone mid-run only reproduces the old bug on a machine whose zone was already
 * non-UTC at startup: Hibernate captures {@code Clock.systemDefaultZone()} in static fields when
 * its {@code ClockHelper} loads. So the structural check below is what stops a regression on a
 * UTC CI box - no entity may use Hibernate's own timestamp annotations.
 */
@SpringBootTest
class UtcTimestampDefaultZoneTests extends AbstractIntegrationTest {

    private static final Duration TOLERANCE = Duration.ofMinutes(1);

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private GuestRepository guestRepository;

    @Autowired
    private Clock clock;

    private TimeZone originalDefault;
    private final String marker = "utc-tz-test-" + UUID.randomUUID();
    private String guestId;

    @BeforeEach
    void switchJvmToMoscow() {
        originalDefault = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Moscow"));
    }

    @AfterEach
    void restoreZoneAndCleanUp() {
        TimeZone.setDefault(originalDefault);
        auditLogRepository.deleteAll(auditRows());
        if (guestId != null) {
            guestRepository.deleteById(guestId);
        }
    }

    @Test
    void auditEntryCreatedAtIsUtcUnderNonUtcDefaultZone() {
        auditLogService.recordSystemAction(AuditAction.BOOKING_STATUS_CHANGED, AuditEntityType.BOOKING, marker, "tz test");

        List<AuditLogEntity> rows = auditRows();
        assertThat(rows).hasSize(1);
        assertIsUtcNow(rows.get(0).getCreatedAt());
    }

    @Test
    void creationAndUpdateTimestampsAreUtcUnderNonUtcDefaultZone() {
        GuestEntity guest = new GuestEntity();
        guest.setName(marker);
        guest = guestRepository.saveAndFlush(guest);
        guestId = guest.getId();
        assertIsUtcNow(guestRepository.findById(guestId).orElseThrow().getCreatedAt());

        guest.setVip(true);
        guestRepository.saveAndFlush(guest);
        assertIsUtcNow(guestRepository.findById(guestId).orElseThrow().getUpdatedAt());
    }

    @Test
    void nowUtcIgnoresBothDefaultZoneAndHotelZone() {
        assertIsUtcNow(TimestampFormat.nowUtc(clock));
    }

    @Test
    void noEntityUsesHibernateDefaultZoneTimestampAnnotations() throws Exception {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
        List<String> offenders = new ArrayList<>();
        for (BeanDefinition candidate : scanner.findCandidateComponents("com.sunsetbeach")) {
            for (Field field : Class.forName(candidate.getBeanClassName()).getDeclaredFields()) {
                if (field.isAnnotationPresent(CreationTimestamp.class) || field.isAnnotationPresent(UpdateTimestamp.class)) {
                    offenders.add(candidate.getBeanClassName() + "#" + field.getName());
                }
            }
        }
        assertThat(offenders).as("use @UtcCreationTimestamp/@UtcUpdateTimestamp instead").isEmpty();
    }

    private List<AuditLogEntity> auditRows() {
        return auditLogRepository.findAll().stream().filter(r -> marker.equals(r.getEntityId())).toList();
    }

    private static void assertIsUtcNow(LocalDateTime stored) {
        Instant asUtc = TimestampFormat.toUtc(stored).toInstant();
        assertThat(Duration.between(asUtc, Instant.now()).abs()).isLessThan(TOLERANCE);
    }
}
