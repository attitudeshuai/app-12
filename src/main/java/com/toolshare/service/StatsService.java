package com.toolshare.service;

import com.toolshare.dto.stats.HotToolRank;
import com.toolshare.dto.stats.OverviewStats;
import com.toolshare.dto.stats.TrendStats;
import com.toolshare.entity.BorrowRequestStatus;
import com.toolshare.entity.Tool;
import com.toolshare.entity.ToolLogAction;
import com.toolshare.entity.ToolStatus;
import com.toolshare.repository.BorrowRequestRepository;
import com.toolshare.repository.OverdueRecordRepository;
import com.toolshare.repository.ToolBoxRepository;
import com.toolshare.repository.ToolFavoriteRepository;
import com.toolshare.repository.ToolLogRepository;
import com.toolshare.repository.ToolRepository;
import com.toolshare.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class StatsService {

    private static final int DEFAULT_HOT_TOOLS_LIMIT = 10;
    private static final long BORROW_WEIGHT = 2;
    private static final long FAVORITE_WEIGHT = 1;

    private final UserRepository userRepository;
    private final ToolBoxRepository toolBoxRepository;
    private final ToolRepository toolRepository;
    private final BorrowRequestRepository borrowRequestRepository;
    private final ToolLogRepository toolLogRepository;
    private final OverdueRecordRepository overdueRecordRepository;
    private final ToolFavoriteRepository toolFavoriteRepository;

    public StatsService(UserRepository userRepository,
                        ToolBoxRepository toolBoxRepository,
                        ToolRepository toolRepository,
                        BorrowRequestRepository borrowRequestRepository,
                        ToolLogRepository toolLogRepository,
                        OverdueRecordRepository overdueRecordRepository,
                        ToolFavoriteRepository toolFavoriteRepository) {
        this.userRepository = userRepository;
        this.toolBoxRepository = toolBoxRepository;
        this.toolRepository = toolRepository;
        this.borrowRequestRepository = borrowRequestRepository;
        this.toolLogRepository = toolLogRepository;
        this.overdueRecordRepository = overdueRecordRepository;
        this.toolFavoriteRepository = toolFavoriteRepository;
    }

    public OverviewStats getOverviewStats() {
        OverviewStats stats = new OverviewStats();

        stats.setTotalUsers(userRepository.count());
        stats.setTotalToolBoxes(toolBoxRepository.count());
        stats.setTotalTools(toolRepository.count());
        stats.setTotalBorrowRequests(borrowRequestRepository.count());
        stats.setTotalToolLogs(toolLogRepository.count());

        Map<String, Long> toolsByStatus = new HashMap<>();
        for (ToolStatus status : ToolStatus.values()) {
            toolsByStatus.put(status.name(), toolRepository.countByStatus(status));
        }
        stats.setToolsByStatus(toolsByStatus);

        List<Object[]> categoryCounts = toolRepository.countByCategory();
        Map<String, Long> toolsByCategory = categoryCounts.stream()
                .collect(Collectors.toMap(
                        arr -> arr[0] != null ? arr[0].toString() : "未分类",
                        arr -> ((Number) arr[1]).longValue()
                ));
        stats.setToolsByCategory(toolsByCategory);

        Map<String, Long> borrowRequestsByStatus = new HashMap<>();
        for (BorrowRequestStatus status : BorrowRequestStatus.values()) {
            borrowRequestsByStatus.put(status.name(), borrowRequestRepository.countByStatus(status));
        }
        stats.setBorrowRequestsByStatus(borrowRequestsByStatus);

        stats.setTotalOverdueRecords(overdueRecordRepository.count());
        stats.setUnresolvedOverdueRecords(overdueRecordRepository.countByResolved(false));

        stats.setHotTools(getHotTools(DEFAULT_HOT_TOOLS_LIMIT));

        return stats;
    }

    public List<HotToolRank> getHotTools(int limit) {
        List<Object[]> borrowCounts = borrowRequestRepository.countByToolIdGrouped();
        List<Object[]> favoriteCounts = toolFavoriteRepository.countByToolIdGrouped();

        Map<Long, Long> borrowCountMap = borrowCounts.stream()
                .collect(Collectors.toMap(
                        arr -> ((Number) arr[0]).longValue(),
                        arr -> ((Number) arr[1]).longValue()
                ));
        Map<Long, Long> favoriteCountMap = favoriteCounts.stream()
                .collect(Collectors.toMap(
                        arr -> ((Number) arr[0]).longValue(),
                        arr -> ((Number) arr[1]).longValue()
                ));

        Set<Long> toolIds = new HashSet<>();
        toolIds.addAll(borrowCountMap.keySet());
        toolIds.addAll(favoriteCountMap.keySet());

        if (toolIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<Tool> tools = toolRepository.findAllById(toolIds);
        Map<Long, Tool> toolMap = tools.stream()
                .collect(Collectors.toMap(Tool::getId, t -> t));

        List<HotToolRank> ranks = new ArrayList<>();
        for (Long toolId : toolIds) {
            Tool tool = toolMap.get(toolId);
            if (tool == null) {
                continue;
            }
            long borrowCount = borrowCountMap.getOrDefault(toolId, 0L);
            long favoriteCount = favoriteCountMap.getOrDefault(toolId, 0L);
            long compositeScore = borrowCount * BORROW_WEIGHT + favoriteCount * FAVORITE_WEIGHT;

            HotToolRank rank = new HotToolRank();
            rank.setToolId(toolId);
            rank.setToolName(tool.getName());
            rank.setCategory(tool.getCategory());
            rank.setImage(tool.getImage());
            rank.setBorrowCount(borrowCount);
            rank.setFavoriteCount(favoriteCount);
            rank.setCompositeScore(compositeScore);
            ranks.add(rank);
        }

        ranks.sort(Comparator.comparingLong(HotToolRank::getCompositeScore).reversed());

        if (ranks.size() > limit) {
            ranks = ranks.subList(0, limit);
        }

        for (int i = 0; i < ranks.size(); i++) {
            ranks.get(i).setRank(i + 1);
        }

        return ranks;
    }

    public Map<Long, Long> getBorrowCountMap(List<Long> toolIds) {
        if (toolIds == null || toolIds.isEmpty()) {
            return new HashMap<>();
        }
        List<Object[]> counts = borrowRequestRepository.countByToolIdsGrouped(toolIds);
        return counts.stream()
                .collect(Collectors.toMap(
                        arr -> ((Number) arr[0]).longValue(),
                        arr -> ((Number) arr[1]).longValue()
                ));
    }

    public TrendStats getTrendStats(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            startDate = LocalDate.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        TrendStats stats = new TrendStats();
        stats.setStartDate(startDate.toString());
        stats.setEndDate(endDate.toString());

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        List<Object[]> borrowByDate = borrowRequestRepository.countByDateRange(startDateTime, endDateTime);
        List<Map<String, Object>> borrowRequestsByDate = borrowByDate.stream()
                .map(arr -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("date", arr[0].toString());
                    map.put("count", ((Number) arr[1]).longValue());
                    return map;
                })
                .collect(Collectors.toList());
        stats.setBorrowRequestsByDate(borrowRequestsByDate);

        List<Object[]> logByAction = toolLogRepository.countByAction();
        List<Map<String, Object>> toolLogsByAction = logByAction.stream()
                .map(arr -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("action", arr[0].toString());
                    map.put("count", ((Number) arr[1]).longValue());
                    return map;
                })
                .collect(Collectors.toList());
        stats.setToolLogsByAction(toolLogsByAction);

        return stats;
    }
}
