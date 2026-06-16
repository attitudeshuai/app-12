package com.toolshare.repository;

import com.toolshare.entity.ToolReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ToolReviewRepository extends JpaRepository<ToolReview, Long> {

    Page<ToolReview> findByToolId(Long toolId, Pageable pageable);

    Page<ToolReview> findByReviewerId(Long reviewerId, Pageable pageable);

    Optional<ToolReview> findByBorrowRequestId(Long borrowRequestId);

    @Query("SELECT AVG(tr.rating) FROM ToolReview tr WHERE tr.toolId = :toolId")
    Double findAverageRatingByToolId(@Param("toolId") Long toolId);

    @Query("SELECT COUNT(tr) FROM ToolReview tr WHERE tr.toolId = :toolId")
    Long countByToolId(@Param("toolId") Long toolId);

    boolean existsByBorrowRequestId(Long borrowRequestId);
}
