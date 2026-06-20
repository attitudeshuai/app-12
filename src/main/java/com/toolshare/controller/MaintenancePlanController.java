package com.toolshare.controller;

import com.toolshare.dto.ApiResponse;
import com.toolshare.dto.PageResponse;
import com.toolshare.dto.maintenanceplan.CompleteMaintenanceRequest;
import com.toolshare.dto.maintenanceplan.CreateMaintenancePlanRequest;
import com.toolshare.dto.maintenanceplan.MaintenancePlanResponse;
import com.toolshare.dto.maintenanceplan.UpdateMaintenancePlanRequest;
import com.toolshare.service.MaintenancePlanService;
import com.toolshare.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/maintenance-plans")
@Tag(name = "工具维护计划管理", description = "工具定期维护计划的增删改查")
public class MaintenancePlanController {

    private final MaintenancePlanService maintenancePlanService;

    public MaintenancePlanController(MaintenancePlanService maintenancePlanService) {
        this.maintenancePlanService = maintenancePlanService;
    }

    @GetMapping
    @Operation(summary = "获取维护计划列表")
    public ApiResponse<PageResponse<MaintenancePlanResponse>> getAllPlans(
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) Boolean overdue,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "nextMaintenanceDate") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ApiResponse.success(maintenancePlanService.getAllPlans(isActive, overdue, page, size, sortBy, sortDir));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取维护计划详情")
    public ApiResponse<MaintenancePlanResponse> getPlanById(@PathVariable Long id) {
        return ApiResponse.success(maintenancePlanService.getPlanById(id));
    }

    @GetMapping("/tool/{toolId}")
    @Operation(summary = "根据工具ID获取维护计划")
    public ApiResponse<MaintenancePlanResponse> getPlanByToolId(@PathVariable Long toolId) {
        return ApiResponse.success(maintenancePlanService.getPlanByToolId(toolId));
    }

    @PostMapping
    @Operation(summary = "创建维护计划")
    public ApiResponse<MaintenancePlanResponse> createPlan(@Valid @RequestBody CreateMaintenancePlanRequest request) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        return ApiResponse.success(maintenancePlanService.createPlan(request, currentUserId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新维护计划")
    public ApiResponse<MaintenancePlanResponse> updatePlan(@PathVariable Long id,
                                                            @Valid @RequestBody UpdateMaintenancePlanRequest request) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        return ApiResponse.success(maintenancePlanService.updatePlan(id, request, currentUserId));
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "标记维护完成")
    public ApiResponse<MaintenancePlanResponse> completeMaintenance(@PathVariable Long id,
                                                                     @Valid @RequestBody CompleteMaintenanceRequest request) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        return ApiResponse.success(maintenancePlanService.completeMaintenance(id, request, currentUserId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除维护计划")
    public ApiResponse<Void> deletePlan(@PathVariable Long id) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        maintenancePlanService.deletePlan(id, currentUserId);
        return ApiResponse.success("删除成功", null);
    }
}
