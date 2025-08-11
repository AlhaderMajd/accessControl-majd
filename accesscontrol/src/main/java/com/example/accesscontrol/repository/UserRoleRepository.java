package com.example.accesscontrol.repository;

import com.example.accesscontrol.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    boolean existsByUser_IdAndRole_Id(Long userId, Long roleId);

    List<UserRole> findByUser_IdInAndRole_IdIn(List<Long> userIds, List<Long> roleIds);

    int deleteByUser_IdInAndRole_IdIn(List<Long> userIds, List<Long> roleIds);

    void deleteByUser_IdIn(List<Long> userIds);

    void deleteAllByRole_IdIn(List<Long> roleIds);

    @Query("""
            SELECT r.name
            FROM UserRole ur
            JOIN ur.role r
            WHERE ur.user.id = :userId
            """)
    List<String> findRoleNamesByUserId(@Param("userId") Long userId);
}
