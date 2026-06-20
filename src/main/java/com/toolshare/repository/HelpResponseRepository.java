package com.toolshare.repository;

import com.toolshare.entity.HelpResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HelpResponseRepository extends JpaRepository<HelpResponse, Long> {

    List<HelpResponse> findByHelpPostIdOrderByCreatedAtDesc(Long helpPostId);

    Page<HelpResponse> findByHelpPostId(Long helpPostId, Pageable pageable);

    Page<HelpResponse> findByResponderId(Long responderId, Pageable pageable);

    Optional<HelpResponse> findByHelpPostIdAndResponderId(Long helpPostId, Long responderId);

    boolean existsByHelpPostIdAndResponderId(Long helpPostId, Long responderId);
}
