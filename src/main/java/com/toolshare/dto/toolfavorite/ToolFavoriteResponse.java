package com.toolshare.dto.toolfavorite;

import com.toolshare.entity.ToolStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToolFavoriteResponse {
    private Long id;
    private Long userId;
    private Long toolId;
    private String toolName;
    private String toolCategory;
    private ToolStatus toolStatus;
    private String toolImage;
    private String toolDescription;
    private Long toolOwnerId;
    private String toolOwnerName;
    private LocalDateTime createdAt;
}
