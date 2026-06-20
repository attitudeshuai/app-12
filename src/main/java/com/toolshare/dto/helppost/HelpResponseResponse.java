package com.toolshare.dto.helppost;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HelpResponseResponse {
    private Long id;
    private Long helpPostId;
    private String helpPostTitle;
    private Long responderId;
    private String responderName;
    private String message;
    private String contactInfo;
    private boolean isAccepted;
    private LocalDateTime createdAt;
}
