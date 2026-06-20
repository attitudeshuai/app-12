package com.toolshare.controller;

import com.toolshare.dto.ApiResponse;
import com.toolshare.dto.PageResponse;
import com.toolshare.dto.admin.UpdateUserEnabledRequest;
import com.toolshare.dto.admin.UpdateUserRoleRequest;
import com.toolshare.dto.auth.UserResponse;
import com.toolshare.dto.borrowrequest.BorrowRequestResponse;
import com.toolshare.dto.tool.ToolResponse;
import com.toolshare.dto.toolbox.ToolBoxResponse;
import com.toolshare.entity.BorrowRequestStatus;
import com.toolshare.entity.Role;
import com.toolshare.entity.ToolStatus;
import com.toolshare.exception.BadRequestException;
import com.toolshare.service.AdminService;
import com.toolshare.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "管理员后台", description = "管理员专用接口，需要管理员权限")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    private void checkAdmin() {
        if (!SecurityUtil.isAdmin()) {
            throw new BadRequestException("需要管理员权限");
        }
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
        checkAdmin();
        return ApiResponse.success(adminService.getAllUsers(keyword, role, isEnabled, page, size, sortBy, sortDir));
    }

    @GetMapping("/users/{id}")
    @Operation(summary = "获取用户详情")
    public ApiResponse<UserResponse> getUserById(@PathVariable Long id) {
        checkAdmin();
        return ApiResponse.success(adminService.getUserById(id));
    }

    @PatchMapping("/users/{id}/role")
    @Operation(summary = "修改用户角色")
    public ApiResponse<UserResponse> updateUserRole(@PathVariable Long id,
                                                    @Valid @RequestBody UpdateUserRoleRequest request) {
        checkAdmin();
        Long currentUserId = SecurityUtil.getCurrentUserId();
        return ApiResponse.success(adminService.updateUserRole(id, request.getRole(), currentUserId));
    }

    @PatchMapping("/users/{id}/enabled")
    @Operation(summary = "启用/禁用用户")
    public ApiResponse<UserResponse> updateUserEnabled(@PathVariable Long id,
                                                       @Valid @RequestBody UpdateUserEnabledRequest request) {
        checkAdmin();
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
        checkAdmin();
        return ApiResponse.success(adminService.getAllTools(keyword, category, status, boxId, page, size, sortBy, sortDir));
    }

    @PatchMapping("/tools/{id}/disable")
    @Operation(summary = "禁用工具")
    public ApiResponse<ToolResponse> disableTool(@PathVariable Long id) {
        checkAdmin();
        return ApiResponse.success(adminService.disableTool(id));
    }

    @PatchMapping("/tools/{id}/enable")
    @Operation(summary = "启用工具")
    public ApiResponse<ToolResponse> enableTool(@PathVariable Long id) {
        checkAdmin();
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
        checkAdmin();
        return ApiResponse.success(adminService.getAllToolBoxes(keyword, isActive, page, size, sortBy, sortDir));
    }

    @PatchMapping("/toolboxes/{id}/deactivate")
    @Operation(summary = "停用工具箱")
    public ApiResponse<ToolBoxResponse> deactivateToolBox(@PathVariable Long id) {
        checkAdmin();
        return ApiResponse.success(adminService.deactivateToolBox(id));
    }

    @PatchMapping("/toolboxes/{id}/activate")
    @Operation(summary = "启用工具箱")
    public ApiResponse<ToolBoxResponse> activateToolBox(@PathVariable Long id) {
        checkAdmin();
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
        checkAdmin();
        return ApiResponse.success(adminService.getAllBorrowRequests(status, requesterId, toolId,
                startDate, endDate, page, size, sortBy, sortDir));
    }
}
