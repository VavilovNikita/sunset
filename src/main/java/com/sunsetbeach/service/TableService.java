package com.sunsetbeach.service;

import com.sunsetbeach.entity.TableEntity;
import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.error.ValidationException;
import com.sunsetbeach.mapper.TableMapper;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.OrderStatus;
import com.sunsetbeach.model.Table;
import com.sunsetbeach.model.TableInput;
import com.sunsetbeach.model.TablePositionInput;
import com.sunsetbeach.repository.OrderRepository;
import com.sunsetbeach.repository.TableRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TableService {

    private static final List<OrderStatus> OPEN_STATUSES = List.of(OrderStatus.OPEN, OrderStatus.SENT);

    private final TableRepository tableRepository;
    private final OrderRepository orderRepository;
    private final TableMapper tableMapper;
    private final AuditLogService auditLogService;

    public TableService(TableRepository tableRepository, OrderRepository orderRepository, TableMapper tableMapper, AuditLogService auditLogService) {
        this.tableRepository = tableRepository;
        this.orderRepository = orderRepository;
        this.tableMapper = tableMapper;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<Table> list() {
        return tableRepository.findAll().stream().map(tableMapper::toDto).toList();
    }

    @Transactional
    public Table create(TableInput input) {
        TableEntity entity = new TableEntity();
        tableMapper.applyInput(entity, input);
        return tableMapper.toDto(tableRepository.saveAndFlush(entity));
    }

    @Transactional
    public Table update(String id, TableInput input) {
        TableEntity entity = tableRepository.findById(id).orElseThrow(() -> new NotFoundException("Table not found"));
        tableMapper.applyInput(entity, input);
        return tableMapper.toDto(tableRepository.save(entity));
    }

    /**
     * Batch, same all-or-nothing shape as {@link RoomUnitService#savePositions} - see that
     * method's own javadoc. Independent feature (the spa's own floor plan), same mechanics.
     */
    @Transactional
    public List<Table> savePositions(List<TablePositionInput> inputs) {
        for (TablePositionInput input : inputs) {
            BigDecimal x = input.getPositionX().orElse(null);
            BigDecimal y = input.getPositionY().orElse(null);
            if ((x == null) != (y == null)) {
                throw ValidationException.field("positionY", "positionX and positionY must both be set or both be null");
            }
            if (outOfRange(x)) {
                throw ValidationException.field("positionX", "positionX must be between 0 and 1");
            }
            if (outOfRange(y)) {
                throw ValidationException.field("positionY", "positionY must be between 0 and 1");
            }
        }

        List<String> ids = inputs.stream().map(TablePositionInput::getTableId).toList();
        Map<String, TableEntity> entitiesById = tableRepository.findAllById(ids).stream().collect(Collectors.toMap(TableEntity::getId, e -> e));
        for (String id : ids) {
            if (!entitiesById.containsKey(id)) {
                throw new BadRequestException("Unknown table: " + id);
            }
        }

        List<TableEntity> saved = new ArrayList<>();
        for (TablePositionInput input : inputs) {
            TableEntity entity = entitiesById.get(input.getTableId());
            BigDecimal newX = input.getPositionX().orElse(null);
            BigDecimal newY = input.getPositionY().orElse(null);
            boolean changed = !positionEquals(entity.getPositionX(), newX) || !positionEquals(entity.getPositionY(), newY);

            entity.setPositionX(newX);
            entity.setPositionY(newY);
            saved.add(tableRepository.save(entity));

            if (changed) {
                auditLogService.record(
                        AuditAction.POS_TABLE_POSITION_UPDATED,
                        AuditEntityType.TABLE,
                        entity.getId(),
                        newX == null ? "Table " + entity.getLabel() + " removed from the spa floor plan" : "Table " + entity.getLabel() + " placed on the spa floor plan");
            }
        }
        tableRepository.flush();
        return saved.stream().map(tableMapper::toDto).toList();
    }

    private static boolean outOfRange(BigDecimal value) {
        return value != null && (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(BigDecimal.ONE) > 0);
    }

    /** Scale-insensitive comparison - "0.5" from a request body and "0.5000" round-tripped through numeric(5,4) are the same position. */
    private static boolean positionEquals(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            return a == b;
        }
        return a.compareTo(b) == 0;
    }

    @Transactional
    public void delete(String id) {
        if (!tableRepository.existsById(id)) {
            throw new NotFoundException("Table not found");
        }
        if (orderRepository.existsByTableIdAndStatusIn(id, OPEN_STATUSES)) {
            throw new ConflictException("This table has open orders and can't be deleted.");
        }
        // Closed/cancelled orders may still reference this table; the FK is ON DELETE SET
        // NULL for exactly that reason, so this can't hit a DataIntegrityViolationException.
        tableRepository.deleteById(id);
        tableRepository.flush();
    }
}
