package com.toolshare.dto.maintenanceplan;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CompleteMaintenanceRequest {

    @NotNull(message = "维护完成日期不能为空")
    private LocalDate maintenanceDate;

    private String remarks;
}
