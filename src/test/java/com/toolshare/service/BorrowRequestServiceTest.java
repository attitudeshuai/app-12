package com.toolshare.service;

import com.toolshare.dto.borrowrequest.BorrowRequestResponse;
import com.toolshare.dto.borrowrequest.CreateBorrowRequestRequest;
import com.toolshare.dto.borrowrequest.UpdateBorrowRequestRequest;
import com.toolshare.dto.borrowrequest.UpdateBorrowStatusRequest;
import com.toolshare.entity.BorrowRequest;
import com.toolshare.entity.BorrowRequestStatus;
import com.toolshare.entity.NotificationType;
import com.toolshare.entity.OverdueRecord;
import com.toolshare.entity.Tool;
import com.toolshare.entity.ToolBox;
import com.toolshare.entity.ToolLogAction;
import com.toolshare.entity.ToolStatus;
import com.toolshare.entity.User;
import com.toolshare.exception.BadRequestException;
import com.toolshare.repository.BorrowRequestRepository;
import com.toolshare.repository.OverdueRecordRepository;
import com.toolshare.repository.ToolBoxRepository;
import com.toolshare.repository.ToolRepository;
import com.toolshare.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BorrowRequestServiceTest {

    @Mock
    private BorrowRequestRepository borrowRequestRepository;

    @Mock
    private ToolRepository toolRepository;

    @Mock
    private ToolBoxRepository toolBoxRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ToolLogService toolLogService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ToolReviewService toolReviewService;

    @Mock
    private OverdueRecordRepository overdueRecordRepository;

    @InjectMocks
    private BorrowRequestService borrowRequestService;

    private Tool tool;
    private ToolBox activeToolBox;
    private ToolBox inactiveToolBox;
    private BorrowRequest borrowRequest;
    private Long ownerId;
    private Long requesterId;

    @BeforeEach
    void setUp() {
        ownerId = 100L;
        requesterId = 200L;

        activeToolBox = new ToolBox();
        activeToolBox.setId(1L);
        activeToolBox.setName("活跃工具箱");
        activeToolBox.setIsActive(true);

        inactiveToolBox = new ToolBox();
        inactiveToolBox.setId(1L);
        inactiveToolBox.setName("停用工具箱");
        inactiveToolBox.setIsActive(false);

        tool = new Tool();
        tool.setId(10L);
        tool.setBoxId(1L);
        tool.setName("测试工具");
        tool.setStatus(ToolStatus.BORROWED);
        tool.setOwnerId(ownerId);

        borrowRequest = new BorrowRequest();
        borrowRequest.setId(1000L);
        borrowRequest.setToolId(10L);
        borrowRequest.setRequesterId(requesterId);
        borrowRequest.setStatus(BorrowRequestStatus.APPROVED);
        borrowRequest.setStartDate(LocalDate.now().minusDays(7));
        borrowRequest.setExpectedReturnDate(LocalDate.now().plusDays(3));

        User requester = new User();
        requester.setId(requesterId);
        requester.setUsername("借用人");
        when(userRepository.findById(requesterId)).thenReturn(Optional.of(requester));

        User owner = new User();
        owner.setId(ownerId);
        owner.setUsername("所有人");
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
    }

    @Test
    @DisplayName("工具归还 - 工具箱活跃时归还，工具状态应为AVAILABLE且无停用前标记")
    void updateBorrowStatus_Returned_ActiveToolBox_ShouldSetAvailable() {
        UpdateBorrowStatusRequest request = new UpdateBorrowStatusRequest();
        request.setStatus(BorrowRequestStatus.RETURNED);

        when(borrowRequestRepository.findById(1000L)).thenReturn(Optional.of(borrowRequest));
        when(toolRepository.findById(10L)).thenReturn(Optional.of(tool));
        when(toolBoxRepository.findById(1L)).thenReturn(Optional.of(activeToolBox));
        when(toolRepository.save(any(Tool.class))).thenAnswer(inv -> inv.getArgument(0));
        when(borrowRequestRepository.save(any(BorrowRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        when(toolReviewService.hasReviewed(any())).thenReturn(false);

        borrowRequestService.updateBorrowStatus(1000L, request, ownerId);

        ArgumentCaptor<Tool> toolCaptor = ArgumentCaptor.forClass(Tool.class);
        verify(toolRepository).save(toolCaptor.capture());

        Tool savedTool = toolCaptor.getValue();
        assertEquals(ToolStatus.AVAILABLE, savedTool.getStatus());
        assertNull(savedTool.getStatusBeforeBoxDeactivated());

        verify(toolLogService).createLogInternal(eq(10L), eq(requesterId),
                eq(ToolLogAction.RETURN), anyString());
        verify(notificationService).createNotification(
                eq(requesterId),
                eq(NotificationType.BORROW_RETURNED),
                anyString(),
                anyString(),
                eq(1000L)
        );
    }

    @Test
    @DisplayName("工具归还 - 工具箱停用时归还，工具状态应为MAINTENANCE且标记停用前为AVAILABLE")
    void updateBorrowStatus_Returned_InactiveToolBox_ShouldSetMaintenanceWithMarker() {
        UpdateBorrowStatusRequest request = new UpdateBorrowStatusRequest();
        request.setStatus(BorrowRequestStatus.RETURNED);

        when(borrowRequestRepository.findById(1000L)).thenReturn(Optional.of(borrowRequest));
        when(toolRepository.findById(10L)).thenReturn(Optional.of(tool));
        when(toolBoxRepository.findById(1L)).thenReturn(Optional.of(inactiveToolBox));
        when(toolRepository.save(any(Tool.class))).thenAnswer(inv -> inv.getArgument(0));
        when(borrowRequestRepository.save(any(BorrowRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        when(toolReviewService.hasReviewed(any())).thenReturn(false);

        borrowRequestService.updateBorrowStatus(1000L, request, ownerId);

        ArgumentCaptor<Tool> toolCaptor = ArgumentCaptor.forClass(Tool.class);
        verify(toolRepository).save(toolCaptor.capture());

        Tool savedTool = toolCaptor.getValue();
        assertEquals(ToolStatus.MAINTENANCE, savedTool.getStatus());
        assertEquals(ToolStatus.AVAILABLE, savedTool.getStatusBeforeBoxDeactivated(),
                "停用期间归还的工具应标记停用前状态为AVAILABLE，以便工具箱激活时恢复");
    }

    @Test
    @DisplayName("完整流程验证：借用→停用工具箱→归还→激活工具箱，工具应恢复为AVAILABLE")
    void fullBorrowCycle_ToolBoxDeactivatedDuringBorrow_ToolShouldRestoreOnActivation() {
        tool.setStatus(ToolStatus.BORROWED);

        UpdateBorrowStatusRequest returnRequest = new UpdateBorrowStatusRequest();
        returnRequest.setStatus(BorrowRequestStatus.RETURNED);

        when(borrowRequestRepository.findById(1000L)).thenReturn(Optional.of(borrowRequest));
        when(toolRepository.findById(10L)).thenReturn(Optional.of(tool));
        when(toolBoxRepository.findById(1L)).thenReturn(Optional.of(inactiveToolBox));
        when(toolRepository.save(any(Tool.class))).thenAnswer(inv -> inv.getArgument(0));
        when(borrowRequestRepository.save(any(BorrowRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        when(toolReviewService.hasReviewed(any())).thenReturn(false);

        borrowRequestService.updateBorrowStatus(1000L, returnRequest, ownerId);

        assertEquals(ToolStatus.MAINTENANCE, tool.getStatus());
        assertEquals(ToolStatus.AVAILABLE, tool.getStatusBeforeBoxDeactivated());

        reset(toolBoxRepository, toolRepository, toolReviewService, userRepository);
        ToolBox toolBoxForTest = new ToolBox();
        toolBoxForTest.setId(1L);
        toolBoxForTest.setIsActive(false);
        toolBoxForTest.setManagerId(ownerId);
        when(toolBoxRepository.findById(1L)).thenReturn(Optional.of(toolBoxForTest));
        when(toolRepository.findByBoxId(1L)).thenReturn(java.util.Arrays.asList(tool));
        when(toolBoxRepository.save(any(ToolBox.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(new User()));

        ToolBoxService testBoxService = new ToolBoxService(
                toolBoxRepository, toolRepository, userRepository
        );
        testBoxService.adminUpdateToolBoxActive(1L, true);

        assertEquals(ToolStatus.AVAILABLE, tool.getStatus(),
                "工具箱激活后，停用期间归还的工具应恢复为AVAILABLE");
        assertNull(tool.getStatusBeforeBoxDeactivated());
    }

    @Test
    @DisplayName("非工具所有者也非申请者，不能修改申请状态 - 应抛出异常")
    void updateBorrowStatus_Returned_NonOwnerAndNonRequester_ShouldThrow() {
        UpdateBorrowStatusRequest request = new UpdateBorrowStatusRequest();
        request.setStatus(BorrowRequestStatus.RETURNED);

        when(borrowRequestRepository.findById(1000L)).thenReturn(Optional.of(borrowRequest));
        when(toolRepository.findById(10L)).thenReturn(Optional.of(tool));

        Long nonOwnerNonRequesterId = 999L;
        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                borrowRequestService.updateBorrowStatus(1000L, request, nonOwnerNonRequesterId)
        );

        assertTrue(ex.getMessage().contains("无权修改此申请状态"));
        verify(toolRepository, never()).save(any(Tool.class));
    }

    @Test
    @DisplayName("只有已批准(APPROVED)状态的申请才能确认归还 - 应抛出异常")
    void updateBorrowStatus_Returned_NotApproved_ShouldThrow() {
        borrowRequest.setStatus(BorrowRequestStatus.PENDING);
        UpdateBorrowStatusRequest request = new UpdateBorrowStatusRequest();
        request.setStatus(BorrowRequestStatus.RETURNED);

        when(borrowRequestRepository.findById(1000L)).thenReturn(Optional.of(borrowRequest));
        when(toolRepository.findById(10L)).thenReturn(Optional.of(tool));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                borrowRequestService.updateBorrowStatus(1000L, request, ownerId)
        );

        assertTrue(ex.getMessage().contains("只能归还已批准的申请"));
        verify(toolRepository, never()).save(any(Tool.class));
    }

    @Test
    @DisplayName("归还时如果有逾期记录，应标记为已解决")
    void updateBorrowStatus_Returned_WithOverdue_ShouldResolveOverdue() {
        UpdateBorrowStatusRequest request = new UpdateBorrowStatusRequest();
        request.setStatus(BorrowRequestStatus.RETURNED);

        OverdueRecord overdueRecord = new OverdueRecord();
        overdueRecord.setResolved(false);

        when(borrowRequestRepository.findById(1000L)).thenReturn(Optional.of(borrowRequest));
        when(toolRepository.findById(10L)).thenReturn(Optional.of(tool));
        when(toolBoxRepository.findById(1L)).thenReturn(Optional.of(activeToolBox));
        when(toolRepository.save(any(Tool.class))).thenAnswer(inv -> inv.getArgument(0));
        when(borrowRequestRepository.save(any(BorrowRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        when(overdueRecordRepository.findByBorrowRequestId(1000L)).thenReturn(Optional.of(overdueRecord));
        when(toolReviewService.hasReviewed(any())).thenReturn(false);

        borrowRequestService.updateBorrowStatus(1000L, request, ownerId);

        assertTrue(overdueRecord.isResolved());
        assertNotNull(overdueRecord.getResolvedAt());
        verify(overdueRecordRepository).save(overdueRecord);
    }

    @Test
    @DisplayName("借用人本人不能确认归还 - 只有所有者可以")
    void updateBorrowStatus_Returned_RequesterTryConfirm_ShouldThrow() {
        UpdateBorrowStatusRequest request = new UpdateBorrowStatusRequest();
        request.setStatus(BorrowRequestStatus.RETURNED);

        when(borrowRequestRepository.findById(1000L)).thenReturn(Optional.of(borrowRequest));
        when(toolRepository.findById(10L)).thenReturn(Optional.of(tool));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                borrowRequestService.updateBorrowStatus(1000L, request, requesterId)
        );

        assertTrue(ex.getMessage().contains("只有工具所有者可以确认归还"));
        verify(toolRepository, never()).save(any(Tool.class));
    }

    @Test
    @DisplayName("工具箱停用时归还后再激活 - 原本就维护的工具不应被恢复")
    void updateBorrowStatus_Returned_MixedMaintenanceTools_OnlyMarkedShouldRestore() {
        UpdateBorrowStatusRequest returnRequest = new UpdateBorrowStatusRequest();
        returnRequest.setStatus(BorrowRequestStatus.RETURNED);

        when(borrowRequestRepository.findById(1000L)).thenReturn(Optional.of(borrowRequest));
        when(toolRepository.findById(10L)).thenReturn(Optional.of(tool));
        when(toolBoxRepository.findById(1L)).thenReturn(Optional.of(inactiveToolBox));
        when(toolRepository.save(any(Tool.class))).thenAnswer(inv -> inv.getArgument(0));
        when(borrowRequestRepository.save(any(BorrowRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        when(toolReviewService.hasReviewed(any())).thenReturn(false);

        borrowRequestService.updateBorrowStatus(1000L, returnRequest, ownerId);

        Tool returnedDuringInactive = tool;
        Tool originalMaintenance = new Tool();
        originalMaintenance.setId(11L);
        originalMaintenance.setBoxId(1L);
        originalMaintenance.setStatus(ToolStatus.MAINTENANCE);
        originalMaintenance.setStatusBeforeBoxDeactivated(null);

        reset(toolBoxRepository, toolRepository, userRepository);
        ToolBox toolBoxForTest = new ToolBox();
        toolBoxForTest.setId(1L);
        toolBoxForTest.setIsActive(false);
        toolBoxForTest.setManagerId(ownerId);
        when(toolBoxRepository.findById(1L)).thenReturn(Optional.of(toolBoxForTest));
        when(toolRepository.findByBoxId(1L)).thenReturn(java.util.Arrays.asList(returnedDuringInactive, originalMaintenance));
        when(toolBoxRepository.save(any(ToolBox.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(new User()));

        ToolBoxService testBoxService = new ToolBoxService(
                toolBoxRepository, toolRepository, userRepository
        );
        testBoxService.adminUpdateToolBoxActive(1L, true);

        assertEquals(ToolStatus.AVAILABLE, returnedDuringInactive.getStatus(),
                "停用期间归还的工具应恢复为AVAILABLE");
        assertNull(returnedDuringInactive.getStatusBeforeBoxDeactivated());

        assertEquals(ToolStatus.MAINTENANCE, originalMaintenance.getStatus(),
                "原本就维护的工具不应被恢复为AVAILABLE");
        assertNull(originalMaintenance.getStatusBeforeBoxDeactivated());
    }

    @Test
    @DisplayName("创建预约 - 时段与已存在的PENDING申请冲突 - 应抛出异常")
    void createBorrowRequest_ConflictWithPending_ShouldThrow() {
        tool.setStatus(ToolStatus.AVAILABLE);
        LocalDate startDate = LocalDate.now().plusDays(1);
        LocalDate endDate = LocalDate.now().plusDays(5);

        CreateBorrowRequestRequest request = new CreateBorrowRequestRequest();
        request.setToolId(10L);
        request.setStartDate(startDate);
        request.setExpectedReturnDate(endDate);

        BorrowRequest existingRequest = new BorrowRequest();
        existingRequest.setId(2000L);
        existingRequest.setToolId(10L);
        existingRequest.setStatus(BorrowRequestStatus.PENDING);
        existingRequest.setStartDate(startDate.minusDays(1));
        existingRequest.setExpectedReturnDate(endDate.plusDays(1));
        existingRequest.setRequesterId(300L);

        when(toolRepository.findById(10L)).thenReturn(Optional.of(tool));
        when(toolBoxRepository.findById(1L)).thenReturn(Optional.of(activeToolBox));
        when(borrowRequestRepository.findConflictingBorrows(
                eq(10L), anyList(), eq(startDate), eq(endDate), isNull()))
                .thenReturn(java.util.Arrays.asList(existingRequest));
        when(userRepository.findById(300L)).thenReturn(Optional.of(new User()));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                borrowRequestService.createBorrowRequest(request, requesterId));

        assertTrue(ex.getMessage().contains("时段已被"));
        assertTrue(ex.getMessage().contains("待审核"));
        verify(borrowRequestRepository, never()).save(any(BorrowRequest.class));
    }

    @Test
    @DisplayName("创建预约 - 时段与已存在的APPROVED申请冲突 - 应抛出异常")
    void createBorrowRequest_ConflictWithApproved_ShouldThrow() {
        tool.setStatus(ToolStatus.AVAILABLE);
        LocalDate startDate = LocalDate.now().plusDays(1);
        LocalDate endDate = LocalDate.now().plusDays(5);

        CreateBorrowRequestRequest request = new CreateBorrowRequestRequest();
        request.setToolId(10L);
        request.setStartDate(startDate);
        request.setExpectedReturnDate(endDate);

        BorrowRequest existingRequest = new BorrowRequest();
        existingRequest.setId(2000L);
        existingRequest.setToolId(10L);
        existingRequest.setStatus(BorrowRequestStatus.APPROVED);
        existingRequest.setStartDate(startDate);
        existingRequest.setExpectedReturnDate(endDate);
        existingRequest.setRequesterId(300L);

        when(toolRepository.findById(10L)).thenReturn(Optional.of(tool));
        when(toolBoxRepository.findById(1L)).thenReturn(Optional.of(activeToolBox));
        when(borrowRequestRepository.findConflictingBorrows(
                eq(10L), anyList(), eq(startDate), eq(endDate), isNull()))
                .thenReturn(java.util.Arrays.asList(existingRequest));
        when(userRepository.findById(300L)).thenReturn(Optional.of(new User()));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                borrowRequestService.createBorrowRequest(request, requesterId));

        assertTrue(ex.getMessage().contains("已批准"));
        verify(borrowRequestRepository, never()).save(any(BorrowRequest.class));
    }

    @Test
    @DisplayName("创建预约 - 时段部分重叠 - 应检测到冲突")
    void createBorrowRequest_PartialOverlap_ShouldThrow() {
        tool.setStatus(ToolStatus.AVAILABLE);
        LocalDate existingStart = LocalDate.now().plusDays(1);
        LocalDate existingEnd = LocalDate.now().plusDays(5);
        LocalDate newStart = LocalDate.now().plusDays(3);
        LocalDate newEnd = LocalDate.now().plusDays(7);

        CreateBorrowRequestRequest request = new CreateBorrowRequestRequest();
        request.setToolId(10L);
        request.setStartDate(newStart);
        request.setExpectedReturnDate(newEnd);

        BorrowRequest existingRequest = new BorrowRequest();
        existingRequest.setId(2000L);
        existingRequest.setToolId(10L);
        existingRequest.setStatus(BorrowRequestStatus.APPROVED);
        existingRequest.setStartDate(existingStart);
        existingRequest.setExpectedReturnDate(existingEnd);
        existingRequest.setRequesterId(300L);

        when(toolRepository.findById(10L)).thenReturn(Optional.of(tool));
        when(toolBoxRepository.findById(1L)).thenReturn(Optional.of(activeToolBox));
        when(borrowRequestRepository.findConflictingBorrows(
                eq(10L), anyList(), eq(newStart), eq(newEnd), isNull()))
                .thenReturn(java.util.Arrays.asList(existingRequest));
        when(userRepository.findById(300L)).thenReturn(Optional.of(new User()));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                borrowRequestService.createBorrowRequest(request, requesterId));

        assertNotNull(ex);
    }

    @Test
    @DisplayName("创建预约 - 无冲突 - 应成功创建")
    void createBorrowRequest_NoConflict_ShouldSucceed() {
        tool.setStatus(ToolStatus.AVAILABLE);
        LocalDate startDate = LocalDate.now().plusDays(1);
        LocalDate endDate = LocalDate.now().plusDays(5);

        CreateBorrowRequestRequest request = new CreateBorrowRequestRequest();
        request.setToolId(10L);
        request.setStartDate(startDate);
        request.setExpectedReturnDate(endDate);

        when(toolRepository.findById(10L)).thenReturn(Optional.of(tool));
        when(toolBoxRepository.findById(1L)).thenReturn(Optional.of(activeToolBox));
        when(borrowRequestRepository.findConflictingBorrows(
                eq(10L), anyList(), eq(startDate), eq(endDate), isNull()))
                .thenReturn(new ArrayList<>());
        when(borrowRequestRepository.save(any(BorrowRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        BorrowRequestResponse response = borrowRequestService.createBorrowRequest(request, requesterId);

        assertNotNull(response);
        verify(borrowRequestRepository).save(any(BorrowRequest.class));
    }

    @Test
    @DisplayName("更新预约 - 时段与其他申请冲突 - 应抛出异常")
    void updateBorrowRequest_ConflictWithOther_ShouldThrow() {
        tool.setStatus(ToolStatus.AVAILABLE);
        LocalDate newStart = LocalDate.now().plusDays(10);
        LocalDate newEnd = LocalDate.now().plusDays(15);

        BorrowRequest existingRequest = new BorrowRequest();
        existingRequest.setId(1000L);
        existingRequest.setToolId(10L);
        existingRequest.setStatus(BorrowRequestStatus.PENDING);
        existingRequest.setRequesterId(requesterId);
        existingRequest.setStartDate(LocalDate.now().plusDays(1));
        existingRequest.setExpectedReturnDate(LocalDate.now().plusDays(5));

        BorrowRequest otherRequest = new BorrowRequest();
        otherRequest.setId(2000L);
        otherRequest.setToolId(10L);
        otherRequest.setStatus(BorrowRequestStatus.APPROVED);
        otherRequest.setStartDate(newStart);
        otherRequest.setExpectedReturnDate(newEnd);
        otherRequest.setRequesterId(300L);

        UpdateBorrowRequestRequest updateRequest = new UpdateBorrowRequestRequest();
        updateRequest.setStartDate(newStart);
        updateRequest.setExpectedReturnDate(newEnd);

        when(borrowRequestRepository.findById(1000L)).thenReturn(Optional.of(existingRequest));
        when(toolRepository.findById(10L)).thenReturn(Optional.of(tool));
        when(borrowRequestRepository.findConflictingBorrows(
                eq(10L), anyList(), eq(newStart), eq(newEnd), eq(1000L)))
                .thenReturn(java.util.Arrays.asList(otherRequest));
        when(userRepository.findById(300L)).thenReturn(Optional.of(new User()));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                borrowRequestService.updateBorrowRequest(1000L, updateRequest, requesterId));

        assertTrue(ex.getMessage().contains("时段已被"));
        verify(borrowRequestRepository, never()).save(any(BorrowRequest.class));
    }

    @Test
    @DisplayName("更新预约 - 仅修改备注不修改日期 - 不检测冲突")
    void updateBorrowRequest_OnlyRemark_ShouldNotCheckConflict() {
        tool.setStatus(ToolStatus.AVAILABLE);
        LocalDate originalStart = LocalDate.now().plusDays(1);
        LocalDate originalEnd = LocalDate.now().plusDays(5);

        BorrowRequest existingRequest = new BorrowRequest();
        existingRequest.setId(1000L);
        existingRequest.setToolId(10L);
        existingRequest.setStatus(BorrowRequestStatus.PENDING);
        existingRequest.setRequesterId(requesterId);
        existingRequest.setStartDate(originalStart);
        existingRequest.setExpectedReturnDate(originalEnd);

        UpdateBorrowRequestRequest updateRequest = new UpdateBorrowRequestRequest();
        updateRequest.setRemark("新备注");

        when(borrowRequestRepository.findById(1000L)).thenReturn(Optional.of(existingRequest));
        when(toolRepository.findById(10L)).thenReturn(Optional.of(tool));
        when(borrowRequestRepository.save(any(BorrowRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        when(toolReviewService.hasReviewed(any())).thenReturn(false);

        borrowRequestService.updateBorrowRequest(1000L, updateRequest, requesterId);

        verify(borrowRequestRepository, never()).findConflictingBorrows(
                any(), anyList(), any(), any(), any());
        verify(borrowRequestRepository).save(any(BorrowRequest.class));
    }

    @Test
    @DisplayName("更新预约 - 与自身冲突 - 应被排除检测")
    void updateBorrowRequest_SelfConflict_ShouldBeExcluded() {
        tool.setStatus(ToolStatus.AVAILABLE);
        LocalDate newStart = LocalDate.now().plusDays(2);
        LocalDate newEnd = LocalDate.now().plusDays(6);

        BorrowRequest existingRequest = new BorrowRequest();
        existingRequest.setId(1000L);
        existingRequest.setToolId(10L);
        existingRequest.setStatus(BorrowRequestStatus.PENDING);
        existingRequest.setRequesterId(requesterId);
        existingRequest.setStartDate(LocalDate.now().plusDays(1));
        existingRequest.setExpectedReturnDate(LocalDate.now().plusDays(5));

        UpdateBorrowRequestRequest updateRequest = new UpdateBorrowRequestRequest();
        updateRequest.setStartDate(newStart);
        updateRequest.setExpectedReturnDate(newEnd);

        when(borrowRequestRepository.findById(1000L)).thenReturn(Optional.of(existingRequest));
        when(toolRepository.findById(10L)).thenReturn(Optional.of(tool));
        when(borrowRequestRepository.findConflictingBorrows(
                eq(10L), anyList(), eq(newStart), eq(newEnd), eq(1000L)))
                .thenReturn(new ArrayList<>());
        when(borrowRequestRepository.save(any(BorrowRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        when(toolReviewService.hasReviewed(any())).thenReturn(false);

        BorrowRequestResponse response = borrowRequestService.updateBorrowRequest(1000L, updateRequest, requesterId);

        assertNotNull(response);
        verify(borrowRequestRepository).save(any(BorrowRequest.class));
    }
}
