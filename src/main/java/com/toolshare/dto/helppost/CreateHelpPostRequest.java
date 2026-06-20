package com.toolshare.dto.helppost;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateHelpPostRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题长度不能超过200个字符")
    private String title;

    @NotBlank(message = "内容不能为空")
    private String content;

    @Size(max = 50, message = "分类长度不能超过50个字符")
    private String category;

    @Size(max = 200, message = "地点长度不能超过200个字符")
    private String location;

    private LocalDateTime deadline;
}
