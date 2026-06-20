package com.toolshare.dto.helppost;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateHelpResponseRequest {

    private String message;

    @NotBlank(message = "联系方式不能为空")
    @Size(max = 200, message = "联系方式长度不能超过200个字符")
    private String contactInfo;
}
