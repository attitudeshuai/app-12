package com.toolshare.dto.tool;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CompleteRepairRequest {
    @NotBlank(message = "维修说明不能为空")
    @Size(max = 2000, message = "维修说明不能超过2000个字符")
    private String description;
}
