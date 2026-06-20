package com.toolshare.dto.maintenanceplan;

import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateMaintenancePlanRequest {

    @Positive(message = "维护间隔天数必须大于0")
    private Integer intervalDays;

    private LocalDate lastMaintenanceDate;

    private LocalDate nextMaintenanceDate;

    private String description;

    private Boolean isActive;
}
