package com.sunsetbeach.mapper;

import com.sunsetbeach.entity.OrderEntity;
import com.sunsetbeach.entity.OrderItemEntity;
import com.sunsetbeach.entity.OrderItemVoidEntity;
import com.sunsetbeach.entity.PaymentEntity;
import com.sunsetbeach.model.Order;
import com.sunsetbeach.model.OrderItemVoid;
import com.sunsetbeach.model.OrderStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {

    private final OrderItemMapper orderItemMapper;

    public OrderMapper(OrderItemMapper orderItemMapper) {
        this.orderItemMapper = orderItemMapper;
    }

    /**
     * Everything an {@link Order} carries besides the order row and its lines. {@code payment} is
     * the order's one {@code Payment} or {@code null} (it decides {@code paymentMethod} and, for a
     * {@code PAID} order, {@code closedAt}); {@code emailsByUserId} resolves {@code voidedByEmail}.
     */
    public record Extras(
            String openedByEmail,
            PaymentEntity payment,
            List<OrderItemVoidEntity> voids,
            Map<String, String> emailsByUserId,
            String spaAppointmentId) {}

    public Order toDto(OrderEntity entity, List<OrderItemEntity> items, Extras extras) {
        PaymentEntity payment = extras.payment();
        return new Order(
                entity.getId(),
                entity.getNumber(),
                closedAt(entity, payment),
                extras.spaAppointmentId(),
                extras.voids().stream().map(v -> toVoidDto(v, extras.emailsByUserId())).toList(),
                entity.getTableId(),
                entity.getBookingId(),
                entity.getGuestName(),
                entity.getStatus(),
                entity.getOpenedByUserId(),
                extras.openedByEmail(),
                PriceFormat.asDecimalString(entity.getTotal()),
                entity.getNote(),
                items.stream().map(orderItemMapper::toDto).toList(),
                TimestampFormat.toUtc(entity.getCreatedAt()),
                TimestampFormat.toUtc(entity.getUpdatedAt()),
                payment != null ? payment.getMethod() : null,
                entity.getGuestAccessToken());
    }

    /** See {@code Order.closedAt}: the payment's time for PAID, the cancellation (last update) for CANCELLED. */
    private static OffsetDateTime closedAt(OrderEntity entity, PaymentEntity payment) {
        if (entity.getStatus() == OrderStatus.PAID && payment != null) {
            return TimestampFormat.toUtc(payment.getCreatedAt());
        }
        if (entity.getStatus() == OrderStatus.CANCELLED) {
            return TimestampFormat.toUtc(entity.getUpdatedAt());
        }
        return null;
    }

    private static OrderItemVoid toVoidDto(OrderItemVoidEntity v, Map<String, String> emailsByUserId) {
        return new OrderItemVoid(
                v.getId(),
                v.getMenuItemId(),
                v.getQuantity(),
                PriceFormat.asDecimalString(v.getUnitPrice()),
                v.getNote(),
                v.getSentAt() != null ? TimestampFormat.toUtc(v.getSentAt()) : null,
                v.getReason(),
                v.getVoidedByUserId(),
                emailsByUserId.getOrDefault(v.getVoidedByUserId(), v.getVoidedByUserId()),
                TimestampFormat.toUtc(v.getVoidedAt()));
    }
}
