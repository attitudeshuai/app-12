package com.toolshare.repository;

import com.toolshare.entity.HelpPost;
import com.toolshare.entity.HelpPostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public interface HelpPostRepository extends JpaRepository<HelpPost, Long> {

    Page<HelpPost> findByStatus(HelpPostStatus status, Pageable pageable);

    Page<HelpPost> findByPosterId(Long posterId, Pageable pageable);

    Page<HelpPost> findByPosterIdAndStatus(Long posterId, HelpPostStatus status, Pageable pageable);

    Page<HelpPost> findByAcceptedResponderId(Long acceptedResponderId, Pageable pageable);

    @Query("SELECT hp FROM HelpPost hp WHERE hp.category = :category AND hp.status = :status")
    Page<HelpPost> findByCategoryAndStatus(@Param("category") String category, @Param("status") HelpPostStatus status, Pageable pageable);

    @Query("SELECT hp FROM HelpPost hp WHERE hp.category = :category")
    Page<HelpPost> findByCategory(@Param("category") String category, Pageable pageable);

    @Query("SELECT hp FROM HelpPost hp WHERE hp.status IN :statuses")
    Page<HelpPost> findByStatusIn(@Param("statuses") List<HelpPostStatus> statuses, Pageable pageable);

    @Query("SELECT hp FROM HelpPost hp WHERE hp.status IN :statuses AND hp.category = :category")
    Page<HelpPost> findByStatusInAndCategory(@Param("statuses") List<HelpPostStatus> statuses, @Param("category") String category, Pageable pageable);

    @Query("SELECT COUNT(hr) FROM HelpResponse hr WHERE hr.helpPostId = :helpPostId")
    Long countResponsesByHelpPostId(@Param("helpPostId") Long helpPostId);

    @Query("SELECT hr.helpPostId, COUNT(hr) FROM HelpResponse hr WHERE hr.helpPostId IN :helpPostIds GROUP BY hr.helpPostId")
    List<Object[]> countResponsesByHelpPostIds(@Param("helpPostIds") List<Long> helpPostIds);

    default Map<Long, Long> findResponseCountMapByHelpPostIds(List<Long> helpPostIds) {
        return countResponsesByHelpPostIds(helpPostIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));
    }
}
