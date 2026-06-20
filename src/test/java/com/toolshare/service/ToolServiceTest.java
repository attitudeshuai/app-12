package com.toolshare.service;

import com.toolshare.dto.tool.CompleteRepairRequest;
import com.toolshare.dto.tool.ReportToolRequest;
import com.toolshare.dto.tool.ToolResponse;
import com.toolshare.dto.tool.UpdateToolRequest;
import com.toolshare.dto.tool.UpdateToolStatusRequest;
import com.toolshare.entity.Tool;
import com.toolshare.entity.ToolBox;
import com.toolshare.entity.ToolLogAction;
import com.toolshare.entity.ToolStatus;
import com.toolshare.exception.BadRequestException;
import com.toolshare.exception.ResourceNotFoundException;
import com.toolshare.repository.ToolBoxRepository;
import com.toolshare.repository.ToolFavoriteRepository;
import com.toolshare.repository.ToolRepository;
import com.toolshare.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ToolServiceTest {

    @Mock
    private ToolRepository toolRepository;

    @Mock
    private ToolBoxRepository toolBoxRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ToolReviewService toolReviewService;

    @Mock
    private ToolFavoriteRepository toolFavoriteRepository;

    @Mock
    private ToolLogService toolLogService;

    @Mock
    private StatsService statsService;

    @InjectMocks
    private ToolService toolService;

    private Tool tool;
    private Long ownerId;
    private Long otherUserId;

    @BeforeEach
    void setUp() {
        ownerId = 100L;
        otherUserId = 999L;

        tool = new Tool();
        tool.setId(10L);
        tool.setBoxId(1L);
        tool.setName("测试工具");
        tool.setStatus(ToolStatus.AVAILABLE);
        tool.setOwnerId(ownerId);
    }

    @Test
    @DisplayName("普通用户通过updateToolStatus尝试设为禁用 - 应抛出异常")
    void updateToolStatus_RegularUserSetDisabled_ShouldThrow() {
        UpdateToolStatusRequest request = new UpdateToolStatusRequest();
        request.setStatus(ToolStatus.DISABLED);

        when(toolRepository.findById(10L)).thenReturn(Optional.of(tool));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                toolService.updateToolStatus(10L, request, ownerId)
        );

        assertTrue(ex.getMessage().contains("只有管理员可以禁用工具"));
        verify(toolRepository, never()).save(any(Tool.class));
    }

    @Test
    @DisplayName("普通用户通过updateTool尝试设为禁用 - 应抛出异常")
    void updateTool_RegularUserSetDisabled_ShouldThrow() {
        UpdateToolRequest request = new UpdateToolRequest();
        request.setStatus(ToolStatus.DISABLED);

        when(toolRepository.findById(10L)).thenReturn(Optional.of(tool));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                toolService.updateTool(10L, request, ownerId)
        );

        assertTrue(ex.getMessage().contains("只有管理员可以禁用工具"));
        verify(toolRepository, never()).save(any(Tool.class));
    }

    @Test
    @DisplayName("普通用户通过updateToolStatus可以设置其他合法状态 - MAINTENANCE")
    void updateToolStatus_RegularUserSetMaintenance_ShouldSucceed() {
        UpdateToolStatusRequest request = new UpdateToolStatusRequest();
        request.setStatus(ToolStatus.MAINTENANCE);

        when(toolRepository.findById(10L)).thenReturn(Optional.of(tool));
        when(toolRepository.save(any(Tool.class))).thenAnswer(inv -> inv.getArgument(0));
        when(toolBoxRepository.findById(any())).thenReturn(Optional.empty());
        when(userRepository.findById(any())).thenReturn(Optional.empty());
        when(toolReviewService.getAverageRatingByToolId(any())).thenReturn(0.0);
        when(toolReviewService.getReviewCountByToolId(any())).thenReturn(0L);
        when(statsService.getBorrowCountMap(any())).thenReturn(Collections.emptyMap());

        toolService.updateToolStatus(10L, request, ownerId);

        verify(toolRepository).save(any(Tool.class));
        assertEquals(ToolStatus.MAINTENANCE, tool.getStatus());
    }

    @Test
    @DisplayName("普通用户通过updateToolStatus可以设置其他合法状态 - BROKEN")
    void updateToolStatus_RegularUserSetBroken_ShouldSucceed() {
        UpdateToolStatusRequest request = new UpdateToolStatusRequest();
        request.setStatus(ToolStatus.BROKEN);

        when(toolRepository.findById(10L)).thenReturn(Optional.of(tool));
        when(toolRepository.save(any(Tool.class))).thenAnswer(inv -> inv.getArgument(0));
        when(toolBoxRepository.findById(any())).thenReturn(Optional.empty());
        when(userRepository.findById(any())).thenReturn(Optional.empty());
        when(toolReviewService.getAverageRatingByToolId(any())).thenReturn(0.0);
        when(toolReviewService.getReviewCountByToolId(any())).thenReturn(0L);
        when(statsService.getBorrowCountMap(any())).thenReturn(Collections.emptyMap());

        toolService.updateToolStatus(10L, request, ownerId);

        verify(toolRepository).save(any(Tool.class));
        assertEquals(ToolStatus.BROKEN, tool.getStatus());
    }

    @Test
    @DisplayName("非工具拥有者尝试修改状态 - 应抛出无权异常")
    void updateToolStatus_NonOwnerTryEdit_ShouldThrow() {
        UpdateToolStatusRequest request = new UpdateToolStatusRequest();
        request.setStatus(ToolStatus.MAINTENANCE);

        when(toolRepository.findById(10L)).thenReturn(Optional.of(tool));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                toolService.updateToolStatus(10L, request, otherUserId)
        );

        assertTrue(ex.getMessage().contains("无权修改此工具状态"));
        verify(toolRepository, never()).save(any(Tool.class));
    }

    @Test
    @DisplayName("管理员禁用工具 - BORROWED状态工具应被拒绝")
    void adminDisableTool_BorrowedStatus_ShouldThrow() {
        tool.setStatus(ToolStatus.BORROWED);

        when(toolRepository.findById(10L)).thenReturn(Optional.of(tool));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                toolService.adminDisableTool(10L)
        );

        assertTrue(ex.getMessage().contains("正在借用中，无法禁用"));
        verify(toolRepository, never()).save(any(Tool.class));
    }

    @Test
    @DisplayName("管理员禁用工具 - AVAILABLE工具可成功禁用")
    void adminDisableTool_AvailableTool_ShouldSucceed() {
        when(toolRepository.findById(10L)).thenReturn(Optional.of(tool));
        when(toolRepository.save(any(Tool.class))).thenAnswer(inv -> inv.getArgument(0));
        when(toolBoxRepository.findById(any())).thenReturn(Optional.empty());
        when(userRepository.findById(any())).thenReturn(Optional.empty());
        when(toolReviewService.getAverageRatingByToolId(any())).thenReturn(0.0);
        when(toolReviewService.getReviewCountByToolId(any())).thenReturn(0L);
        when(statsService.getBorrowCountMap(any())).thenReturn(Collections.emptyMap());

        toolService.adminDisableTool(10L);

        verify(toolRepository).save(tool);
        assertEquals(ToolStatus.DISABLED, tool.getStatus());
    }

    @Test
    @DisplayName("管理员启用工具 - 未被禁用的工具应被拒绝")
    void adminEnableTool_NotDisabled_ShouldThrow() {
        tool.setStatus(ToolStatus.AVAILABLE);

        when(toolRepository.findById(10L)).thenReturn(Optional.of(tool));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                toolService.adminEnableTool(10L)
        );

        assertTrue(ex.getMessage().contains("工具未被禁用"));
        verify(toolRepository, never()).save(any(Tool.class));
    }

    @Test
    @DisplayName("管理员启用工具 - 所属工具箱已停用应置为MAINTENANCE而非AVAILABLE")
    void adminEnableTool_InactiveToolBox_ShouldSetMaintenance() {
        tool.setStatus(ToolStatus.DISABLED);

        ToolBox inactiveBox = new ToolBox();
        inactiveBox.setId(1L);
        inactiveBox.setIsActive(false);

        when(toolRepository.findById(10L)).thenReturn(Optional.of(tool));
        when(toolBoxRepository.findById(1L)).thenReturn(Optional.of(inactiveBox));
        when(toolRepository.save(any(Tool.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(any())).thenReturn(Optional.empty());
        when(toolReviewService.getAverageRatingByToolId(any())).thenReturn(0.0);
        when(toolReviewService.getReviewCountByToolId(any())).thenReturn(0L);
        when(statsService.getBorrowCountMap(any())).thenReturn(Collections.emptyMap());

        toolService.adminEnableTool(10L);

        verify(toolRepository).save(tool);
        assertEquals(ToolStatus.MAINTENANCE, tool.getStatus());
    }

    @Test
    @DisplayName("管理员启用工具 - 所属工具箱活跃应置为AVAILABLE")
    void adminEnableTool_ActiveToolBox_ShouldSetAvailable() {
        tool.setStatus(ToolStatus.DISABLED);

        ToolBox activeBox = new ToolBox();
        activeBox.setId(1L);
        activeBox.setIsActive(true);

        when(toolRepository.findById(10L)).thenReturn(Optional.of(tool));
        when(toolBoxRepository.findById(1L)).thenReturn(Optional.of(activeBox));
        when(toolRepository.save(any(Tool.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(any())).thenReturn(Optional.empty());
        when(toolReviewService.getAverageRatingByToolId(any())).thenReturn(0.0);
        when(toolReviewService.getReviewCountByToolId(any())).thenReturn(0L);
        when(statsService.getBorrowCountMap(any())).thenReturn(Collections.emptyMap());

        toolService.adminEnableTool(10L);

        verify(toolRepository).save(tool);
        assertEquals(ToolStatus.AVAILABLE, tool.getStatus());
    }

    @Test
    @DisplayName("工具不存在时 - updateTool应抛出资源不存在异常")
    void updateTool_ToolNotFound_ShouldThrowNotFound() {
        UpdateToolRequest request = new UpdateToolRequest();

        when(toolRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                toolService.updateTool(999L, request, ownerId)
        );
    }

    @Test
    @DisplayName("管理员禁用工具 - 工具不存在应抛出资源不存在异常")
    void adminDisableTool_ToolNotFound_ShouldThrowNotFound() {
        when(toolRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                toolService.adminDisableTool(999L)
        );
    }

    @Test
    @DisplayName("报修工具 - 可用工具报修成功，状态变为MAINTENANCE")
    void reportTool_AvailableTool_ShouldSucceed() {
        ReportToolRequest request = new ReportToolRequest();
        request.setDescription("工具无法正常使用");

        when(toolRepository.findById(10L)).thenReturn(Optional.of(tool));
        when(toolRepository.save(any(Tool.class))).thenAnswer(inv -> inv.getArgument(0));
        when(toolBoxRepository.findById(any())).thenReturn(Optional.empty());
        when(userRepository.findById(any())).thenReturn(Optional.empty());
        when(toolReviewService.getAverageRatingByToolId(any())).thenReturn(0.0);
        when(toolReviewService.getReviewCountByToolId(any())).thenReturn(0L);
        when(statsService.getBorrowCountMap(any())).thenReturn(Collections.emptyMap());

        ToolResponse result = toolService.reportTool(10L, request, ownerId);

        verify(toolRepository).save(tool);
        assertEquals(ToolStatus.MAINTENANCE, tool.getStatus());
        verify(toolLogService).createLogInternal(eq(10L), eq(ownerId), eq(ToolLogAction.REPORT), eq("工具无法正常使用"));
    }

    @Test
    @DisplayName("报修工具 - 已在维修中的工具不应重复报修")
    void reportTool_AlreadyMaintenance_ShouldThrow() {
        tool.setStatus(ToolStatus.MAINTENANCE);
        ReportToolRequest request = new ReportToolRequest();
        request.setDescription("工具无法正常使用");

        when(toolRepository.findById(10L)).thenReturn(Optional.of(tool));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                toolService.reportTool(10L, request, ownerId)
        );

        assertTrue(ex.getMessage().contains("已在维修中"));
        verify(toolRepository, never()).save(any(Tool.class));
        verify(toolLogService, never()).createLogInternal(anyLong(), anyLong(), any(), anyString());
    }

    @Test
    @DisplayName("报修工具 - 已禁用的工具无法报修")
    void reportTool_DisabledTool_ShouldThrow() {
        tool.setStatus(ToolStatus.DISABLED);
        ReportToolRequest request = new ReportToolRequest();
        request.setDescription("工具无法正常使用");

        when(toolRepository.findById(10L)).thenReturn(Optional.of(tool));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                toolService.reportTool(10L, request, ownerId)
        );

        assertTrue(ex.getMessage().contains("已被禁用"));
        verify(toolRepository, never()).save(any(Tool.class));
        verify(toolLogService, never()).createLogInternal(anyLong(), anyLong(), any(), anyString());
    }

    @Test
    @DisplayName("报修工具 - 工具不存在应抛出资源不存在异常")
    void reportTool_ToolNotFound_ShouldThrowNotFound() {
        ReportToolRequest request = new ReportToolRequest();
        request.setDescription("工具无法正常使用");

        when(toolRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                toolService.reportTool(999L, request, ownerId)
        );
    }

    @Test
    @DisplayName("完成维修 - 工具所有者可完成维修，状态恢复AVAILABLE")
    void completeRepair_OwnerCanComplete_ShouldSucceed() {
        tool.setStatus(ToolStatus.MAINTENANCE);
        CompleteRepairRequest request = new CompleteRepairRequest();
        request.setDescription("已更换零件，维修完成");

        ToolBox activeBox = new ToolBox();
        activeBox.setId(1L);
        activeBox.setIsActive(true);

        when(toolRepository.findById(10L)).thenReturn(Optional.of(tool));
        when(toolBoxRepository.findById(1L)).thenReturn(Optional.of(activeBox));
        when(toolRepository.save(any(Tool.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(any())).thenReturn(Optional.empty());
        when(toolReviewService.getAverageRatingByToolId(any())).thenReturn(0.0);
        when(toolReviewService.getReviewCountByToolId(any())).thenReturn(0L);
        when(statsService.getBorrowCountMap(any())).thenReturn(Collections.emptyMap());

        ToolResponse result = toolService.completeRepair(10L, request, ownerId);

        verify(toolRepository).save(tool);
        assertEquals(ToolStatus.AVAILABLE, tool.getStatus());
        verify(toolLogService).createLogInternal(eq(10L), eq(ownerId), eq(ToolLogAction.REPAIR), eq("已更换零件，维修完成"));
    }

    @Test
    @DisplayName("完成维修 - 非工具所有者无法完成维修")
    void completeRepair_NonOwner_ShouldThrow() {
        tool.setStatus(ToolStatus.MAINTENANCE);
        CompleteRepairRequest request = new CompleteRepairRequest();
        request.setDescription("已更换零件，维修完成");

        when(toolRepository.findById(10L)).thenReturn(Optional.of(tool));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                toolService.completeRepair(10L, request, otherUserId)
        );

        assertTrue(ex.getMessage().contains("只有工具所有者可以完成维修"));
        verify(toolRepository, never()).save(any(Tool.class));
        verify(toolLogService, never()).createLogInternal(anyLong(), anyLong(), any(), anyString());
    }

    @Test
    @DisplayName("完成维修 - 不在维修状态的工具无法完成维修")
    void completeRepair_NotMaintenanceStatus_ShouldThrow() {
        tool.setStatus(ToolStatus.AVAILABLE);
        CompleteRepairRequest request = new CompleteRepairRequest();
        request.setDescription("已更换零件，维修完成");

        when(toolRepository.findById(10L)).thenReturn(Optional.of(tool));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                toolService.completeRepair(10L, request, ownerId)
        );

        assertTrue(ex.getMessage().contains("不在维修状态"));
        verify(toolRepository, never()).save(any(Tool.class));
        verify(toolLogService, never()).createLogInternal(anyLong(), anyLong(), any(), anyString());
    }

    @Test
    @DisplayName("完成维修 - 工具箱已停用时维修完成，状态保持MAINTENANCE")
    void completeRepair_InactiveToolBox_ShouldStayMaintenance() {
        tool.setStatus(ToolStatus.MAINTENANCE);
        CompleteRepairRequest request = new CompleteRepairRequest();
        request.setDescription("已更换零件，维修完成");

        ToolBox inactiveBox = new ToolBox();
        inactiveBox.setId(1L);
        inactiveBox.setIsActive(false);

        when(toolRepository.findById(10L)).thenReturn(Optional.of(tool));
        when(toolBoxRepository.findById(1L)).thenReturn(Optional.of(inactiveBox));
        when(toolRepository.save(any(Tool.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(any())).thenReturn(Optional.empty());
        when(toolReviewService.getAverageRatingByToolId(any())).thenReturn(0.0);
        when(toolReviewService.getReviewCountByToolId(any())).thenReturn(0L);
        when(statsService.getBorrowCountMap(any())).thenReturn(Collections.emptyMap());

        ToolResponse result = toolService.completeRepair(10L, request, ownerId);

        verify(toolRepository).save(tool);
        assertEquals(ToolStatus.MAINTENANCE, tool.getStatus());
        verify(toolLogService).createLogInternal(eq(10L), eq(ownerId), eq(ToolLogAction.REPAIR), eq("已更换零件，维修完成"));
    }

    @Test
    @DisplayName("完成维修 - 工具不存在应抛出资源不存在异常")
    void completeRepair_ToolNotFound_ShouldThrowNotFound() {
        CompleteRepairRequest request = new CompleteRepairRequest();
        request.setDescription("已更换零件，维修完成");

        when(toolRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                toolService.completeRepair(999L, request, ownerId)
        );
    }
}
