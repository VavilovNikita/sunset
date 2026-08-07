package com.sunsetbeach.service;

import com.sunsetbeach.entity.TableEntity;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.mapper.TableMapper;
import com.sunsetbeach.model.OrderStatus;
import com.sunsetbeach.model.Table;
import com.sunsetbeach.model.TableInput;
import com.sunsetbeach.repository.OrderRepository;
import com.sunsetbeach.repository.TableRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TableService {

    private static final List<OrderStatus> OPEN_STATUSES = List.of(OrderStatus.OPEN, OrderStatus.SENT);

    private final TableRepository tableRepository;
    private final OrderRepository orderRepository;
    private final TableMapper tableMapper;

    public TableService(TableRepository tableRepository, OrderRepository orderRepository, TableMapper tableMapper) {
        this.tableRepository = tableRepository;
        this.orderRepository = orderRepository;
        this.tableMapper = tableMapper;
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
