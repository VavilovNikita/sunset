package com.sunsetbeach.mapper;

import com.sunsetbeach.entity.OrderEntity;
import com.sunsetbeach.entity.OrderItemEntity;
import com.sunsetbeach.model.Order;
import com.sunsetbeach.model.PaymentMethod;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {

    private final OrderItemMapper orderItemMapper;

    public OrderMapper(OrderItemMapper orderItemMapper) {
        this.orderItemMapper = orderItemMapper;
    }

    /** {@code paymentMethod} is {@code null} for anything not (yet) {@code PAID} - see the field's openapi.yaml doc. */
    public Order toDto(OrderEntity entity, List<OrderItemEntity> items, String openedByEmail, PaymentMethod paymentMethod) {
        return new Order(
                entity.getId(),
                entity.getTableId(),
                entity.getBookingId(),
                entity.getGuestName(),
                entity.getStatus(),
                entity.getOpenedByUserId(),
                openedByEmail,
                PriceFormat.asDecimalString(entity.getTotal()),
                entity.getNote(),
                items.stream().map(orderItemMapper::toDto).toList(),
                TimestampFormat.toUtc(entity.getCreatedAt()),
                TimestampFormat.toUtc(entity.getUpdatedAt()),
                paymentMethod);
    }
}
