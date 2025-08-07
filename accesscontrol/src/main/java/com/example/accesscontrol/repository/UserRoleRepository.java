package com.example.accesscontrol.repository;

import com.example.accesscontrol.entity.UserRole;
import com.example.accesscontrol.entity.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {

    List<UserRole> findByUserId(Long userId);

    List<UserRole> findByUserIdInAndRoleIdIn(List<Long> userIds, List<Long> roleIds);

    int deleteByUserIdInAndRoleIdIn(List<Long> userIds, List<Long> roleIds);

    void deleteByUserIdIn(List<Long> userIds);

    void deleteAllByRoleIdIn(List<Long> roleIds);

    @Query("SELECT r.name FROM Role r JOIN UserRole ur ON ur.roleId = r.id WHERE ur.userId = :userId")
    List<String> findRoleNamesByUserId(Long userId);
}
