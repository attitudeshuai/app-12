package com.toolshare.controller;

import com.toolshare.dto.ApiResponse;
import com.toolshare.dto.stats.HotToolRank;
import com.toolshare.dto.stats.OverviewStats;
import com.toolshare.dto.stats.PersonalBorrowStats;
import com.toolshare.dto.stats.TrendStats;
import com.toolshare.service.StatsService;
import com.toolshare.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/stats")
@Tag(name = "统计管理", description = "数据统计与趋势分析")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/overview")
    @Operation(summary = "总览统计（含热门工具排行）")
    public ApiResponse<OverviewStats> getOverviewStats() {
        return ApiResponse.success(statsService.getOverviewStats());
    }

    @GetMapping("/hot-tools")
    @Operation(summary = "热门工具排行（按借用次数和收藏次数综合排名）")
    public ApiResponse<List<HotToolRank>> getHotTools(
            @RequestParam(defaultValue = "10") int limit) {
        if (limit <= 0) {
            limit = 10;
        }
        if (limit > 100) {
            limit = 100;
        }
        return ApiResponse.success(statsService.getHotTools(limit));
    }

    @GetMapping("/trend")
    @Operation(summary = "趋势统计")
    public ApiResponse<TrendStats> getTrendStats(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        return ApiResponse.success(statsService.getTrendStats(startDate, endDate));
    }

    @GetMapping("/personal-borrow")
    @Operation(summary = "我的借用统计")
    public ApiResponse<PersonalBorrowStats> getPersonalBorrowStats() {
        Long userId = SecurityUtil.getCurrentUserId();
        return ApiResponse.success(statsService.getPersonalBorrowStats(userId));
    }
}
