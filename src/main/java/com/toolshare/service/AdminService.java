package com.toolshare.service;

import com.toolshare.dto.PageResponse;
import com.toolshare.dto.auth.UserResponse;
import com.toolshare.dto.borrowrequest.BorrowRequestResponse;
import com.toolshare.dto.tool.ToolResponse;
import com.toolshare.dto.toolbox.ToolBoxResponse;
import com.toolshare.entity.BorrowRequestStatus;
import com.toolshare.entity.Role;
import com.toolshare.entity.ToolStatus;
import com.toolshare.entity.User;
import com.toolshare.exception.BadRequestException;
import com.toolshare.exception.ResourceNotFoundException;
import com.toolshare.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.toolshare.entity.ToolLogAction;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final AuthService authService;
    private final BorrowRequestService borrowRequestService;
    private final ToolService toolService;
    private final ToolBoxService toolBoxService;
    private final ToolLogService toolLogService;

    public AdminService(UserRepository userRepository,
                       AuthService authService,
                       BorrowRequestService borrowRequestService,
                       ToolService toolService,
                       ToolBoxService toolBoxService,
                       ToolLogService toolLogService) {
        this.userRepository = userRepository;
        this.authService = authService;
        this.borrowRequestService = borrowRequestService;
        this.toolService = toolService;
        this.toolBoxService = toolBoxService;
        this.toolLogService = toolLogService;
    }

    public PageResponse<UserResponse> getAllUsers(String keyword, Role role, Boolean isEnabled,
                                           int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<User> userPage = userRepository.search(keyword, role, isEnabled, pageable);
        Page<UserResponse> responsePage = userPage.map(authService::toUserResponse);

        return PageResponse.from(responsePage);
    }

    public UserResponse getUserById(Long id) {
        return authService.adminGetUserById(id);
    }

    @Transactional
    public UserResponse updateUserRole(Long id, Role role, Long currentUserId) {
        return authService.adminUpdateUserRole(id, role, currentUserId);
    }

    @Transactional
    public UserResponse updateUserEnabled(Long id, Boolean isEnabled, Long currentUserId) {
        return authService.adminUpdateUserEnabled(id, isEnabled, currentUserId);
    }

    public PageResponse<ToolResponse> getAllTools(String keyword, String category, ToolStatus status,
                                               Long boxId, int page, int size, String sortBy, String sortDir) {
        return toolService.getAllTools(keyword, category, status, boxId, page, size, sortBy, sortDir);
    }

    @Transactional
    public ToolResponse disableTool(Long id) {
        return toolService.adminDisableTool(id);
    }

    @Transactional
    public ToolResponse enableTool(Long id) {
        return toolService.adminEnableTool(id);
    }

    public PageResponse<ToolBoxResponse> getAllToolBoxes(String keyword, Boolean isActive,
                                                         int page, int size, String sortBy, String sortDir) {
        return toolBoxService.getAllToolBoxes(keyword, isActive, page, size, sortBy, sortDir);
    }

    @Transactional
    public ToolBoxResponse deactivateToolBox(Long id) {
        return toolBoxService.adminUpdateToolBoxActive(id, false);
    }

    @Transactional
    public ToolBoxResponse activateToolBox(Long id) {
        return toolBoxService.adminUpdateToolBoxActive(id, true);
    }

    public PageResponse<BorrowRequestResponse> getAllBorrowRequests(BorrowRequestStatus status, Long requesterId,
                                                             Long toolId, LocalDate startDate, LocalDate endDate,
                                                             int page, int size, String sortBy, String sortDir) {
        return borrowRequestService.getAllBorrowRequests(status, requesterId, toolId,
                startDate, endDate, page, size, sortBy, sortDir);
    }

    public byte[] exportToolLogs(Long toolId, Long userId, ToolLogAction action,
                                  LocalDateTime startTime, LocalDateTime endTime) {
        return toolLogService.exportToolLogsToCsv(toolId, userId, action, startTime, endTime);
    }
}
