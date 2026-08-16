package com.sunsetbeach.mapper;

import com.sunsetbeach.entity.MenuItemEntity;
import com.sunsetbeach.model.MenuDepartment;
import com.sunsetbeach.model.MenuItem;
import com.sunsetbeach.model.MenuItemInput;
import org.springframework.stereotype.Component;

@Component
public class MenuItemMapper {

    public MenuItem toDto(MenuItemEntity entity) {
        return new MenuItem(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getCategory(),
                entity.getDepartment(),
                PriceFormat.asDecimalString(entity.getPrice()),
                entity.isAvailable(),
                TimestampFormat.toUtc(entity.getCreatedAt()));
    }

    /** MenuItemInput is a full replacement on both create and update - applies every field. */
    public void applyInput(MenuItemEntity entity, MenuItemInput input) {
        entity.setName(input.getName().trim());
        entity.setDescription(input.getDescription().trim());
        entity.setCategory(input.getCategory().trim());
        entity.setDepartment(input.getDepartment() != null ? input.getDepartment() : MenuDepartment.KITCHEN);
        entity.setPrice(input.getPrice());
        entity.setAvailable(input.getIsAvailable() != null ? input.getIsAvailable() : true);
    }
}
