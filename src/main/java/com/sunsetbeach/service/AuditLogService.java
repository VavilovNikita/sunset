package com.sunsetbeach.service;

import com.sunsetbeach.entity.AuditLogEntity;
import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.error.ValidationException;
import com.sunsetbeach.mapper.AuditLogMapper;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.AuditLogEntry;
import com.sunsetbeach.model.AuditLogPage;
import com.sunsetbeach.repository.AuditLogRepository;
import com.sunsetbeach.security.StaffPrincipal;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Append-only, explicit audit trail for significant staff actions - written directly from the
 * service layer at the point of each mutation (see the {@code auditLogService.record(...)} call
 * sites across BookingService/OrderService/ShiftService/UserService/RoomService/PricingService/
 * RoomUnitService), not via Hibernate Envers: Envers' shadow-table-per-entity approach would
 * complicate this project's hand-written, carefully-ordered Flyway migrations for very little
 * benefit here, and an explicit call site lets each action describe itself in the language a
 * hotel manager would actually use, instead of a generic before/after entity diff.
 *
 * <h2>Why a summary sentence, not a snapshot or a field diff</h2>
 * Three shapes were considered for what a row stores:
 * <ul>
 *   <li><b>Full before/after entity snapshots</b> - the most complete option, but the table
 *       grows by roughly 2x the size of every mutated entity forever, most of which (ids,
 *       timestamps, unrelated fields) is never what anyone is trying to find out, and reading it
 *       means a developer-shaped JSON diff, not something a manager can read at a glance.</li>
 *   <li><b>Only the changed fields</b> - more compact than a full snapshot, but still a
 *       structured diff that needs a renderer to turn {@code {"status":{"from":"NEW","to":"PAID"}}}
 *       into a sentence - and by the time that renderer exists, it duplicates knowledge the
 *       service method already had at the exact moment it made the change.</li>
 *   <li><b>A human-readable summary, written where the change happens</b> - what this class
 *       uses. The service method already holds both the old and new values as local variables;
 *       writing "Status changed from NEW to PAID" there costs nothing extra and is exactly what
 *       a manager reading this table wants, not a JSON blob they'd need a developer to
 *       interpret.</li>
 * </ul>
 * The tradeoff, stated plainly: this is not a byte-for-byte reconstructable history of every
 * field on every entity - if a dispute needs the exact prior value of a field a summary didn't
 * mention, that value isn't recoverable from here. In exchange, the table stays small (a short
 * string per action, not a growing snapshot) and every row is readable without tooling. Given the
 * actual ask - who did what, when, chiefly around money and guest data - a precise sentence
 * written by the code that made the change covers that without the storage/readability cost of
 * the other two options.
 *
 * <h2>Growth</h2>
 * A small hotel produces on the order of a few dozen audited actions a day (bookings, POS orders,
 * shift closes) - even at 100/day that's ~36,500 rows/year, each a short row (a UUID, an email,
 * a sentence) - a few MB/year, nowhere near a scale where a single Postgres table with the
 * indexes in migration V16 needs partitioning or archival. What does need to scale is *reading*
 * it: {@code GET /audit-log} is paginated and filtered (actor, action, entity, date range)
 * precisely because "show me everything" stops being a usable page long before the table itself
 * becomes a storage problem.
 *
 * <h2>Never blocks the operation it describes</h2>
 * {@link #record} runs in its own transaction ({@link Propagation#REQUIRES_NEW}) and swallows
 * every exception itself - the same fail-open contract {@code EmailService}/{@code PrintService}
 * use for a send that must never fail the booking/order operation that triggered it. The
 * {@code REQUIRES_NEW} propagation is deliberate, not just the try/catch: a plain
 * {@code @Transactional} (joining the caller's transaction) risks a failure here marking the
 * *whole* transaction rollback-only at the database level, invisible to a try/catch in
 * application code, if Hibernate defers the actual flush past the point this method returns. A
 * separate transaction means a broken audit write can only ever fail its own tiny insert. Every
 * call site places {@code record(...)} as the last step of its method, after the real business
 * write has already succeeded - so the (much smaller, and now-mitigated) inverse risk, an audit
 * row surviving for an action whose surrounding transaction later fails for an unrelated reason,
 * essentially never arises in practice.
 */
@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper auditLogMapper;

    public AuditLogService(AuditLogRepository auditLogRepository, AuditLogMapper auditLogMapper) {
        this.auditLogRepository = auditLogRepository;
        this.auditLogMapper = auditLogMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AuditAction action, AuditEntityType entityType, String entityId, String summary) {
        try {
            StaffPrincipal actor = (StaffPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            AuditLogEntity entry = new AuditLogEntity();
            entry.setActorUserId(actor.id());
            entry.setActorEmail(actor.email());
            entry.setActorRole(actor.role());
            entry.setAction(action);
            entry.setEntityType(entityType);
            entry.setEntityId(entityId);
            entry.setSummary(summary);
            auditLogRepository.save(entry);
        } catch (Exception e) {
            log.error(
                    "Failed to record audit log entry (action={}, entityType={}, entityId={}, summary=\"{}\")",
                    action,
                    entityType,
                    entityId,
                    summary,
                    e);
        }
    }

    @Transactional(readOnly = true)
    public AuditLogPage search(
            String actorEmail,
            AuditAction action,
            AuditEntityType entityType,
            String entityId,
            String from,
            String to,
            Integer page,
            Integer pageSize) {
        if (entityId != null && entityType == null) {
            throw ValidationException.field("entityId", "entityId requires entityType");
        }

        LocalDate fromDate = from != null ? LocalDate.parse(from) : null;
        LocalDate toDate = to != null ? LocalDate.parse(to) : null;
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new BadRequestException("from must be on or before to");
        }

        int pageNumber = page != null ? page : 0;
        int size = pageSize != null ? pageSize : 50;

        Specification<AuditLogEntity> spec = buildSpecification(actorEmail, action, entityType, entityId, fromDate, toDate);
        Page<AuditLogEntity> result =
                auditLogRepository.findAll(spec, PageRequest.of(pageNumber, size, Sort.by(Sort.Direction.DESC, "createdAt")));

        List<AuditLogEntry> items = result.getContent().stream().map(auditLogMapper::toDto).toList();
        return new AuditLogPage(items, pageNumber, size, (int) result.getTotalElements());
    }

    private static Specification<AuditLogEntity> buildSpecification(
            String actorEmail, AuditAction action, AuditEntityType entityType, String entityId, LocalDate from, LocalDate to) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (actorEmail != null && !actorEmail.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("actorEmail")), "%" + actorEmail.toLowerCase(Locale.ROOT) + "%"));
            }
            if (action != null) {
                predicates.add(cb.equal(root.get("action"), action));
            }
            if (entityType != null) {
                predicates.add(cb.equal(root.get("entityType"), entityType));
            }
            if (entityId != null) {
                predicates.add(cb.equal(root.get("entityId"), entityId));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay()));
            }
            if (to != null) {
                predicates.add(cb.lessThan(root.get("createdAt"), to.plusDays(1).atStartOfDay()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
