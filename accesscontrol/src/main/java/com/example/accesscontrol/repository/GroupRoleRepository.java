package com.example.accesscontrol.repository;

import com.example.accesscontrol.entity.GroupRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupRoleRepository extends JpaRepository<GroupRole, Long> {

    // Exists (used in initializer)
    boolean existsByGroup_IdAndRole_Id(Long groupId, Long roleId);

    // Used by GroupRoleService.assignRolesToGroups
    List<GroupRole> findByGroup_IdInAndRole_IdIn(List<Long> groupIds, List<Long> roleIds);

    // Used by GroupRoleService.deassignRolesFromGroups
    int deleteByGroup_IdInAndRole_IdIn(List<Long> groupIds, List<Long> roleIds);

    // Used by GroupRoleService.deleteByGroupIds / deleteByRoleIds
    void deleteByGroup_IdIn(List<Long> groupIds);

    void deleteByRole_IdIn(List<Long> roleIds);

    // Used by GroupRoleService.getRoleIdsByGroupId
    List<GroupRole> findByGroup_Id(Long groupId);
}
