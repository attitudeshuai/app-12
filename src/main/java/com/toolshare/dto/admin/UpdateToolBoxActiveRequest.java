package com.toolshare.dto.admin;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateToolBoxActiveRequest {
    @NotNull(message = "激活状态不能为空")
    private Boolean isActive;
}
