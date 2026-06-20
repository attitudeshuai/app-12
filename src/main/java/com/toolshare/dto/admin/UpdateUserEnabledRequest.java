package com.toolshare.dto.admin;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateUserEnabledRequest {
    @NotNull(message = "启用状态不能为空")
    private Boolean isEnabled;
}
