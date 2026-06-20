package com.toolshare.service;

import com.toolshare.entity.Tool;
import com.toolshare.entity.ToolBox;
import com.toolshare.entity.ToolStatus;
import com.toolshare.exception.BadRequestException;
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

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ToolBoxServiceTest {

    @Mock
    private ToolBoxRepository toolBoxRepository;

    @Mock
    private ToolRepository toolRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ToolBoxService toolBoxService;

    private ToolBox toolBox;
    private Tool availableTool;
    private Tool borrowedTool;
    private Tool originalMaintenanceTool;

    @BeforeEach
    void setUp() {
        toolBox = new ToolBox();
        toolBox.setId(1L);
        toolBox.setName("测试工具箱");
        toolBox.setLocation("测试地点");
        toolBox.setManagerId(1L);
        toolBox.setIsActive(true);

        availableTool = new Tool();
        availableTool.setId(10L);
        availableTool.setBoxId(1L);
        availableTool.setName("可用工具");
        availableTool.setStatus(ToolStatus.AVAILABLE);

        borrowedTool = new Tool();
        borrowedTool.setId(11L);
        borrowedTool.setBoxId(1L);
        borrowedTool.setName("借出工具");
        borrowedTool.setStatus(ToolStatus.BORROWED);

        originalMaintenanceTool = new Tool();
        originalMaintenanceTool.setId(12L);
        originalMaintenanceTool.setBoxId(1L);
        originalMaintenanceTool.setName("原本就处于维护的工具");
        originalMaintenanceTool.setStatus(ToolStatus.MAINTENANCE);
    }

    @Test
    @DisplayName("停用工具箱 - 应将AVAILABLE工具置为MAINTENANCE并记录原始状态")
    void adminUpdateToolBoxActive_Deactivate_ShouldMarkAvailableToolsAsMaintenance() {
        when(toolBoxRepository.findById(1L)).thenReturn(Optional.of(toolBox));
        when(toolRepository.findByBoxId(1L))
                .thenReturn(Arrays.asList(availableTool, borrowedTool, originalMaintenanceTool));
        when(toolBoxRepository.save(any(ToolBox.class))).thenReturn(toolBox);

        toolBoxService.adminUpdateToolBoxActive(1L, false);

        ArgumentCaptor<Tool> toolCaptor = ArgumentCaptor.forClass(Tool.class);
        verify(toolRepository, times(1)).save(toolCaptor.capture());

        Tool savedTool = toolCaptor.getValue();
        assertEquals(10L, savedTool.getId());
        assertEquals(ToolStatus.MAINTENANCE, savedTool.getStatus());
        assertEquals(ToolStatus.AVAILABLE, savedTool.getStatusBeforeBoxDeactivated());

        assertEquals(ToolStatus.BORROWED, borrowedTool.getStatus());
        assertNull(borrowedTool.getStatusBeforeBoxDeactivated());

        assertEquals(ToolStatus.MAINTENANCE, originalMaintenanceTool.getStatus());
        assertNull(originalMaintenanceTool.getStatusBeforeBoxDeactivated());

        assertFalse(toolBox.getIsActive());
        verify(toolBoxRepository).save(toolBox);
    }

    @Test
    @DisplayName("停用工具箱 - 已经停用应抛出异常")
    void adminUpdateToolBoxActive_Deactivate_WhenAlreadyDeactivated_ShouldThrow() {
        toolBox.setIsActive(false);
        when(toolBoxRepository.findById(1L)).thenReturn(Optional.of(toolBox));

        assertThrows(BadRequestException.class, () ->
                toolBoxService.adminUpdateToolBoxActive(1L, false)
        );

        verify(toolRepository, never()).save(any(Tool.class));
        verify(toolBoxRepository, never()).save(any(ToolBox.class));
    }

    @Test
    @DisplayName("激活工具箱 - 只恢复因本次停用被置为维护的工具")
    void adminUpdateToolBoxActive_Activate_ShouldOnlyRestoreToolsMarkedAsDeactivatedByBox() {
        toolBox.setIsActive(false);

        Tool restoredTool = new Tool();
        restoredTool.setId(10L);
        restoredTool.setBoxId(1L);
        restoredTool.setName("可恢复工具");
        restoredTool.setStatus(ToolStatus.MAINTENANCE);
        restoredTool.setStatusBeforeBoxDeactivated(ToolStatus.AVAILABLE);

        Tool notRestoredTool = new Tool();
        notRestoredTool.setId(12L);
        notRestoredTool.setBoxId(1L);
        notRestoredTool.setName("原本就维护的工具");
        notRestoredTool.setStatus(ToolStatus.MAINTENANCE);
        notRestoredTool.setStatusBeforeBoxDeactivated(null);

        when(toolBoxRepository.findById(1L)).thenReturn(Optional.of(toolBox));
        when(toolRepository.findByBoxId(1L))
                .thenReturn(Arrays.asList(restoredTool, notRestoredTool));
        when(toolBoxRepository.save(any(ToolBox.class))).thenReturn(toolBox);

        toolBoxService.adminUpdateToolBoxActive(1L, true);

        ArgumentCaptor<Tool> toolCaptor = ArgumentCaptor.forClass(Tool.class);
        verify(toolRepository, times(1)).save(toolCaptor.capture());

        Tool savedTool = toolCaptor.getValue();
        assertEquals(10L, savedTool.getId());
        assertEquals(ToolStatus.AVAILABLE, savedTool.getStatus());
        assertNull(savedTool.getStatusBeforeBoxDeactivated());

        assertEquals(ToolStatus.MAINTENANCE, notRestoredTool.getStatus());
        assertNull(notRestoredTool.getStatusBeforeBoxDeactivated());

        assertTrue(toolBox.getIsActive());
        verify(toolBoxRepository).save(toolBox);
    }

    @Test
    @DisplayName("激活工具箱 - 已经激活应抛出异常")
    void adminUpdateToolBoxActive_Activate_WhenAlreadyActive_ShouldThrow() {
        toolBox.setIsActive(true);
        when(toolBoxRepository.findById(1L)).thenReturn(Optional.of(toolBox));

        assertThrows(BadRequestException.class, () ->
                toolBoxService.adminUpdateToolBoxActive(1L, true)
        );

        verify(toolRepository, never()).save(any(Tool.class));
        verify(toolBoxRepository, never()).save(any(ToolBox.class));
    }

    @Test
    @DisplayName("停用-激活完整循环 - 维护状态工具在循环后不被误恢复为可用")
    void adminUpdateToolBoxActive_FullDeactivateActivateCycle_ShouldNotAffectOriginalMaintenanceTools() {
        when(toolBoxRepository.save(any(ToolBox.class))).thenReturn(toolBox);

        when(toolBoxRepository.findById(1L)).thenReturn(Optional.of(toolBox));
        when(toolRepository.findByBoxId(1L))
                .thenReturn(Arrays.asList(availableTool, originalMaintenanceTool));

        toolBoxService.adminUpdateToolBoxActive(1L, false);

        assertEquals(ToolStatus.MAINTENANCE, availableTool.getStatus());
        assertEquals(ToolStatus.AVAILABLE, availableTool.getStatusBeforeBoxDeactivated());
        assertEquals(ToolStatus.MAINTENANCE, originalMaintenanceTool.getStatus());
        assertNull(originalMaintenanceTool.getStatusBeforeBoxDeactivated());

        reset(toolBoxRepository);
        when(toolBoxRepository.findById(1L)).thenReturn(Optional.of(toolBox));
        when(toolBoxRepository.save(any(ToolBox.class))).thenReturn(toolBox);

        toolBoxService.adminUpdateToolBoxActive(1L, true);

        assertEquals(ToolStatus.AVAILABLE, availableTool.getStatus());
        assertNull(availableTool.getStatusBeforeBoxDeactivated());
        assertEquals(ToolStatus.MAINTENANCE, originalMaintenanceTool.getStatus());
        assertNull(originalMaintenanceTool.getStatusBeforeBoxDeactivated());
    }
}
