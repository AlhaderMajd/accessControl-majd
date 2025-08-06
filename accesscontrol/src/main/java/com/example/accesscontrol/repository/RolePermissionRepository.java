package com.example.accesscontrol.repository;

import com.example.accesscontrol.entity.RolePermission;
import com.example.accesscontrol.entity.RolePermissionId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermissionId> {
}
