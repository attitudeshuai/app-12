package com.toolshare.controller;

import com.toolshare.dto.ApiResponse;
import com.toolshare.dto.PageResponse;
import com.toolshare.dto.helppost.*;
import com.toolshare.entity.HelpPostStatus;
import com.toolshare.service.HelpBoardService;
import com.toolshare.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/help-board")
@Tag(name = "社区求助板管理", description = "社区求助帖和响应的管理")
public class HelpBoardController {

    private final HelpBoardService helpBoardService;

    public HelpBoardController(HelpBoardService helpBoardService) {
        this.helpBoardService = helpBoardService;
    }

    @PostMapping("/posts")
    @Operation(summary = "发布求助帖")
    public ApiResponse<HelpPostResponse> createHelpPost(@Valid @RequestBody CreateHelpPostRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ApiResponse.success(helpBoardService.createHelpPost(request, userId));
    }

    @PutMapping("/posts/{postId}")
    @Operation(summary = "更新求助帖")
    public ApiResponse<HelpPostResponse> updateHelpPost(
            @PathVariable Long postId,
            @Valid @RequestBody UpdateHelpPostRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ApiResponse.success(helpBoardService.updateHelpPost(postId, request, userId));
    }

    @DeleteMapping("/posts/{postId}")
    @Operation(summary = "删除求助帖")
    public ApiResponse<Void> deleteHelpPost(@PathVariable Long postId) {
        Long userId = SecurityUtil.getCurrentUserId();
        helpBoardService.deleteHelpPost(postId, userId);
        return ApiResponse.success("删除成功", null);
    }

    @GetMapping("/posts/{postId}")
    @Operation(summary = "获取求助帖详情")
    public ApiResponse<HelpPostResponse> getHelpPostById(@PathVariable Long postId) {
        return ApiResponse.success(helpBoardService.getHelpPostById(postId));
    }

    @GetMapping("/posts")
    @Operation(summary = "获取求助帖列表")
    public ApiResponse<PageResponse<HelpPostResponse>> getAllHelpPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) List<HelpPostStatus> statuses) {
        return ApiResponse.success(helpBoardService.getAllHelpPosts(page, size, category, statuses));
    }

    @GetMapping("/posts/mine")
    @Operation(summary = "获取我发布的求助帖")
    public ApiResponse<PageResponse<HelpPostResponse>> getMyHelpPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) HelpPostStatus status) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ApiResponse.success(helpBoardService.getMyHelpPosts(userId, page, size, status));
    }

    @GetMapping("/posts/accepted")
    @Operation(summary = "获取我被接受的求助帖")
    public ApiResponse<PageResponse<HelpPostResponse>> getAcceptedHelpPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ApiResponse.success(helpBoardService.getAcceptedHelpPosts(userId, page, size));
    }

    @PostMapping("/posts/{postId}/responses")
    @Operation(summary = "响应求助帖")
    public ApiResponse<HelpResponseResponse> createHelpResponse(
            @PathVariable Long postId,
            @Valid @RequestBody CreateHelpResponseRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ApiResponse.success(helpBoardService.createHelpResponse(postId, request, userId));
    }

    @PostMapping("/posts/{postId}/responses/{responseId}/accept")
    @Operation(summary = "接受某个响应")
    public ApiResponse<HelpResponseResponse> acceptHelpResponse(
            @PathVariable Long postId,
            @PathVariable Long responseId) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ApiResponse.success(helpBoardService.acceptHelpResponse(postId, responseId, userId));
    }

    @PostMapping("/posts/{postId}/complete")
    @Operation(summary = "标记求助帖完成")
    public ApiResponse<HelpPostResponse> completeHelpPost(@PathVariable Long postId) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ApiResponse.success(helpBoardService.completeHelpPost(postId, userId));
    }

    @PostMapping("/posts/{postId}/cancel")
    @Operation(summary = "取消求助帖")
    public ApiResponse<HelpPostResponse> cancelHelpPost(@PathVariable Long postId) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ApiResponse.success(helpBoardService.cancelHelpPost(postId, userId));
    }

    @GetMapping("/posts/{postId}/responses")
    @Operation(summary = "获取求助帖的响应列表")
    public ApiResponse<List<HelpResponseResponse>> getHelpResponsesByPostId(@PathVariable Long postId) {
        return ApiResponse.success(helpBoardService.getHelpResponsesByPostId(postId));
    }

    @GetMapping("/responses/mine")
    @Operation(summary = "获取我的响应列表")
    public ApiResponse<PageResponse<HelpResponseResponse>> getMyHelpResponses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ApiResponse.success(helpBoardService.getMyHelpResponses(userId, page, size));
    }
}
