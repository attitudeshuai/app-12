package com.toolshare.service;

import com.toolshare.dto.PageResponse;
import com.toolshare.dto.tool.CompleteRepairRequest;
import com.toolshare.dto.tool.CreateToolRequest;
import com.toolshare.dto.tool.ReportToolRequest;
import com.toolshare.dto.tool.ToolResponse;
import com.toolshare.dto.tool.UpdateToolRequest;
import com.toolshare.dto.tool.UpdateToolStatusRequest;
import com.toolshare.entity.Tool;
import com.toolshare.entity.ToolBox;
import com.toolshare.entity.ToolLogAction;
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
import java.util.Comparator;
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
    private final ToolLogService toolLogService;
    private final StatsService statsService;

    public ToolService(ToolRepository toolRepository, ToolBoxRepository toolBoxRepository, UserRepository userRepository,
                       ToolReviewService toolReviewService, ToolFavoriteRepository toolFavoriteRepository,
                       ToolLogService toolLogService, StatsService statsService) {
        this.toolRepository = toolRepository;
        this.toolBoxRepository = toolBoxRepository;
        this.userRepository = userRepository;
        this.toolReviewService = toolReviewService;
        this.toolFavoriteRepository = toolFavoriteRepository;
        this.toolLogService = toolLogService;
        this.statsService = statsService;
    }

    public PageResponse<ToolResponse> getAllTools(String keyword, String category, ToolStatus status, Long boxId,
                                                   int page, int size, String sortBy, String sortDir) {
        return getAllTools(keyword, category, status, boxId, null, page, size, sortBy, sortDir);
    }

    public PageResponse<ToolResponse> getAllTools(String keyword, String category, ToolStatus status, Long boxId,
                                                   List<Long> boxIds, int page, int size, String sortBy, String sortDir) {
        List<Long> effectiveBoxIds = resolveBoxIds(boxId, boxIds);

        if ("borrowCount".equals(sortBy)) {
            return getAllToolsSortedByBorrowCount(keyword, category, status, effectiveBoxIds, page, size, sortDir);
        }

        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Tool> toolPage;
        if (effectiveBoxIds != null && !effectiveBoxIds.isEmpty()) {
            toolPage = toolRepository.searchWithMultipleBoxes(keyword, category, status, effectiveBoxIds, pageable);
        } else if (boxId != null) {
            toolPage = toolRepository.search(keyword, category, status, boxId, pageable);
        } else {
            toolPage = toolRepository.search(keyword, category, status, null, pageable);
        }

        List<ToolResponse> responseList = toResponseList(toolPage.getContent());
        return PageResponse.of(responseList, toolPage.getTotalElements(), toolPage.getNumber(), toolPage.getSize());
    }

    private List<Long> resolveBoxIds(Long boxId, List<Long> boxIds) {
        if (boxIds != null && !boxIds.isEmpty()) {
            return boxIds;
        }
        if (boxId != null) {
            return List.of(boxId);
        }
        return null;
    }

    private PageResponse<ToolResponse> getAllToolsSortedByBorrowCount(String keyword, String category, ToolStatus status,
                                                                       List<Long> boxIds, int page, int size, String sortDir) {
        List<Tool> allTools;
        if (boxIds != null && !boxIds.isEmpty()) {
            allTools = toolRepository.searchWithMultipleBoxes(keyword, category, status, boxIds, Pageable.unpaged()).getContent();
        } else {
            allTools = toolRepository.search(keyword, category, status, null, Pageable.unpaged()).getContent();
        }

        List<ToolResponse> allResponses = toResponseList(allTools);

        Comparator<ToolResponse> borrowCountComparator = Comparator.comparing(
                response -> response.getBorrowCount() != null ? response.getBorrowCount() : 0L
        );
        if ("desc".equalsIgnoreCase(sortDir)) {
            borrowCountComparator = borrowCountComparator.reversed();
        }
        allResponses.sort(borrowCountComparator);

        long totalElements = allResponses.size();
        int fromIndex = Math.min(page * size, allResponses.size());
        int toIndex = Math.min(fromIndex + size, allResponses.size());
        List<ToolResponse> pageContent = allResponses.subList(fromIndex, toIndex);

        return PageResponse.of(pageContent, totalElements, page, size);
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
        tool.setMaxBorrowDays(request.getMaxBorrowDays());

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
        if (request.getMaxBorrowDays() != null) {
            tool.setMaxBorrowDays(request.getMaxBorrowDays());
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
    public ToolResponse reportTool(Long id, ReportToolRequest request, Long currentUserId) {
        Tool tool = toolRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("工具不存在"));

        if (tool.getStatus() == ToolStatus.DISABLED) {
            throw new BadRequestException("工具已被禁用，无法报修");
        }

        if (tool.getStatus() == ToolStatus.MAINTENANCE) {
            throw new BadRequestException("工具已在维修中，无需重复报修");
        }

        tool.setStatus(ToolStatus.MAINTENANCE);
        Tool savedTool = toolRepository.save(tool);

        toolLogService.createLogInternal(id, currentUserId, ToolLogAction.REPORT, request.getDescription());

        return toResponse(savedTool);
    }

    @Transactional
    public ToolResponse completeRepair(Long id, CompleteRepairRequest request, Long currentUserId) {
        Tool tool = toolRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("工具不存在"));

        if (!tool.getOwnerId().equals(currentUserId)) {
            throw new BadRequestException("只有工具所有者可以完成维修");
        }

        if (tool.getStatus() != ToolStatus.MAINTENANCE) {
            throw new BadRequestException("工具不在维修状态，无法完成维修");
        }

        ToolBox toolBox = toolBoxRepository.findById(tool.getBoxId()).orElse(null);
        if (toolBox != null && !Boolean.TRUE.equals(toolBox.getIsActive())) {
            tool.setStatus(ToolStatus.MAINTENANCE);
        } else {
            tool.setStatus(ToolStatus.AVAILABLE);
        }

        Tool savedTool = toolRepository.save(tool);

        toolLogService.createLogInternal(id, currentUserId, ToolLogAction.REPAIR, request.getDescription());

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
            List<Object[]> favCounts = toolFavoriteRepository.countByToolIdsGrouped(new ArrayList<>(toolIds));
            favoriteCountMap = favCounts.stream()
                    .collect(Collectors.toMap(
                            arr -> ((Number) arr[0]).longValue(),
                            arr -> ((Number) arr[1]).longValue()
                    ));
        }

        Map<Long, Long> borrowCountMap = statsService.getBorrowCountMap(new ArrayList<>(toolIds));

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
            response.setMaxBorrowDays(tool.getMaxBorrowDays());

            response.setBoxName(boxNameMap.get(tool.getBoxId()));
            response.setOwnerName(ownerNameMap.get(tool.getOwnerId()));
            response.setAverageRating(averageRatingMap.get(tool.getId()));
            response.setReviewCount(reviewCountMap.get(tool.getId()));
            response.setIsFavorited(currentUserId != null && favoritedToolIds.contains(tool.getId()));

            Long favoriteCount = favoriteCountMap.getOrDefault(tool.getId(), 0L);
            Long borrowCount = borrowCountMap.getOrDefault(tool.getId(), 0L);
            response.setFavoriteCount(favoriteCount);
            response.setBorrowCount(borrowCount);
            response.setHotRankScore(borrowCount * 2 + favoriteCount);

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
        response.setMaxBorrowDays(tool.getMaxBorrowDays());

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
        Long favoriteCount = toolFavoriteRepository.countByToolId(tool.getId());
        Long borrowCount = statsService.getBorrowCountMap(java.util.Collections.singletonList(tool.getId()))
                .getOrDefault(tool.getId(), 0L);
        response.setFavoriteCount(favoriteCount);
        response.setBorrowCount(borrowCount);
        response.setHotRankScore(borrowCount * 2 + favoriteCount);

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
