package com.sunsetbeach.mapper;

import com.sunsetbeach.entity.PrinterEntity;
import com.sunsetbeach.model.Printer;
import com.sunsetbeach.model.PrinterCodepage;
import com.sunsetbeach.model.PrinterInput;
import org.springframework.stereotype.Component;

@Component
public class PrinterMapper {

    public Printer toDto(PrinterEntity entity) {
        return new Printer(
                entity.getId(),
                entity.getName(),
                entity.getDepartment(),
                entity.getHost(),
                entity.getPort(),
                entity.getCodepage(),
                entity.isActive(),
                TimestampFormat.toUtc(entity.getCreatedAt()));
    }

    /** PrinterInput is a full replacement on both create and update - applies every field. */
    public void applyInput(PrinterEntity entity, PrinterInput input) {
        entity.setName(input.getName().trim());
        entity.setDepartment(input.getDepartment());
        entity.setHost(input.getHost().trim());
        entity.setPort(input.getPort() != null ? input.getPort() : 9100);
        entity.setCodepage(input.getCodepage() != null ? input.getCodepage() : PrinterCodepage.PC437);
        entity.setActive(input.getIsActive() != null ? input.getIsActive() : true);
    }
}
