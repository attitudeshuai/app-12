package com.toolshare.controller;

import com.toolshare.dto.ApiResponse;
import com.toolshare.dto.PageResponse;
import com.toolshare.dto.admin.UpdateUserEnabledRequest;
import com.toolshare.dto.admin.UpdateUserRoleRequest;
import com.toolshare.dto.auth.UserResponse;
import com.toolshare.dto.borrowrequest.BorrowRequestResponse;
import com.toolshare.dto.stats.UserActivityRankingResponse;
import com.toolshare.dto.tool.ToolResponse;
import com.toolshare.dto.toolbox.ToolBoxResponse;
import com.toolshare.entity.BorrowRequestStatus;
import com.toolshare.entity.Role;
import com.toolshare.entity.ToolLogAction;
import com.toolshare.entity.ToolStatus;
import com.toolshare.service.AdminService;
import com.toolshare.service.UserActivityService;
import com.toolshare.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "管理员后台", description = "管理员专用接口，需要管理员权限")
public class AdminController {

    private final AdminService adminService;
    private final UserActivityService userActivityService;

    public AdminController(AdminService adminService, UserActivityService userActivityService) {
        this.adminService = adminService;
        this.userActivityService = userActivityService;
    }

    @GetMapping("/users")
    @Operation(summary = "获取所有用户列表")
    public ApiResponse<PageResponse<UserResponse>> getAllUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean isEnabled,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ApiResponse.success(adminService.getAllUsers(keyword, role, isEnabled, page, size, sortBy, sortDir));
    }

    @GetMapping("/users/{id}")
    @Operation(summary = "获取用户详情")
    public ApiResponse<UserResponse> getUserById(@PathVariable Long id) {
        return ApiResponse.success(adminService.getUserById(id));
    }

    @PatchMapping("/users/{id}/role")
    @Operation(summary = "修改用户角色")
    public ApiResponse<UserResponse> updateUserRole(@PathVariable Long id,
                                                    @Valid @RequestBody UpdateUserRoleRequest request) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        return ApiResponse.success(adminService.updateUserRole(id, request.getRole(), currentUserId));
    }

    @PatchMapping("/users/{id}/enabled")
    @Operation(summary = "启用/禁用用户")
    public ApiResponse<UserResponse> updateUserEnabled(@PathVariable Long id,
                                                       @Valid @RequestBody UpdateUserEnabledRequest request) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        return ApiResponse.success(adminService.updateUserEnabled(id, request.getIsEnabled(), currentUserId));
    }

    @GetMapping("/tools")
    @Operation(summary = "获取所有工具列表")
    public ApiResponse<PageResponse<ToolResponse>> getAllTools(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) ToolStatus status,
            @RequestParam(required = false) Long boxId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ApiResponse.success(adminService.getAllTools(keyword, category, status, boxId, page, size, sortBy, sortDir));
    }

    @PatchMapping("/tools/{id}/disable")
    @Operation(summary = "禁用工具")
    public ApiResponse<ToolResponse> disableTool(@PathVariable Long id) {
        return ApiResponse.success(adminService.disableTool(id));
    }

    @PatchMapping("/tools/{id}/enable")
    @Operation(summary = "启用工具")
    public ApiResponse<ToolResponse> enableTool(@PathVariable Long id) {
        return ApiResponse.success(adminService.enableTool(id));
    }

    @GetMapping("/toolboxes")
    @Operation(summary = "获取所有工具箱列表")
    public ApiResponse<PageResponse<ToolBoxResponse>> getAllToolBoxes(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ApiResponse.success(adminService.getAllToolBoxes(keyword, isActive, page, size, sortBy, sortDir));
    }

    @PatchMapping("/toolboxes/{id}/deactivate")
    @Operation(summary = "停用工具箱")
    public ApiResponse<ToolBoxResponse> deactivateToolBox(@PathVariable Long id) {
        return ApiResponse.success(adminService.deactivateToolBox(id));
    }

    @PatchMapping("/toolboxes/{id}/activate")
    @Operation(summary = "启用工具箱")
    public ApiResponse<ToolBoxResponse> activateToolBox(@PathVariable Long id) {
        return ApiResponse.success(adminService.activateToolBox(id));
    }

    @GetMapping("/borrow-requests")
    @Operation(summary = "获取所有借用申请")
    public ApiResponse<PageResponse<BorrowRequestResponse>> getAllBorrowRequests(
            @RequestParam(required = false) BorrowRequestStatus status,
            @RequestParam(required = false) Long requesterId,
            @RequestParam(required = false) Long toolId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ApiResponse.success(adminService.getAllBorrowRequests(status, requesterId, toolId,
                startDate, endDate, page, size, sortBy, sortDir));
    }

    @GetMapping("/tool-logs/export")
    @Operation(summary = "导出使用日志为CSV文件")
    public ResponseEntity<byte[]> exportToolLogs(
            @RequestParam(required = false) Long toolId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) ToolLogAction action,
            @RequestParam(required = false) LocalDateTime startTime,
            @RequestParam(required = false) LocalDateTime endTime) {
        byte[] csvData = adminService.exportToolLogs(toolId, userId, action, startTime, endTime);

        String fileName = "tool_logs_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(fileName, java.nio.charset.StandardCharsets.UTF_8)
                .build());

        return ResponseEntity.ok()
                .headers(headers)
                .body(csvData);
    }

    @GetMapping("/user-activity-ranking")
    @Operation(summary = "用户活跃度排行")
    public ApiResponse<List<UserActivityRankingResponse>> getUserActivityRanking(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "20") int limit) {
        if (days <= 0) {
            days = 30;
        }
        if (limit <= 0) {
            limit = 20;
        }
        if (limit > 100) {
            limit = 100;
        }
        return ApiResponse.success(userActivityService.getActivityRanking(days, limit));
    }
}
