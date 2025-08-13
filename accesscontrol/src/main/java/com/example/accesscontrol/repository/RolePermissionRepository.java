package com.example.accesscontrol.repository;

import com.example.accesscontrol.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {

    boolean existsByRole_IdAndPermission_Id(Long roleId, Long permissionId);

    List<RolePermission> findByRole_IdInAndPermission_IdIn(List<Long> roleIds, List<Long> permissionIds);

    int deleteByRole_IdInAndPermission_IdIn(List<Long> roleIds, List<Long> permissionIds);

    void deleteByRole_IdIn(List<Long> roleIds);

    void deleteByPermission_IdIn(List<Long> permissionIds);
}
