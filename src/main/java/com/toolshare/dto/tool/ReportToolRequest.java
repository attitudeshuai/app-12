package com.toolshare.dto.tool;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReportToolRequest {
    @NotBlank(message = "故障描述不能为空")
    @Size(max = 2000, message = "故障描述不能超过2000个字符")
    private String description;
}
