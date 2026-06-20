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

import java.time.LocalDate;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final BorrowRequestService borrowRequestService;
    private final ToolService toolService;
    private final ToolBoxService toolBoxService;

    public AdminService(UserRepository userRepository,
                       BorrowRequestService borrowRequestService,
                       ToolService toolService,
                       ToolBoxService toolBoxService) {
        this.userRepository = userRepository;
        this.borrowRequestService = borrowRequestService;
        this.toolService = toolService;
        this.toolBoxService = toolBoxService;
    }

    public PageResponse<UserResponse> getAllUsers(String keyword, Role role, Boolean isEnabled,
                                           int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<User> userPage = userRepository.search(keyword, role, isEnabled, pageable);
        Page<UserResponse> responsePage = userPage.map(AuthService::toUserResponse);

        return PageResponse.from(responsePage);
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        return AuthService.toUserResponse(user);
    }

    @Transactional
    public UserResponse updateUserRole(Long id, Role role, Long currentUserId) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

        if (user.getId().equals(currentUserId)) {
            throw new BadRequestException("不能修改自己的角色");
        }

        user.setRole(role);
        User savedUser = userRepository.save(user);
        return AuthService.toUserResponse(savedUser);
    }

    @Transactional
    public UserResponse updateUserEnabled(Long id, Boolean isEnabled, Long currentUserId) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

        if (user.getId().equals(currentUserId)) {
            throw new BadRequestException("不能禁用自己的账号");
        }

        user.setIsEnabled(isEnabled);
        User savedUser = userRepository.save(user);
        return AuthService.toUserResponse(savedUser);
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
}
