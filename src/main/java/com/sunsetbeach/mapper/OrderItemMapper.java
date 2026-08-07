package com.sunsetbeach.mapper;

import com.sunsetbeach.entity.OrderItemEntity;
import com.sunsetbeach.model.OrderItem;
import org.springframework.stereotype.Component;

@Component
public class OrderItemMapper {

    public OrderItem toDto(OrderItemEntity entity) {
        return new OrderItem(
                entity.getId(),
                entity.getOrderId(),
                entity.getMenuItemId(),
                entity.getQuantity(),
                PriceFormat.asDecimalString(entity.getUnitPrice()),
                entity.getNote(),
                TimestampFormat.toUtc(entity.getCreatedAt()));
    }
}
