package com.toolshare.dto.stats;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserActivityRankingResponse {
    private Integer rank;
    private Long userId;
    private String username;
    private String avatar;
    private Long loginCount;
    private Long publishedToolCount;
    private Long initiatedBorrowCount;
    private Long completedBorrowCount;
    private Double activityScore;
}
