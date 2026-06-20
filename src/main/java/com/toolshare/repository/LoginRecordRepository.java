package com.toolshare.repository;

import com.toolshare.entity.LoginRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LoginRecordRepository extends JpaRepository<LoginRecord, Long> {

    long countByUserIdAndLoginAtBetween(Long userId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT lr.userId, COUNT(lr) FROM LoginRecord lr " +
           "WHERE lr.loginAt >= :startTime GROUP BY lr.userId")
    List<Object[]> countByUserIdSince(@Param("startTime") LocalDateTime startTime);
}
