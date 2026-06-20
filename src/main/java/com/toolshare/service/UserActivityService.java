package com.toolshare.service;

import com.toolshare.dto.stats.UserActivityRankingResponse;
import com.toolshare.entity.BorrowRequestStatus;
import com.toolshare.entity.User;
import com.toolshare.repository.BorrowRequestRepository;
import com.toolshare.repository.LoginRecordRepository;
import com.toolshare.repository.ToolRepository;
import com.toolshare.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserActivityService {

    private static final double LOGIN_WEIGHT = 1.0;
    private static final double PUBLISHED_TOOL_WEIGHT = 5.0;
    private static final double INITIATED_BORROW_WEIGHT = 3.0;
    private static final double COMPLETED_BORROW_WEIGHT = 4.0;

    private final UserRepository userRepository;
    private final LoginRecordRepository loginRecordRepository;
    private final ToolRepository toolRepository;
    private final BorrowRequestRepository borrowRequestRepository;

    public UserActivityService(UserRepository userRepository,
                               LoginRecordRepository loginRecordRepository,
                               ToolRepository toolRepository,
                               BorrowRequestRepository borrowRequestRepository) {
        this.userRepository = userRepository;
        this.loginRecordRepository = loginRecordRepository;
        this.toolRepository = toolRepository;
        this.borrowRequestRepository = borrowRequestRepository;
    }

    public List<UserActivityRankingResponse> getActivityRanking(int days, int limit) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);

        List<Object[]> loginCounts = loginRecordRepository.countByUserIdSince(since);
        Map<Long, Long> loginCountMap = toLongMap(loginCounts);

        List<Object[]> toolCounts = toolRepository.countByOwnerIdSince(since);
        Map<Long, Long> toolCountMap = toLongMap(toolCounts);

        List<Object[]> initiatedBorrowCounts = borrowRequestRepository.countInitiatedByUserIdSince(since);
        Map<Long, Long> initiatedBorrowMap = toLongMap(initiatedBorrowCounts);

        List<Object[]> completedBorrowCounts = borrowRequestRepository.countCompletedByUserIdSince(since, BorrowRequestStatus.RETURNED);
        Map<Long, Long> completedBorrowMap = toLongMap(completedBorrowCounts);

        Set<Long> userIds = new HashSet<>();
        userIds.addAll(loginCountMap.keySet());
        userIds.addAll(toolCountMap.keySet());
        userIds.addAll(initiatedBorrowMap.keySet());
        userIds.addAll(completedBorrowMap.keySet());

        if (userIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<User> users = userRepository.findAllById(userIds);
        Map<Long, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<UserActivityRankingResponse> rankings = new ArrayList<>();
        for (Long userId : userIds) {
            User user = userMap.get(userId);
            if (user == null) {
                continue;
            }

            long loginCount = loginCountMap.getOrDefault(userId, 0L);
            long publishedToolCount = toolCountMap.getOrDefault(userId, 0L);
            long initiatedBorrowCount = initiatedBorrowMap.getOrDefault(userId, 0L);
            long completedBorrowCount = completedBorrowMap.getOrDefault(userId, 0L);

            double activityScore = loginCount * LOGIN_WEIGHT
                    + publishedToolCount * PUBLISHED_TOOL_WEIGHT
                    + initiatedBorrowCount * INITIATED_BORROW_WEIGHT
                    + completedBorrowCount * COMPLETED_BORROW_WEIGHT;
            activityScore = Math.round(activityScore * 10) / 10.0;

            UserActivityRankingResponse ranking = new UserActivityRankingResponse();
            ranking.setUserId(userId);
            ranking.setUsername(user.getUsername());
            ranking.setAvatar(user.getAvatar());
            ranking.setLoginCount(loginCount);
            ranking.setPublishedToolCount(publishedToolCount);
            ranking.setInitiatedBorrowCount(initiatedBorrowCount);
            ranking.setCompletedBorrowCount(completedBorrowCount);
            ranking.setActivityScore(activityScore);
            rankings.add(ranking);
        }

        rankings.sort(Comparator.comparingDouble(UserActivityRankingResponse::getActivityScore).reversed());

        if (rankings.size() > limit) {
            rankings = rankings.subList(0, limit);
        }

        for (int i = 0; i < rankings.size(); i++) {
            rankings.get(i).setRank(i + 1);
        }

        return rankings;
    }

    private Map<Long, Long> toLongMap(List<Object[]> counts) {
        return counts.stream()
                .collect(Collectors.toMap(
                        arr -> ((Number) arr[0]).longValue(),
                        arr -> ((Number) arr[1]).longValue()
                ));
    }
}
