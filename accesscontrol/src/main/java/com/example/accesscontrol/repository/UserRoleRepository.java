package com.example.accesscontrol.repository;

import com.example.accesscontrol.entity.UserRole;
import com.example.accesscontrol.entity.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {
    List<UserRole> findByUserId(Long userId);
}
