package com.sunsetbeach.service;

import com.sunsetbeach.entity.PaymentEntity;
import com.sunsetbeach.entity.ShiftEntity;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.mapper.ShiftMapper;
import com.sunsetbeach.model.OrderStatus;
import com.sunsetbeach.model.PaymentMethod;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.Shift;
import com.sunsetbeach.model.ShiftCloseInput;
import com.sunsetbeach.model.ShiftOpenInput;
import com.sunsetbeach.model.ShiftStatus;
import com.sunsetbeach.model.ShiftSummary;
import com.sunsetbeach.model.ShiftTotals;
import com.sunsetbeach.repository.OrderRepository;
import com.sunsetbeach.repository.PaymentRepository;
import com.sunsetbeach.repository.ShiftRepository;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShiftService {

    private static final List<OrderStatus> UNSETTLED_STATUSES = List.of(OrderStatus.OPEN, OrderStatus.SENT);

    private final ShiftRepository shiftRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ShiftMapper shiftMapper;

    public ShiftService(
            ShiftRepository shiftRepository,
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            ShiftMapper shiftMapper) {
        this.shiftRepository = shiftRepository;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.shiftMapper = shiftMapper;
    }

    @Transactional(readOnly = true)
    public Shift getCurrentOpenShift(String userId) {
        ShiftEntity shift = shiftRepository
                .findByOpenedByUserIdAndStatus(userId, ShiftStatus.OPEN)
                .orElseThrow(() -> new NotFoundException("No open shift"));
        return shiftMapper.toDto(shift);
    }

    @Transactional
    public Shift open(String userId, ShiftOpenInput input) {
        if (shiftRepository.findByOpenedByUserIdAndStatus(userId, ShiftStatus.OPEN).isPresent()) {
            throw new ConflictException("You already have an open shift");
        }

        ShiftEntity entity = new ShiftEntity();
        entity.setOpenedByUserId(userId);
        entity.setOpeningCashFloat(input.getOpeningCashFloat());
        try {
            return shiftMapper.toDto(shiftRepository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException e) {
            // Belt-and-suspenders: the unique partial index (Shift_one_open_per_user) is the
            // real guard against a race between two concurrent opens for the same user.
            throw new ConflictException("You already have an open shift");
        }
    }

    @Transactional
    public Shift close(String id, ShiftCloseInput input) {
        ShiftEntity shift = shiftRepository.findById(id).orElseThrow(() -> new NotFoundException("Shift not found"));
        if (shift.getStatus() == ShiftStatus.CLOSED) {
            throw new ConflictException("Shift is already closed");
        }
        // Orders don't carry a shiftId (only Payments do), so there's no way to scope this
        // check to "orders opened during this shift" - it's a system-wide gate: don't let the
        // register close while there's still unpaid business anywhere in the POS.
        if (orderRepository.existsByStatusIn(UNSETTLED_STATUSES)) {
            throw new ConflictException("There are still OPEN/SENT orders that need to be closed or cancelled first");
        }

        shift.setClosedByUserId(shift.getOpenedByUserId());
        shift.setClosedAt(LocalDateTime.now());
        shift.setClosingCashCounted(input.getClosingCashCounted());
        shift.setNotes(input.getNotes().orElse(null));
        shift.setStatus(ShiftStatus.CLOSED);
        return shiftMapper.toDto(shiftRepository.saveAndFlush(shift));
    }

    @Transactional(readOnly = true)
    public ShiftSummary getSummary(String id, String callerId, Role callerRole) {
        ShiftEntity shift = shiftRepository.findById(id).orElseThrow(() -> new NotFoundException("Shift not found"));
        // A plain CASHIER may only look up their own shift - MANAGER/ADMIN can look up any.
        // 404 (not 403) so a cashier can't even confirm another cashier's shift id exists.
        if (callerRole == Role.CASHIER && !shift.getOpenedByUserId().equals(callerId)) {
            throw new NotFoundException("Shift not found");
        }
        return shiftMapper.toSummaryDto(shift, computeTotals(id));
    }

    @Transactional(readOnly = true)
    public String exportCsv(String id) {
        if (!shiftRepository.existsById(id)) {
            throw new NotFoundException("Shift not found");
        }
        List<PaymentEntity> payments = paymentRepository.findByShiftId(id);
        String[] header = {"ID", "Order", "Method", "Amount", "Booking", "Recorded by", "Created at"};
        StringBuilder csv = new StringBuilder();
        appendRow(csv, header);
        for (PaymentEntity p : payments) {
            appendRow(
                    csv,
                    new String[] {
                        p.getId(),
                        p.getOrderId(),
                        p.getMethod().getValue(),
                        p.getAmount().setScale(2, RoundingMode.UNNECESSARY).toPlainString(),
                        p.getBookingId() != null ? p.getBookingId() : "",
                        p.getRecordedByUserId(),
                        DateRangeUtil.formatIsoInstant(p.getCreatedAt())
                    });
        }
        return csv.toString();
    }

    private ShiftTotals computeTotals(String shiftId) {
        return PaymentAggregation.toShiftTotals(PaymentAggregation.aggregate(paymentRepository.findByShiftId(shiftId)));
    }

    private static void appendRow(StringBuilder csv, String[] fields) {
        if (!csv.isEmpty()) {
            csv.append("\r\n");
        }
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                csv.append(',');
            }
            csv.append(csvEscape(fields[i]));
        }
    }

    private static String csvEscape(String value) {
        if (value.indexOf(',') >= 0 || value.indexOf('"') >= 0 || value.indexOf('\n') >= 0) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
        return value;
    }
}
