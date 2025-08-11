package com.example.accesscontrol.service;

import com.example.accesscontrol.entity.Group;
import com.example.accesscontrol.entity.GroupRole;
import com.example.accesscontrol.entity.Role;
import com.example.accesscontrol.repository.GroupRepository;
import com.example.accesscontrol.repository.GroupRoleRepository;
import com.example.accesscontrol.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class GroupRoleService {

    private final GroupRoleRepository groupRoleRepository;
    private final GroupRepository groupRepository;
    private final RoleRepository roleRepository;

    @Transactional
    public int assignRolesToGroups(List<Long> groupIds, List<Long> roleIds) {
        if (groupIds == null || groupIds.isEmpty() || roleIds == null || roleIds.isEmpty()) return 0;

        var existing = groupRoleRepository.findByGroup_IdInAndRole_IdIn(groupIds, roleIds);
        Set<String> existingKeys = existing.stream()
                .map(gr -> gr.getGroup().getId() + "_" + gr.getRole().getId())
                .collect(java.util.stream.Collectors.toSet());

        List<GroupRole> toInsert = new ArrayList<>();
        for (Long gId : groupIds) {
            for (Long rId : roleIds) {
                String key = gId + "_" + rId;
                if (!existingKeys.contains(key)) {
                    GroupRole gr = new GroupRole();
                    gr.setGroup(Group.builder().id(gId).build());
                    gr.setRole(Role.builder().id(rId).build());
                    toInsert.add(gr);
                }
            }
        }
        if (!toInsert.isEmpty()) groupRoleRepository.saveAll(toInsert);
        return toInsert.size();
    }

    @Transactional
    public void deassignRolesFromGroups(List<Long> groupIds, List<Long> roleIds) {
        if (groupIds == null || groupIds.isEmpty() || roleIds == null || roleIds.isEmpty()) return;
        groupRoleRepository.deleteByGroup_IdInAndRole_IdIn(groupIds, roleIds);
    }

    @Transactional
    public void deleteByRoleIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) return;
        groupRoleRepository.deleteByRole_IdIn(roleIds);
    }

    @Transactional
    public void deleteByGroupIds(List<Long> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) return;
        groupRoleRepository.deleteByGroup_IdIn(groupIds);
    }

    @Transactional(readOnly = true)
    public List<Long> getRoleIdsByGroupId(Long groupId) {
        return groupRoleRepository.findByGroup_Id(groupId).stream()
                .map(gr -> gr.getRole().getId())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Long> getExistingGroupIds(List<Long> groupIds) {
        return groupRepository.findAllById(groupIds).stream().map(Group::getId).toList();
    }

    @Transactional(readOnly = true)
    public List<Long> getExistingRoleIds(List<Long> roleIds) {
        return roleRepository.findAllById(roleIds).stream().map(Role::getId).toList();
    }
}
