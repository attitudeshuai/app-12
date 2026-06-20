package com.toolshare.service;

import com.toolshare.dto.PageResponse;
import com.toolshare.dto.maintenanceplan.CompleteMaintenanceRequest;
import com.toolshare.dto.maintenanceplan.CreateMaintenancePlanRequest;
import com.toolshare.dto.maintenanceplan.MaintenancePlanResponse;
import com.toolshare.dto.maintenanceplan.UpdateMaintenancePlanRequest;
import com.toolshare.entity.*;
import com.toolshare.exception.BadRequestException;
import com.toolshare.exception.ResourceNotFoundException;
import com.toolshare.repository.MaintenancePlanRepository;
import com.toolshare.repository.ToolRepository;
import com.toolshare.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MaintenancePlanService {

    private static final int DUE_SOON_DAYS = 7;

    private final MaintenancePlanRepository maintenancePlanRepository;
    private final ToolRepository toolRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ToolLogService toolLogService;

    public MaintenancePlanService(MaintenancePlanRepository maintenancePlanRepository,
                                   ToolRepository toolRepository,
                                   UserRepository userRepository,
                                   NotificationService notificationService,
                                   ToolLogService toolLogService) {
        this.maintenancePlanRepository = maintenancePlanRepository;
        this.toolRepository = toolRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.toolLogService = toolLogService;
    }

    public PageResponse<MaintenancePlanResponse> getAllPlans(Boolean isActive, Boolean overdue,
                                                              int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<MaintenancePlan> planPage;
        LocalDate today = LocalDate.now();

        if (Boolean.TRUE.equals(overdue)) {
            planPage = maintenancePlanRepository.findDuePlans(today, pageable);
        } else if (isActive != null) {
            if (isActive) {
                planPage = maintenancePlanRepository.findByIsActiveTrue(pageable);
            } else {
                planPage = maintenancePlanRepository.findAll(pageable);
                planPage = planPage.map(p -> {
                    if (Boolean.TRUE.equals(p.getIsActive())) return null;
                    return p;
                });
            }
        } else {
            planPage = maintenancePlanRepository.findAll(pageable);
        }

        List<MaintenancePlanResponse> responseList = toResponseList(planPage.getContent(), today);
        long total = planPage.getTotalElements();
        return PageResponse.of(responseList, total, planPage.getNumber(), planPage.getSize());
    }

    public MaintenancePlanResponse getPlanById(Long id) {
        MaintenancePlan plan = maintenancePlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("维护计划不存在"));
        return toResponse(plan, LocalDate.now());
    }

    public MaintenancePlanResponse getPlanByToolId(Long toolId) {
        MaintenancePlan plan = maintenancePlanRepository.findByToolId(toolId)
                .orElseThrow(() -> new ResourceNotFoundException("该工具暂无维护计划"));
        return toResponse(plan, LocalDate.now());
    }

    @Transactional
    public MaintenancePlanResponse createPlan(CreateMaintenancePlanRequest request, Long currentUserId) {
        Tool tool = toolRepository.findById(request.getToolId())
                .orElseThrow(() -> new ResourceNotFoundException("工具不存在"));

        if (!tool.getOwnerId().equals(currentUserId)) {
            throw new BadRequestException("无权为该工具创建维护计划");
        }

        if (maintenancePlanRepository.findByToolId(request.getToolId()).isPresent()) {
            throw new BadRequestException("该工具已有维护计划，请更新而非创建");
        }

        MaintenancePlan plan = new MaintenancePlan();
        plan.setToolId(request.getToolId());
        plan.setIntervalDays(request.getIntervalDays());
        plan.setDescription(request.getDescription());

        LocalDate lastDate = request.getLastMaintenanceDate();
        LocalDate nextDate = request.getNextMaintenanceDate();

        if (nextDate != null) {
            plan.setNextMaintenanceDate(nextDate);
            plan.setLastMaintenanceDate(lastDate);
        } else if (lastDate != null) {
            plan.setLastMaintenanceDate(lastDate);
            plan.setNextMaintenanceDate(lastDate.plusDays(request.getIntervalDays()));
        } else {
            plan.setNextMaintenanceDate(LocalDate.now().plusDays(request.getIntervalDays()));
        }

        plan.setIsActive(true);
        plan.setIsDueSoonNotified(false);
        plan.setIsDueNotified(false);

        MaintenancePlan saved = maintenancePlanRepository.save(plan);
        return toResponse(saved, LocalDate.now());
    }

    @Transactional
    public MaintenancePlanResponse updatePlan(Long id, UpdateMaintenancePlanRequest request, Long currentUserId) {
        MaintenancePlan plan = maintenancePlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("维护计划不存在"));

        Tool tool = toolRepository.findById(plan.getToolId()).orElse(null);
        if (tool != null && !tool.getOwnerId().equals(currentUserId)) {
            throw new BadRequestException("无权修改该维护计划");
        }

        if (request.getIntervalDays() != null) {
            plan.setIntervalDays(request.getIntervalDays());
        }
        if (request.getLastMaintenanceDate() != null) {
            plan.setLastMaintenanceDate(request.getLastMaintenanceDate());
        }
        if (request.getNextMaintenanceDate() != null) {
            plan.setNextMaintenanceDate(request.getNextMaintenanceDate());
            plan.setIsDueSoonNotified(false);
            plan.setIsDueNotified(false);
        }
        if (request.getDescription() != null) {
            plan.setDescription(request.getDescription());
        }
        if (request.getIsActive() != null) {
            plan.setIsActive(request.getIsActive());
        }

        MaintenancePlan saved = maintenancePlanRepository.save(plan);
        return toResponse(saved, LocalDate.now());
    }

    @Transactional
    public MaintenancePlanResponse completeMaintenance(Long id, CompleteMaintenanceRequest request, Long currentUserId) {
        MaintenancePlan plan = maintenancePlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("维护计划不存在"));

        Tool tool = toolRepository.findById(plan.getToolId())
                .orElseThrow(() -> new ResourceNotFoundException("工具不存在"));

        if (!tool.getOwnerId().equals(currentUserId)) {
            throw new BadRequestException("无权为该工具执行维护完成操作");
        }

        plan.setLastMaintenanceDate(request.getMaintenanceDate());
        plan.setNextMaintenanceDate(request.getMaintenanceDate().plusDays(plan.getIntervalDays()));
        plan.setIsDueSoonNotified(false);
        plan.setIsDueNotified(false);

        if (tool.getStatus() == ToolStatus.MAINTENANCE) {
            tool.setStatus(ToolStatus.AVAILABLE);
            toolRepository.save(tool);
        }

        MaintenancePlan saved = maintenancePlanRepository.save(plan);

        toolLogService.createLogInternal(plan.getToolId(), currentUserId, ToolLogAction.MAINTENANCE,
                request.getRemarks() != null ? request.getRemarks() : "定期维护完成");

        notificationService.createNotification(
                tool.getOwnerId(),
                NotificationType.MAINTENANCE_COMPLETED,
                "工具维护完成",
                "工具「" + tool.getName() + "」的定期维护已完成，下次维护日期：" + saved.getNextMaintenanceDate(),
                plan.getId()
        );

        notifyAdmins("工具维护完成",
                "工具「" + tool.getName() + "」的定期维护已完成，维护日期：" + request.getMaintenanceDate(),
                plan.getId());

        return toResponse(saved, LocalDate.now());
    }

    @Transactional
    public void deletePlan(Long id, Long currentUserId) {
        MaintenancePlan plan = maintenancePlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("维护计划不存在"));

        Tool tool = toolRepository.findById(plan.getToolId()).orElse(null);
        if (tool != null && !tool.getOwnerId().equals(currentUserId)) {
            throw new BadRequestException("无权删除该维护计划");
        }

        maintenancePlanRepository.delete(plan);
    }

    @Transactional
    public void processDueMaintenance(LocalDate today) {
        Pageable pageable = PageRequest.of(0, 100);
        Page<MaintenancePlan> overduePage = maintenancePlanRepository.findOverdueUnnotifiedPlans(today, pageable);

        while (!overduePage.isEmpty()) {
            List<MaintenancePlan> toUpdate = new ArrayList<>();
            for (MaintenancePlan plan : overduePage.getContent()) {
                Tool tool = toolRepository.findById(plan.getToolId()).orElse(null);
                if (tool == null) continue;

                if (tool.getStatus() == ToolStatus.AVAILABLE) {
                    tool.setStatus(ToolStatus.MAINTENANCE);
                    toolRepository.save(tool);
                }

                if (!Boolean.TRUE.equals(plan.getIsDueNotified())) {
                    long overdueDays = ChronoUnit.DAYS.between(plan.getNextMaintenanceDate(), today);
                    notificationService.createNotification(
                            tool.getOwnerId(),
                            NotificationType.MAINTENANCE_DUE,
                            "工具需要维护",
                            "工具「" + tool.getName() + "」已达到定期维护时间（逾期 " + overdueDays + " 天），" +
                            "当前状态已设为维护中，请尽快安排维护。",
                            plan.getId()
                    );

                    notifyAdmins("工具需要维护",
                            "工具「" + tool.getName() + "」已达到定期维护时间（逾期 " + overdueDays + " 天），" +
                            "已自动置为维护中状态。",
                            plan.getId());

                    plan.setIsDueNotified(true);
                    toUpdate.add(plan);
                }
            }
            if (!toUpdate.isEmpty()) {
                maintenancePlanRepository.saveAll(toUpdate);
            }

            if (overduePage.hasNext()) {
                overduePage = maintenancePlanRepository.findOverdueUnnotifiedPlans(today,
                        PageRequest.of(overduePage.getNumber() + 1, 100));
            } else {
                break;
            }
        }
    }

    @Transactional
    public void processDueSoonMaintenance(LocalDate today) {
        LocalDate startDate = today.plusDays(1);
        LocalDate endDate = today.plusDays(DUE_SOON_DAYS);
        Pageable pageable = PageRequest.of(0, 100);

        Page<MaintenancePlan> dueSoonPage = maintenancePlanRepository.findDueSoonPlans(startDate, endDate, pageable);

        while (!dueSoonPage.isEmpty()) {
            List<MaintenancePlan> toUpdate = new ArrayList<>();
            for (MaintenancePlan plan : dueSoonPage.getContent()) {
                Tool tool = toolRepository.findById(plan.getToolId()).orElse(null);
                if (tool == null) continue;

                long daysLeft = ChronoUnit.DAYS.between(today, plan.getNextMaintenanceDate());

                notificationService.createNotification(
                        tool.getOwnerId(),
                        NotificationType.MAINTENANCE_DUE_SOON,
                        "工具即将需要维护",
                        "工具「" + tool.getName() + "」还有 " + daysLeft + " 天需要定期维护，请提前做好准备。",
                        plan.getId()
                );

                notifyAdmins("工具即将需要维护",
                        "工具「" + tool.getName() + "」还有 " + daysLeft + " 天需要定期维护。",
                        plan.getId());

                plan.setIsDueSoonNotified(true);
                toUpdate.add(plan);
            }
            if (!toUpdate.isEmpty()) {
                maintenancePlanRepository.saveAll(toUpdate);
            }

            if (dueSoonPage.hasNext()) {
                dueSoonPage = maintenancePlanRepository.findDueSoonPlans(startDate, endDate,
                        PageRequest.of(dueSoonPage.getNumber() + 1, 100));
            } else {
                break;
            }
        }
    }

    public Map<Long, MaintenancePlanResponse> getPlanMapByToolIds(List<Long> toolIds) {
        Map<Long, MaintenancePlanResponse> result = new HashMap<>();
        if (toolIds == null || toolIds.isEmpty()) return result;

        LocalDate today = LocalDate.now();
        List<MaintenancePlan> plans = maintenancePlanRepository.findByToolIdIn(toolIds);

        for (MaintenancePlan plan : plans) {
            result.put(plan.getToolId(), toResponse(plan, today));
        }
        return result;
    }

    private void notifyAdmins(String title, String content, Long relatedId) {
        List<User> admins = userRepository.findByRoleAndIsEnabledTrue(Role.ADMIN);
        for (User admin : admins) {
            notificationService.createNotification(
                    admin.getId(),
                    NotificationType.MAINTENANCE_DUE,
                    title,
                    content,
                    relatedId
            );
        }
    }

    private List<MaintenancePlanResponse> toResponseList(List<MaintenancePlan> plans, LocalDate today) {
        if (plans == null || plans.isEmpty()) return new ArrayList<>();

        plans = plans.stream().filter(p -> p != null).collect(Collectors.toList());

        List<Long> toolIds = plans.stream().map(MaintenancePlan::getToolId).collect(Collectors.toList());
        Map<Long, String> toolNameMap = new HashMap<>();
        toolRepository.findAllById(toolIds).forEach(t -> toolNameMap.put(t.getId(), t.getName()));

        List<MaintenancePlanResponse> responses = new ArrayList<>();
        for (MaintenancePlan plan : plans) {
            MaintenancePlanResponse resp = buildResponse(plan, toolNameMap.get(plan.getToolId()), today);
            responses.add(resp);
        }
        return responses;
    }

    private MaintenancePlanResponse toResponse(MaintenancePlan plan, LocalDate today) {
        String toolName = toolRepository.findById(plan.getToolId())
                .map(Tool::getName).orElse(null);
        return buildResponse(plan, toolName, today);
    }

    private MaintenancePlanResponse buildResponse(MaintenancePlan plan, String toolName, LocalDate today) {
        long daysUntilDue = ChronoUnit.DAYS.between(today, plan.getNextMaintenanceDate());
        boolean isOverdue = daysUntilDue < 0;

        return new MaintenancePlanResponse(
                plan.getId(),
                plan.getToolId(),
                toolName,
                plan.getIntervalDays(),
                plan.getLastMaintenanceDate(),
                plan.getNextMaintenanceDate(),
                plan.getDescription(),
                plan.getIsActive(),
                plan.getCreatedAt(),
                plan.getUpdatedAt(),
                daysUntilDue,
                isOverdue
        );
    }
}
