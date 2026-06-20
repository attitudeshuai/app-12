package com.toolshare.repository;

import com.toolshare.entity.MaintenancePlan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MaintenancePlanRepository extends JpaRepository<MaintenancePlan, Long> {

    Optional<MaintenancePlan> findByToolId(Long toolId);

    Page<MaintenancePlan> findByIsActiveTrue(Pageable pageable);

    @Query("SELECT m FROM MaintenancePlan m WHERE m.isActive = true " +
           "AND m.nextMaintenanceDate <= :dueDate")
    Page<MaintenancePlan> findDuePlans(@Param("dueDate") LocalDate dueDate, Pageable pageable);

    @Query("SELECT m FROM MaintenancePlan m WHERE m.isActive = true " +
           "AND m.nextMaintenanceDate BETWEEN :startDate AND :endDate " +
           "AND m.isDueSoonNotified = false")
    Page<MaintenancePlan> findDueSoonPlans(@Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate,
                                           Pageable pageable);

    @Query("SELECT m FROM MaintenancePlan m WHERE m.isActive = true " +
           "AND m.nextMaintenanceDate <= :today " +
           "AND m.isDueNotified = false")
    Page<MaintenancePlan> findOverdueUnnotifiedPlans(@Param("today") LocalDate today, Pageable pageable);

    List<MaintenancePlan> findByToolIdIn(List<Long> toolIds);

    void deleteByToolId(Long toolId);
}
