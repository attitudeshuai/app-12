package com.toolshare.dto.maintenanceplan;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateMaintenancePlanRequest {

    @NotNull(message = "工具ID不能为空")
    private Long toolId;

    @NotNull(message = "维护间隔天数不能为空")
    @Positive(message = "维护间隔天数必须大于0")
    private Integer intervalDays;

    private LocalDate lastMaintenanceDate;

    private LocalDate nextMaintenanceDate;

    private String description;
}
