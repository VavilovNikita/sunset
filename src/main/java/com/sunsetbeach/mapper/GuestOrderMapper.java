package com.sunsetbeach.mapper;

import com.sunsetbeach.entity.MenuItemEntity;
import com.sunsetbeach.entity.OrderEntity;
import com.sunsetbeach.entity.OrderItemEntity;
import com.sunsetbeach.model.GuestOrderItem;
import com.sunsetbeach.model.GuestOrderView;
import com.sunsetbeach.repository.MenuItemRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Builds the guest-facing {@link GuestOrderView} projection - never the staff {@code Order}
 * shape (see that schema's own openapi.yaml description for what's deliberately left out).
 * Resolves {@code OrderItem.menuItemId} -&gt; name with one batched lookup, same pattern as
 * {@code OrderPrintingService#resolveMenuItems} - a guest's phone has no way to resolve the id
 * itself, so unlike the staff `OrderItem` DTO, this carries the name instead.
 */
@Component
public class GuestOrderMapper {

    private final MenuItemRepository menuItemRepository;

    public GuestOrderMapper(MenuItemRepository menuItemRepository) {
        this.menuItemRepository = menuItemRepository;
    }

    public GuestOrderView toDto(OrderEntity order, List<OrderItemEntity> items, String locationLabel) {
        Map<String, MenuItemEntity> menuItemsById = resolveMenuItems(items);
        List<GuestOrderItem> guestItems = items.stream()
                .map(item -> toGuestItem(item, menuItemsById))
                .toList();
        return new GuestOrderView(order.getId(), order.getStatus(), locationLabel, guestItems, PriceFormat.asDecimalString(order.getTotal()));
    }

    private static GuestOrderItem toGuestItem(OrderItemEntity item, Map<String, MenuItemEntity> menuItemsById) {
        MenuItemEntity menuItem = menuItemsById.get(item.getMenuItemId());
        String name = menuItem != null ? menuItem.getName() : "Unknown item";
        return new GuestOrderItem(name, item.getQuantity(), item.getNote(), PriceFormat.asDecimalString(item.getUnitPrice()));
    }

    private Map<String, MenuItemEntity> resolveMenuItems(List<OrderItemEntity> items) {
        List<String> ids = items.stream().map(OrderItemEntity::getMenuItemId).distinct().toList();
        return menuItemRepository.findAllById(ids).stream().collect(Collectors.toMap(MenuItemEntity::getId, m -> m));
    }
}
