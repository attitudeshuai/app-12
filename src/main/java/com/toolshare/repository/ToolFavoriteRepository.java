package com.toolshare.repository;

import com.toolshare.entity.ToolFavorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ToolFavoriteRepository extends JpaRepository<ToolFavorite, Long> {

    Page<ToolFavorite> findByUserId(Long userId, Pageable pageable);

    Optional<ToolFavorite> findByUserIdAndToolId(Long userId, Long toolId);

    boolean existsByUserIdAndToolId(Long userId, Long toolId);

    void deleteByUserIdAndToolId(Long userId, Long toolId);

    long countByToolId(Long toolId);

    @Query("SELECT tf.toolId FROM ToolFavorite tf WHERE tf.userId = :userId AND tf.toolId IN :toolIds")
    List<Long> findFavoritedToolIdsByUserIdAndToolIds(@Param("userId") Long userId, @Param("toolIds") List<Long> toolIds);

    @Query("SELECT tf.toolId, COUNT(tf) FROM ToolFavorite tf GROUP BY tf.toolId")
    List<Object[]> countByToolIdGrouped();

    @Query("SELECT tf.toolId, COUNT(tf) FROM ToolFavorite tf WHERE tf.toolId IN :toolIds GROUP BY tf.toolId")
    List<Object[]> countByToolIdsGrouped(@Param("toolIds") List<Long> toolIds);
}
