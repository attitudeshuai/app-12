package com.toolshare.dto.helppost;

import com.toolshare.entity.HelpPostStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HelpPostResponse {
    private Long id;
    private Long posterId;
    private String posterName;
    private String title;
    private String content;
    private String category;
    private HelpPostStatus status;
    private String location;
    private LocalDateTime deadline;
    private Long acceptedResponderId;
    private String acceptedResponderName;
    private Integer responseCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
