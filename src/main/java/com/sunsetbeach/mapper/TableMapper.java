package com.sunsetbeach.mapper;

import com.sunsetbeach.entity.TableEntity;
import com.sunsetbeach.model.Table;
import com.sunsetbeach.model.TableInput;
import org.springframework.stereotype.Component;

@Component
public class TableMapper {

    public Table toDto(TableEntity entity) {
        return new Table(entity.getId(), entity.getZone(), entity.getLabel(), entity.getCapacity(), entity.isActive());
    }

    /** TableInput is a full replacement on both create and update - applies every field. */
    public void applyInput(TableEntity entity, TableInput input) {
        entity.setZone(input.getZone());
        entity.setLabel(input.getLabel().trim());
        entity.setCapacity(input.getCapacity());
        entity.setActive(input.getIsActive() != null ? input.getIsActive() : true);
    }
}
