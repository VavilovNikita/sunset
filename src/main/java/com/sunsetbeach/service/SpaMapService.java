package com.sunsetbeach.service;

import com.sunsetbeach.entity.SpaMapEntity;
import com.sunsetbeach.entity.TableEntity;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.mapper.TimestampFormat;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.SpaAppointment;
import com.sunsetbeach.model.SpaAppointmentStatus;
import com.sunsetbeach.model.SpaAppointmentTreatment;
import com.sunsetbeach.model.SpaMap;
import com.sunsetbeach.model.SpaMapTable;
import com.sunsetbeach.model.SpaMapTableAppointment;
import com.sunsetbeach.model.SpaSchedule;
import com.sunsetbeach.model.Zone;
import com.sunsetbeach.repository.SpaMapRepository;
import com.sunsetbeach.repository.TableRepository;
import com.sunsetbeach.security.StaffPrincipal;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * The spa's own floor-plan background image, plus its tables' today-state - see {@link SpaMap}'s
 * own openapi.yaml description for why the image is separate from the property map's, not a
 * reuse of it. The image upload/serve mechanism mirrors {@link PropertyMapService}'s exactly
 * ({@link ImageUploadValidator}, the same staff-only (not {@code /uploads/**}) serving path, the
 * same replace-and-best-effort-delete-the-old-file behaviour) - deliberately not a shared
 * component between the two, since the two images have nothing else in common and forcing a
 * shared abstraction over just the upload/serve plumbing would cost more than it saves for two
 * call sites this small.
 * <p>
 * The table enrichment ({@code busy}/{@code nextAppointmentStartTime}/{@code appointments} on
 * {@link SpaMapTable}) is the same kind {@link PropertyMapService} already does for a room unit,
 * computed the same way: reuse an existing batched read ({@link SpaAppointmentService#getSchedule})
 * rather than querying per table. One call for today's whole schedule, then grouped by table in
 * memory - the cost stays flat regardless of how many tables or appointments exist.
 */
@Service
public class SpaMapService {

    // Single row - see V47__spa_map.sql.
    private static final String SINGLETON_ID = "default";

    // The two statuses that still hold their slot - see V43__spa_appointment_completed_still_
    // occupies_slot.sql and the two EXCLUDE USING gist constraints this mirrors.
    private static final Set<SpaAppointmentStatus> OCCUPYING_STATUSES = Set.of(SpaAppointmentStatus.BOOKED, SpaAppointmentStatus.COMPLETED);

    private final SpaMapRepository spaMapRepository;
    private final TableRepository tableRepository;
    private final SpaAppointmentService spaAppointmentService;
    private final ImageUploadValidator imageUploadValidator;
    private final AuditLogService auditLogService;
    private final Path uploadsRoot;
    private final Clock clock;

    public SpaMapService(
            SpaMapRepository spaMapRepository,
            TableRepository tableRepository,
            SpaAppointmentService spaAppointmentService,
            ImageUploadValidator imageUploadValidator,
            AuditLogService auditLogService,
            @Value("${app.uploads.root}") String uploadsRoot,
            Clock clock) {
        this.spaMapRepository = spaMapRepository;
        this.tableRepository = tableRepository;
        this.spaAppointmentService = spaAppointmentService;
        this.imageUploadValidator = imageUploadValidator;
        this.auditLogService = auditLogService;
        this.uploadsRoot = Path.of(uploadsRoot);
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public SpaMap get() {
        List<TableEntity> spaTables = tableRepository.findByZone(Zone.SPA);

        LocalDate today = LocalDate.now(clock);
        LocalTime now = LocalTime.now(clock);
        SpaSchedule schedule = spaAppointmentService.getSchedule(today);
        Map<String, List<SpaAppointment>> appointmentsByTableId =
                schedule.getAppointments().stream().collect(Collectors.groupingBy(SpaAppointment::getTableId));

        List<SpaMapTable> tableDtos = spaTables.stream()
                .map(t -> toMapTableDto(t, appointmentsByTableId.getOrDefault(t.getId(), List.of()), now))
                .toList();

        SpaMapEntity map = spaMapRepository.findById(SINGLETON_ID).orElse(null);
        String imagePath = map != null ? map.getImagePath() : null;
        return new SpaMap(imagePath, map != null ? TimestampFormat.toUtc(map.getUpdatedAt()) : null, tableDtos);
    }

    private static SpaMapTable toMapTableDto(TableEntity table, List<SpaAppointment> appointments, LocalTime now) {
        boolean busy = appointments.stream().anyMatch(a -> isOccupyingNow(a, now));

        String nextAppointmentStartTime = appointments.stream()
                .filter(a -> a.getStatus() == SpaAppointmentStatus.BOOKED)
                .map(SpaAppointment::getStartTime)
                .filter(startTime -> LocalTime.parse(startTime).isAfter(now))
                .min(String::compareTo) // safe on zero-padded HH:mm - lexicographic order matches chronological order
                .orElse(null);

        List<SpaMapTableAppointment> appointmentDtos = appointments.stream()
                .map(a -> new SpaMapTableAppointment(
                        a.getId(),
                        a.getStartTime(),
                        a.getDurationMinutes(),
                        a.getStatus(),
                        a.getGuestName(),
                        a.getTreatments().stream().map(SpaAppointmentTreatment::getTreatmentName).toList()))
                .toList();

        return new SpaMapTable(
                table.getId(),
                table.getLabel(),
                table.getCapacity(),
                table.isActive(),
                table.getPositionX(),
                table.getPositionY(),
                busy,
                nextAppointmentStartTime,
                appointmentDtos);
    }

    private static boolean isOccupyingNow(SpaAppointment appointment, LocalTime now) {
        if (!OCCUPYING_STATUSES.contains(appointment.getStatus())) {
            return false;
        }
        LocalTime start = LocalTime.parse(appointment.getStartTime());
        LocalTime end = start.plusMinutes(appointment.getDurationMinutes());
        return !now.isBefore(start) && now.isBefore(end);
    }

    @Transactional
    public SpaMap uploadImage(MultipartFile file) {
        ImageUploadValidator.ValidatedImage validated = imageUploadValidator.validate(file);

        Path dir = uploadsRoot.resolve("spa-map");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create upload directory", e);
        }
        String filename = imageUploadValidator.randomFilename(validated.extension());
        try {
            Files.write(dir.resolve(filename), validated.bytes());
        } catch (IOException e) {
            throw new IllegalStateException("Could not write uploaded file", e);
        }

        SpaMapEntity entity = spaMapRepository.findById(SINGLETON_ID).orElseGet(SpaMapEntity::new);
        entity.setId(SINGLETON_ID);
        String oldFilename = entity.getImagePath();
        StaffPrincipal actor = (StaffPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        entity.setImagePath(filename);
        entity.setUpdatedByUserId(actor.id());
        spaMapRepository.saveAndFlush(entity);

        // Best-effort: replacing the plan must never fail because the old file couldn't be
        // removed - matches PropertyMapService#uploadImage/RoomService.deleteImage's own
        // best-effort disk cleanup.
        if (oldFilename != null) {
            try {
                Files.deleteIfExists(dir.resolve(oldFilename));
            } catch (IOException ignored) {
                // best-effort
            }
        }

        auditLogService.record(AuditAction.SPA_MAP_IMAGE_UPDATED, AuditEntityType.SPA_MAP, SINGLETON_ID, "Spa map background image replaced");

        return get();
    }

    @Transactional(readOnly = true)
    public Resource resolveImage() {
        SpaMapEntity entity = spaMapRepository.findById(SINGLETON_ID).orElseThrow(() -> new NotFoundException("No spa map image has been uploaded yet"));

        Path dir = uploadsRoot.resolve("spa-map").toAbsolutePath().normalize();
        Path file = dir.resolve(entity.getImagePath()).normalize();
        if (!file.startsWith(dir) || !Files.isRegularFile(file)) {
            throw new NotFoundException("No spa map image has been uploaded yet");
        }

        try {
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new NotFoundException("No spa map image has been uploaded yet");
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new NotFoundException("No spa map image has been uploaded yet");
        }
    }
}
