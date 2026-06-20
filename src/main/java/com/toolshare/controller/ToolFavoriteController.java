package com.toolshare.controller;

import com.toolshare.dto.ApiResponse;
import com.toolshare.dto.PageResponse;
import com.toolshare.dto.toolfavorite.ToolFavoriteResponse;
import com.toolshare.service.ToolFavoriteService;
import com.toolshare.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/favorites")
@Tag(name = "工具收藏管理", description = "工具收藏的增删改查")
public class ToolFavoriteController {

    private final ToolFavoriteService toolFavoriteService;

    public ToolFavoriteController(ToolFavoriteService toolFavoriteService) {
        this.toolFavoriteService = toolFavoriteService;
    }

    @PostMapping("/{toolId}")
    @Operation(summary = "收藏工具")
    public ApiResponse<ToolFavoriteResponse> addFavorite(@PathVariable Long toolId) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ApiResponse.success(toolFavoriteService.addFavorite(userId, toolId));
    }

    @DeleteMapping("/{toolId}")
    @Operation(summary = "取消收藏工具")
    public ApiResponse<Void> removeFavorite(@PathVariable Long toolId) {
        Long userId = SecurityUtil.getCurrentUserId();
        toolFavoriteService.removeFavorite(userId, toolId);
        return ApiResponse.success("取消收藏成功", null);
    }

    @GetMapping("/{toolId}/exists")
    @Operation(summary = "检查是否已收藏某工具")
    public ApiResponse<Boolean> isFavorited(@PathVariable Long toolId) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ApiResponse.success(toolFavoriteService.isFavorited(userId, toolId));
    }

    @GetMapping("/mine")
    @Operation(summary = "获取我的收藏列表")
    public ApiResponse<PageResponse<ToolFavoriteResponse>> getMyFavorites(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ApiResponse.success(toolFavoriteService.getMyFavorites(userId, page, size));
    }

    @GetMapping("/tool/{toolId}/count")
    @Operation(summary = "获取工具的收藏数量")
    public ApiResponse<Long> getFavoriteCountByToolId(@PathVariable Long toolId) {
        return ApiResponse.success(toolFavoriteService.getFavoriteCountByToolId(toolId));
    }
}
