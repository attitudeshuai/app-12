package com.toolshare.service;

import com.toolshare.dto.PageResponse;
import com.toolshare.dto.tool.CreateToolRequest;
import com.toolshare.dto.tool.ToolResponse;
import com.toolshare.dto.tool.UpdateToolRequest;
import com.toolshare.dto.tool.UpdateToolStatusRequest;
import com.toolshare.entity.Tool;
import com.toolshare.entity.ToolBox;
import com.toolshare.entity.ToolStatus;
import com.toolshare.entity.User;
import com.toolshare.exception.BadRequestException;
import com.toolshare.exception.ResourceNotFoundException;
import com.toolshare.repository.ToolBoxRepository;
import com.toolshare.repository.ToolFavoriteRepository;
import com.toolshare.repository.ToolRepository;
import com.toolshare.repository.UserRepository;
import com.toolshare.util.SecurityUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ToolService {

    private final ToolRepository toolRepository;
    private final ToolBoxRepository toolBoxRepository;
    private final UserRepository userRepository;
    private final ToolReviewService toolReviewService;
    private final ToolFavoriteRepository toolFavoriteRepository;

    public ToolService(ToolRepository toolRepository, ToolBoxRepository toolBoxRepository, UserRepository userRepository,
                       ToolReviewService toolReviewService, ToolFavoriteRepository toolFavoriteRepository) {
        this.toolRepository = toolRepository;
        this.toolBoxRepository = toolBoxRepository;
        this.userRepository = userRepository;
        this.toolReviewService = toolReviewService;
        this.toolFavoriteRepository = toolFavoriteRepository;
    }

    public PageResponse<ToolResponse> getAllTools(String keyword, String category, ToolStatus status, Long boxId,
                                                   int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Tool> toolPage = toolRepository.search(keyword, category, status, boxId, pageable);
        List<ToolResponse> responseList = toResponseList(toolPage.getContent());
        return PageResponse.of(responseList, toolPage.getTotalElements(), toolPage.getNumber(), toolPage.getSize());
    }

    public ToolResponse getToolById(Long id) {
        Tool tool = toolRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("工具不存在"));
        return toResponse(tool);
    }

    public PageResponse<ToolResponse> getMyTools(Long ownerId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Tool> toolPage = toolRepository.findByOwnerId(ownerId, pageable);
        List<ToolResponse> responseList = toResponseList(toolPage.getContent());
        return PageResponse.of(responseList, toolPage.getTotalElements(), toolPage.getNumber(), toolPage.getSize());
    }

    @Transactional
    public ToolResponse createTool(CreateToolRequest request, Long ownerId) {
        ToolBox toolBox = toolBoxRepository.findById(request.getBoxId())
                .orElseThrow(() -> new ResourceNotFoundException("工具箱不存在"));

        Tool tool = new Tool();
        tool.setBoxId(request.getBoxId());
        tool.setName(request.getName());
        tool.setCategory(request.getCategory());
        tool.setDescription(request.getDescription());
        tool.setImage(request.getImage());
        tool.setPurchaseDate(request.getPurchaseDate());
        tool.setOwnerId(ownerId);
        tool.setStatus(ToolStatus.AVAILABLE);

        Tool savedTool = toolRepository.save(tool);
        return toResponse(savedTool);
    }

    @Transactional
    public ToolResponse updateTool(Long id, UpdateToolRequest request, Long currentUserId) {
        Tool tool = toolRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("工具不存在"));

        if (!tool.getOwnerId().equals(currentUserId)) {
            throw new BadRequestException("无权修改此工具");
        }

        if (request.getBoxId() != null) {
            if (!toolBoxRepository.existsById(request.getBoxId())) {
                throw new ResourceNotFoundException("工具箱不存在");
            }
            tool.setBoxId(request.getBoxId());
        }
        if (request.getName() != null) {
            tool.setName(request.getName());
        }
        if (request.getCategory() != null) {
            tool.setCategory(request.getCategory());
        }
        if (request.getDescription() != null) {
            tool.setDescription(request.getDescription());
        }
        if (request.getImage() != null) {
            tool.setImage(request.getImage());
        }
        if (request.getPurchaseDate() != null) {
            tool.setPurchaseDate(request.getPurchaseDate());
        }
        if (request.getStatus() != null) {
            if (request.getStatus() == com.toolshare.entity.ToolStatus.DISABLED) {
                throw new BadRequestException("只有管理员可以禁用工具");
            }
            tool.setStatus(request.getStatus());
        }

        Tool savedTool = toolRepository.save(tool);
        return toResponse(savedTool);
    }

    @Transactional
    public ToolResponse updateToolStatus(Long id, UpdateToolStatusRequest request, Long currentUserId) {
        Tool tool = toolRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("工具不存在"));

        if (!tool.getOwnerId().equals(currentUserId)) {
            throw new BadRequestException("无权修改此工具状态");
        }

        if (request.getStatus() == com.toolshare.entity.ToolStatus.DISABLED) {
            throw new BadRequestException("只有管理员可以禁用工具");
        }

        tool.setStatus(request.getStatus());
        Tool savedTool = toolRepository.save(tool);
        return toResponse(savedTool);
    }

    @Transactional
    public void deleteTool(Long id, Long currentUserId) {
        Tool tool = toolRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("工具不存在"));

        if (!tool.getOwnerId().equals(currentUserId)) {
            throw new BadRequestException("无权删除此工具");
        }

        toolRepository.delete(tool);
    }

    private List<ToolResponse> toResponseList(List<Tool> tools) {
        if (tools == null || tools.isEmpty()) {
            return new ArrayList<>();
        }

        Set<Long> toolIds = tools.stream().map(Tool::getId).collect(Collectors.toSet());
        Set<Long> boxIds = tools.stream().map(Tool::getBoxId).collect(Collectors.toSet());
        Set<Long> ownerIds = tools.stream().map(Tool::getOwnerId).collect(Collectors.toSet());

        Map<Long, Double> averageRatingMap = toolReviewService.getAverageRatingMapByToolIds(new ArrayList<>(toolIds));
        Map<Long, Long> reviewCountMap = toolReviewService.getReviewCountMapByToolIds(new ArrayList<>(toolIds));
        Map<Long, String> boxNameMap = new HashMap<>();
        if (!boxIds.isEmpty()) {
            toolBoxRepository.findAllById(boxIds).forEach(tb -> boxNameMap.put(tb.getId(), tb.getName()));
        }
        Map<Long, String> ownerNameMap = new HashMap<>();
        if (!ownerIds.isEmpty()) {
            userRepository.findAllById(ownerIds).forEach(u -> ownerNameMap.put(u.getId(), u.getUsername()));
        }

        Long currentUserId = SecurityUtil.getCurrentUserId();
        Set<Long> favoritedToolIds = new HashSet<>();
        if (currentUserId != null && !toolIds.isEmpty()) {
            favoritedToolIds = new HashSet<>(toolFavoriteRepository.findFavoritedToolIdsByUserIdAndToolIds(currentUserId, new ArrayList<>(toolIds)));
        }

        Map<Long, Long> favoriteCountMap = new HashMap<>();
        if (!toolIds.isEmpty()) {
            for (Long toolId : toolIds) {
                favoriteCountMap.put(toolId, toolFavoriteRepository.countByToolId(toolId));
            }
        }

        List<ToolResponse> responses = new ArrayList<>();
        for (Tool tool : tools) {
            ToolResponse response = new ToolResponse();
            response.setId(tool.getId());
            response.setBoxId(tool.getBoxId());
            response.setName(tool.getName());
            response.setCategory(tool.getCategory());
            response.setStatus(tool.getStatus());
            response.setDescription(tool.getDescription());
            response.setImage(tool.getImage());
            response.setPurchaseDate(tool.getPurchaseDate());
            response.setOwnerId(tool.getOwnerId());
            response.setCreatedAt(tool.getCreatedAt());

            response.setBoxName(boxNameMap.get(tool.getBoxId()));
            response.setOwnerName(ownerNameMap.get(tool.getOwnerId()));
            response.setAverageRating(averageRatingMap.get(tool.getId()));
            response.setReviewCount(reviewCountMap.get(tool.getId()));
            response.setIsFavorited(currentUserId != null && favoritedToolIds.contains(tool.getId()));
            response.setFavoriteCount(favoriteCountMap.get(tool.getId()));

            responses.add(response);
        }
        return responses;
    }

    private ToolResponse toResponse(Tool tool) {
        ToolResponse response = new ToolResponse();
        response.setId(tool.getId());
        response.setBoxId(tool.getBoxId());
        response.setName(tool.getName());
        response.setCategory(tool.getCategory());
        response.setStatus(tool.getStatus());
        response.setDescription(tool.getDescription());
        response.setImage(tool.getImage());
        response.setPurchaseDate(tool.getPurchaseDate());
        response.setOwnerId(tool.getOwnerId());
        response.setCreatedAt(tool.getCreatedAt());

        toolBoxRepository.findById(tool.getBoxId()).ifPresent(toolBox ->
                response.setBoxName(toolBox.getName())
        );

        userRepository.findById(tool.getOwnerId()).ifPresent(user ->
                response.setOwnerName(user.getUsername())
        );

        response.setAverageRating(toolReviewService.getAverageRatingByToolId(tool.getId()));
        response.setReviewCount(toolReviewService.getReviewCountByToolId(tool.getId()));

        Long currentUserId = SecurityUtil.getCurrentUserId();
        if (currentUserId != null) {
            response.setIsFavorited(toolFavoriteRepository.existsByUserIdAndToolId(currentUserId, tool.getId()));
        } else {
            response.setIsFavorited(false);
        }
        response.setFavoriteCount(toolFavoriteRepository.countByToolId(tool.getId()));

        return response;
    }

    @Transactional
    public ToolResponse adminDisableTool(Long id) {
        Tool tool = toolRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("工具不存在"));

        if (tool.getStatus() == ToolStatus.BORROWED) {
            throw new BadRequestException("工具正在借用中，无法禁用");
        }

        tool.setStatus(ToolStatus.DISABLED);
        Tool savedTool = toolRepository.save(tool);
        return toResponse(savedTool);
    }

    @Transactional
    public ToolResponse adminEnableTool(Long id) {
        Tool tool = toolRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("工具不存在"));

        if (tool.getStatus() != ToolStatus.DISABLED) {
            throw new BadRequestException("工具未被禁用");
        }

        ToolBox toolBox = toolBoxRepository.findById(tool.getBoxId()).orElse(null);
        if (toolBox != null && !Boolean.TRUE.equals(toolBox.getIsActive())) {
            tool.setStatus(ToolStatus.MAINTENANCE);
        } else {
            tool.setStatus(ToolStatus.AVAILABLE);
        }

        Tool savedTool = toolRepository.save(tool);
        return toResponse(savedTool);
    }

    public boolean isToolOwner(Long toolId, Long userId) {
        return toolRepository.findById(toolId)
                .map(tool -> tool.getOwnerId().equals(userId))
                .orElse(false);
    }
}
