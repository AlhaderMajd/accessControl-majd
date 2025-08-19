package com.example.accesscontrol.repository;

import com.example.accesscontrol.dto.user.getUsers.UserSummaryResponse;
import com.example.accesscontrol.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    List<User> findAllByEmailIn(List<String> emails);
    boolean existsByEmailIgnoreCase(String email);
    @Query(
            value = """
            SELECT new com.example.accesscontrol.dto.user.getUsers.UserSummaryResponse(
                u.id, u.email, u.enabled
            )
            FROM User u
            WHERE (:q = '' OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%')))
            """,
            countQuery = """
            SELECT COUNT(u)
            FROM User u
            WHERE (:q = '' OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%')))
            """
    )
    Page<UserSummaryResponse> searchUserSummaries(@Param("q") String q, Pageable pageable);
}
