package com.toolshare.scheduler;

import com.toolshare.service.MaintenancePlanService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Component
public class MaintenanceScheduler {

    private final MaintenancePlanService maintenancePlanService;

    public MaintenanceScheduler(MaintenancePlanService maintenancePlanService) {
        this.maintenancePlanService = maintenancePlanService;
    }

    @Scheduled(cron = "0 0 8 * * ?")
    @Transactional
    public void checkMaintenancePlans() {
        LocalDate today = LocalDate.now();
        maintenancePlanService.processDueSoonMaintenance(today);
        maintenancePlanService.processDueMaintenance(today);
    }
}
