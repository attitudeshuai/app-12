package com.toolshare.service;

import com.toolshare.dto.PageResponse;
import com.toolshare.dto.borrowrequest.BorrowRequestResponse;
import com.toolshare.dto.borrowrequest.CreateBorrowRequestRequest;
import com.toolshare.dto.borrowrequest.UpdateBorrowRequestRequest;
import com.toolshare.dto.borrowrequest.UpdateBorrowStatusRequest;
import com.toolshare.entity.BorrowRequest;
import com.toolshare.entity.BorrowRequestStatus;
import com.toolshare.entity.Tool;
import com.toolshare.entity.ToolLogAction;
import com.toolshare.entity.ToolStatus;
import com.toolshare.exception.BadRequestException;
import com.toolshare.exception.ResourceNotFoundException;
import com.toolshare.repository.BorrowRequestRepository;
import com.toolshare.repository.ToolRepository;
import com.toolshare.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class BorrowRequestService {

    private final BorrowRequestRepository borrowRequestRepository;
    private final ToolRepository toolRepository;
    private final UserRepository userRepository;
    private final ToolLogService toolLogService;

    public BorrowRequestService(BorrowRequestRepository borrowRequestRepository,
                                ToolRepository toolRepository,
                                UserRepository userRepository,
                                ToolLogService toolLogService) {
        this.borrowRequestRepository = borrowRequestRepository;
        this.toolRepository = toolRepository;
        this.userRepository = userRepository;
        this.toolLogService = toolLogService;
    }

    public PageResponse<BorrowRequestResponse> getAllBorrowRequests(BorrowRequestStatus status, Long requesterId,
                                                                    Long toolId, LocalDate startDate, LocalDate endDate,
                                                                    int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<BorrowRequest> requestPage = borrowRequestRepository.search(status, requesterId, toolId, startDate, endDate, pageable);
        Page<BorrowRequestResponse> responsePage = requestPage.map(this::toResponse);

        return PageResponse.from(responsePage);
    }

    public BorrowRequestResponse getBorrowRequestById(Long id) {
        BorrowRequest borrowRequest = borrowRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("借用申请不存在"));
        return toResponse(borrowRequest);
    }

    public PageResponse<BorrowRequestResponse> getMyBorrowRequests(Long requesterId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<BorrowRequest> requestPage = borrowRequestRepository.findByRequesterId(requesterId, pageable);
        Page<BorrowRequestResponse> responsePage = requestPage.map(this::toResponse);
        return PageResponse.from(responsePage);
    }

    @Transactional
    public BorrowRequestResponse createBorrowRequest(CreateBorrowRequestRequest request, Long requesterId) {
        Tool tool = toolRepository.findById(request.getToolId())
                .orElseThrow(() -> new ResourceNotFoundException("工具不存在"));

        if (tool.getStatus() != ToolStatus.AVAILABLE) {
            throw new BadRequestException("该工具当前不可借用");
        }

        if (request.getStartDate().isAfter(request.getExpectedReturnDate())) {
            throw new BadRequestException("开始日期不能晚于预计归还日期");
        }

        BorrowRequest borrowRequest = new BorrowRequest();
        borrowRequest.setToolId(request.getToolId());
        borrowRequest.setRequesterId(requesterId);
        borrowRequest.setStartDate(request.getStartDate());
        borrowRequest.setExpectedReturnDate(request.getExpectedReturnDate());
        borrowRequest.setRemark(request.getRemark());
        borrowRequest.setStatus(BorrowRequestStatus.PENDING);

        BorrowRequest savedRequest = borrowRequestRepository.save(borrowRequest);
        return toResponse(savedRequest);
    }

    @Transactional
    public BorrowRequestResponse updateBorrowRequest(Long id, UpdateBorrowRequestRequest request, Long currentUserId) {
        BorrowRequest borrowRequest = borrowRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("借用申请不存在"));

        if (!borrowRequest.getRequesterId().equals(currentUserId)) {
            throw new BadRequestException("无权修改此借用申请");
        }

        if (borrowRequest.getStatus() != BorrowRequestStatus.PENDING) {
            throw new BadRequestException("只能修改待审核的申请");
        }

        if (request.getStartDate() != null) {
            borrowRequest.setStartDate(request.getStartDate());
        }
        if (request.getExpectedReturnDate() != null) {
            borrowRequest.setExpectedReturnDate(request.getExpectedReturnDate());
        }
        if (request.getRemark() != null) {
            borrowRequest.setRemark(request.getRemark());
        }

        if (borrowRequest.getStartDate().isAfter(borrowRequest.getExpectedReturnDate())) {
            throw new BadRequestException("开始日期不能晚于预计归还日期");
        }

        BorrowRequest savedRequest = borrowRequestRepository.save(borrowRequest);
        return toResponse(savedRequest);
    }

    @Transactional
    public BorrowRequestResponse updateBorrowStatus(Long id, UpdateBorrowStatusRequest request, Long currentUserId) {
        BorrowRequest borrowRequest = borrowRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("借用申请不存在"));

        Tool tool = toolRepository.findById(borrowRequest.getToolId())
                .orElseThrow(() -> new ResourceNotFoundException("工具不存在"));

        boolean isOwner = tool.getOwnerId().equals(currentUserId);
        boolean isRequester = borrowRequest.getRequesterId().equals(currentUserId);

        if (!isOwner && !isRequester) {
            throw new BadRequestException("无权修改此申请状态");
        }

        BorrowRequestStatus newStatus = request.getStatus();

        switch (newStatus) {
            case APPROVED:
                if (!isOwner) {
                    throw new BadRequestException("只有工具所有者可以批准申请");
                }
                if (borrowRequest.getStatus() != BorrowRequestStatus.PENDING) {
                    throw new BadRequestException("只能批准待审核的申请");
                }
                if (tool.getStatus() != ToolStatus.AVAILABLE) {
                    throw new BadRequestException("工具当前不可借用");
                }
                tool.setStatus(ToolStatus.BORROWED);
                toolRepository.save(tool);
                toolLogService.createLogInternal(tool.getId(), borrowRequest.getRequesterId(),
                        ToolLogAction.BORROW, "借用申请已批准");
                break;

            case REJECTED:
                if (!isOwner) {
                    throw new BadRequestException("只有工具所有者可以拒绝申请");
                }
                if (borrowRequest.getStatus() != BorrowRequestStatus.PENDING) {
                    throw new BadRequestException("只能拒绝待审核的申请");
                }
                break;

            case RETURNED:
                if (!isOwner) {
                    throw new BadRequestException("只有工具所有者可以确认归还");
                }
                if (borrowRequest.getStatus() != BorrowRequestStatus.APPROVED) {
                    throw new BadRequestException("只能归还已批准的申请");
                }
                tool.setStatus(ToolStatus.AVAILABLE);
                toolRepository.save(tool);
                borrowRequest.setActualReturnDate(LocalDate.now());
                toolLogService.createLogInternal(tool.getId(), borrowRequest.getRequesterId(),
                        ToolLogAction.RETURN, "工具已归还");
                break;

            default:
                throw new BadRequestException("无效的状态变更");
        }

        borrowRequest.setStatus(newStatus);
        BorrowRequest savedRequest = borrowRequestRepository.save(borrowRequest);
        return toResponse(savedRequest);
    }

    @Transactional
    public void deleteBorrowRequest(Long id, Long currentUserId) {
        BorrowRequest borrowRequest = borrowRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("借用申请不存在"));

        if (!borrowRequest.getRequesterId().equals(currentUserId)) {
            throw new BadRequestException("无权删除此借用申请");
        }

        if (borrowRequest.getStatus() != BorrowRequestStatus.PENDING) {
            throw new BadRequestException("只能删除待审核的申请");
        }

        borrowRequestRepository.delete(borrowRequest);
    }

    private BorrowRequestResponse toResponse(BorrowRequest borrowRequest) {
        BorrowRequestResponse response = new BorrowRequestResponse();
        response.setId(borrowRequest.getId());
        response.setToolId(borrowRequest.getToolId());
        response.setRequesterId(borrowRequest.getRequesterId());
        response.setStartDate(borrowRequest.getStartDate());
        response.setExpectedReturnDate(borrowRequest.getExpectedReturnDate());
        response.setActualReturnDate(borrowRequest.getActualReturnDate());
        response.setStatus(borrowRequest.getStatus());
        response.setRemark(borrowRequest.getRemark());
        response.setCreatedAt(borrowRequest.getCreatedAt());

        toolRepository.findById(borrowRequest.getToolId()).ifPresent(tool ->
                response.setToolName(tool.getName())
        );

        userRepository.findById(borrowRequest.getRequesterId()).ifPresent(user ->
                response.setRequesterName(user.getUsername())
        );

        return response;
    }
}
