package com.toolshare.repository;

import com.toolshare.entity.Role;
import com.toolshare.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR u.username LIKE %:keyword% OR u.email LIKE %:keyword%) " +
           "AND (:role IS NULL OR u.role = :role) " +
           "AND (:isEnabled IS NULL OR u.isEnabled = :isEnabled)")
    Page<User> search(@Param("keyword") String keyword,
                      @Param("role") Role role,
                      @Param("isEnabled") Boolean isEnabled,
                      Pageable pageable);

    List<User> findByRoleAndIsEnabledTrue(Role role);
}
