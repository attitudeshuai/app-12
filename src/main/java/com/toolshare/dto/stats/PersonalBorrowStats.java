package com.toolshare.dto.stats;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PersonalBorrowStats {
    private long totalBorrowCount;
    private long currentBorrowingCount;
    private long overdueCount;
    private double averageReturnPunctualityRate;
}
