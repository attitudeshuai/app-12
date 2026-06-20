package com.toolshare.dto.maintenanceplan;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MaintenancePlanResponse {

    private Long id;
    private Long toolId;
    private String toolName;
    private Integer intervalDays;
    private LocalDate lastMaintenanceDate;
    private LocalDate nextMaintenanceDate;
    private String description;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long daysUntilDue;
    private Boolean isOverdue;
}
