package com.toolshare.dto.stats;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HotToolRank {
    private Long toolId;
    private String toolName;
    private String category;
    private String image;
    private Long borrowCount;
    private Long favoriteCount;
    private Long compositeScore;
    private Integer rank;
}
