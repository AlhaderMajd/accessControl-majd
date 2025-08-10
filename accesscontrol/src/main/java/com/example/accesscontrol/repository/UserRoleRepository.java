package com.example.accesscontrol.repository;

import com.example.accesscontrol.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRole.Id> {

    List<UserRole> findByIdUserIdInAndIdRoleIdIn(List<Long> userIds, List<Long> roleIds);

    int deleteByIdUserIdInAndIdRoleIdIn(List<Long> userIds, List<Long> roleIds);

    void deleteByIdUserIdIn(List<Long> userIds);

    void deleteAllByIdRoleIdIn(List<Long> roleIds);

    @Query("""
           SELECT r.name
           FROM Role r
           JOIN UserRole ur ON ur.id.roleId = r.id
           WHERE ur.id.userId = :userId
           """)
    List<String> findRoleNamesByUserId(@Param("userId") Long userId);
}
