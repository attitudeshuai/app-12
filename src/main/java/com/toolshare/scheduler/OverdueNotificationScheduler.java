package com.toolshare.scheduler;

import com.toolshare.entity.BorrowRequest;
import com.toolshare.entity.BorrowRequestStatus;
import com.toolshare.entity.NotificationType;
import com.toolshare.entity.Tool;
import com.toolshare.repository.BorrowRequestRepository;
import com.toolshare.repository.NotificationRepository;
import com.toolshare.repository.ToolRepository;
import com.toolshare.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class OverdueNotificationScheduler {

    private final BorrowRequestRepository borrowRequestRepository;
    private final ToolRepository toolRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    public OverdueNotificationScheduler(BorrowRequestRepository borrowRequestRepository,
                                        ToolRepository toolRepository,
                                        NotificationRepository notificationRepository,
                                        NotificationService notificationService) {
        this.borrowRequestRepository = borrowRequestRepository;
        this.toolRepository = toolRepository;
        this.notificationRepository = notificationRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(cron = "0 0 9 * * ?")
    public void checkOverdueBorrows() {
        LocalDate today = LocalDate.now();
        Pageable pageable = PageRequest.of(0, 100);
        Page<BorrowRequest> overduePage = borrowRequestRepository.search(
                BorrowRequestStatus.APPROVED, null, null, null, today.minusDays(1), pageable);

        while (!overduePage.isEmpty()) {
            for (BorrowRequest borrowRequest : overduePage.getContent()) {
                if (borrowRequest.getExpectedReturnDate().isBefore(today)) {
                    boolean alreadyNotified = !notificationRepository.findByUserIdAndTypeAndRelatedIdAndRead(
                            borrowRequest.getRequesterId(),
                            NotificationType.BORROW_OVERDUE,
                            borrowRequest.getId(),
                            false
                    ).isEmpty();

                    if (!alreadyNotified) {
                        Tool tool = toolRepository.findById(borrowRequest.getToolId()).orElse(null);
                        String toolName = tool != null ? tool.getName() : "未知工具";
                        notificationService.createNotification(
                                borrowRequest.getRequesterId(),
                                NotificationType.BORROW_OVERDUE,
                                "工具借用已逾期",
                                "您借用的工具「" + toolName + "」已超过预计归还日期，请尽快归还。",
                                borrowRequest.getId()
                        );
                    }
                }
            }

            if (overduePage.hasNext()) {
                overduePage = borrowRequestRepository.search(
                        BorrowRequestStatus.APPROVED, null, null, null, today.minusDays(1),
                        PageRequest.of(overduePage.getNumber() + 1, 100));
            } else {
                break;
            }
        }
    }
}
