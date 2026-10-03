package com.sunsetbeach.service;

import com.sunsetbeach.entity.OrderEntity;
import com.sunsetbeach.entity.RestaurantMapEntity;
import com.sunsetbeach.entity.TableEntity;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.mapper.TimestampFormat;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.OrderStatus;
import com.sunsetbeach.model.RestaurantMap;
import com.sunsetbeach.model.RestaurantMapTable;
import com.sunsetbeach.model.Zone;
import com.sunsetbeach.repository.OrderRepository;
import com.sunsetbeach.repository.RestaurantMapRepository;
import com.sunsetbeach.repository.TableRepository;
import com.sunsetbeach.security.StaffPrincipal;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
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
 * The restaurant's own floor-plan background image, plus its tables' open orders - the third
 * copy of {@link SpaMapService}'s mechanism (itself a copy of {@link PropertyMapService}'s),
 * deliberately mirrored rather than abstracted for the reason that class's javadoc gives: the
 * shared part is only the upload/serve plumbing, and each map's enrichment is different. Table
 * placement is not copied at all - it's the existing {@code PATCH /tables/positions}, since a
 * table's zone already decides which plan its one position is on (SPA there, everything else
 * here).
 * <p>
 * The enrichment is one query for every open ({@code OPEN}/{@code SENT}) order across all tables,
 * grouped in memory - the same set the POS board treats as "occupied", never a query per table.
 */
@Service
public class RestaurantMapService {

    // Single row - see V124__restaurant_map.sql.
    private static final String SINGLETON_ID = "default";

    private static final Set<OrderStatus> OPEN_STATUSES = Set.of(OrderStatus.OPEN, OrderStatus.SENT);

    private static final String UPLOAD_DIR = "restaurant-map";

    private final RestaurantMapRepository restaurantMapRepository;
    private final TableRepository tableRepository;
    private final OrderRepository orderRepository;
    private final ImageUploadValidator imageUploadValidator;
    private final AuditLogService auditLogService;
    private final Path uploadsRoot;

    public RestaurantMapService(
            RestaurantMapRepository restaurantMapRepository,
            TableRepository tableRepository,
            OrderRepository orderRepository,
            ImageUploadValidator imageUploadValidator,
            AuditLogService auditLogService,
            @Value("${app.uploads.root}") String uploadsRoot) {
        this.restaurantMapRepository = restaurantMapRepository;
        this.tableRepository = tableRepository;
        this.orderRepository = orderRepository;
        this.imageUploadValidator = imageUploadValidator;
        this.auditLogService = auditLogService;
        this.uploadsRoot = Path.of(uploadsRoot);
    }

    @Transactional(readOnly = true)
    public RestaurantMap get() {
        List<TableEntity> tables = tableRepository.findByZoneNot(Zone.SPA);
        Map<String, List<String>> openOrderIdsByTableId = orderRepository.findByTableIdIsNotNullAndStatusIn(OPEN_STATUSES).stream()
                .collect(Collectors.groupingBy(OrderEntity::getTableId, Collectors.mapping(OrderEntity::getId, Collectors.toList())));

        List<RestaurantMapTable> tableDtos = tables.stream()
                .map(t -> new RestaurantMapTable(
                        t.getId(),
                        t.getLabel(),
                        t.getZone(),
                        t.getCapacity(),
                        t.isActive(),
                        t.getPositionX(),
                        t.getPositionY(),
                        openOrderIdsByTableId.getOrDefault(t.getId(), List.of())))
                .toList();

        RestaurantMapEntity map = restaurantMapRepository.findById(SINGLETON_ID).orElse(null);
        return new RestaurantMap(
                map != null ? map.getImagePath() : null, map != null ? TimestampFormat.toUtc(map.getUpdatedAt()) : null, tableDtos);
    }

    @Transactional
    public RestaurantMap uploadImage(MultipartFile file) {
        ImageUploadValidator.ValidatedImage validated = imageUploadValidator.validate(file);

        Path dir = uploadsRoot.resolve(UPLOAD_DIR);
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

        RestaurantMapEntity entity = restaurantMapRepository.findById(SINGLETON_ID).orElseGet(RestaurantMapEntity::new);
        entity.setId(SINGLETON_ID);
        String oldFilename = entity.getImagePath();
        StaffPrincipal actor = (StaffPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        entity.setImagePath(filename);
        entity.setUpdatedByUserId(actor.id());
        restaurantMapRepository.saveAndFlush(entity);

        // Best-effort, same as SpaMapService#uploadImage: replacing the plan must never fail
        // because the old file couldn't be removed.
        if (oldFilename != null) {
            try {
                Files.deleteIfExists(dir.resolve(oldFilename));
            } catch (IOException ignored) {
                // best-effort
            }
        }

        auditLogService.record(
                AuditAction.RESTAURANT_MAP_IMAGE_UPDATED, AuditEntityType.RESTAURANT_MAP, SINGLETON_ID, "Restaurant map background image replaced");

        return get();
    }

    @Transactional(readOnly = true)
    public Resource resolveImage() {
        RestaurantMapEntity entity = restaurantMapRepository.findById(SINGLETON_ID)
                .orElseThrow(() -> new NotFoundException("No restaurant map image has been uploaded yet"));

        Path dir = uploadsRoot.resolve(UPLOAD_DIR).toAbsolutePath().normalize();
        Path file = dir.resolve(entity.getImagePath()).normalize();
        if (!file.startsWith(dir) || !Files.isRegularFile(file)) {
            throw new NotFoundException("No restaurant map image has been uploaded yet");
        }

        try {
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new NotFoundException("No restaurant map image has been uploaded yet");
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new NotFoundException("No restaurant map image has been uploaded yet");
        }
    }
}
