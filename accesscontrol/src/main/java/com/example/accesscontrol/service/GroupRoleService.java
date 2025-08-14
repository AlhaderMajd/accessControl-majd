package com.example.accesscontrol.service;

import com.example.accesscontrol.entity.Group;
import com.example.accesscontrol.entity.GroupRole;
import com.example.accesscontrol.entity.Role;
import com.example.accesscontrol.repository.GroupRepository;
import com.example.accesscontrol.repository.GroupRoleRepository;
import com.example.accesscontrol.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
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
    public int assignGroupRolePairs(Map<Long, Set<Long>> wanted) {
        if (wanted == null || wanted.isEmpty()) return 0;

        var groupIds = new java.util.ArrayList<>(wanted.keySet());
        var roleIds = wanted.values().stream().flatMap(Set::stream).distinct().toList();

        var existingBefore = groupRoleRepository.findByGroup_IdInAndRole_IdIn(groupIds, roleIds);
        var existingKeys = existingBefore.stream()
                .map(gr -> gr.getGroup().getId() + "_" + gr.getRole().getId())
                .collect(java.util.stream.Collectors.toSet());

        List<GroupRole> toInsert = new java.util.ArrayList<>();
        for (var e : wanted.entrySet()) {
            Long gId = e.getKey();
            for (Long rId : e.getValue()) {
                String key = gId + "_" + rId;
                if (!existingKeys.contains(key)) {
                    GroupRole gr = new GroupRole();
                    gr.setGroup(Group.builder().id(gId).build());
                    gr.setRole(Role.builder().id(rId).build());
                    toInsert.add(gr);
                }
            }
        }
        if (toInsert.isEmpty()) return 0;

        try {
            groupRoleRepository.saveAll(toInsert);
            return toInsert.size();
        } catch (DataIntegrityViolationException ex) {
            var after = groupRoleRepository.findByGroup_IdInAndRole_IdIn(groupIds, roleIds);
            return Math.max(0, after.size() - existingBefore.size());
        }
    }

    @Transactional
    public int deleteGroupRolePairs(Map<Long, Set<Long>> wanted) {
        if (wanted == null || wanted.isEmpty()) return 0;

        var groupIds = new java.util.ArrayList<>(wanted.keySet());
        var roleIds = wanted.values().stream().flatMap(Set::stream).distinct().toList();

        var existing = groupRoleRepository.findByGroup_IdInAndRole_IdIn(groupIds, roleIds);

        Set<String> wantedKeys = new java.util.HashSet<>();
        for (var e : wanted.entrySet()) {
            Long gId = e.getKey();
            for (Long rId : e.getValue()) {
                wantedKeys.add(gId + "_" + rId);
            }
        }

        var toDelete = existing.stream()
                .filter(gr -> wantedKeys.contains(gr.getGroup().getId() + "_" + gr.getRole().getId()))
                .toList();

        if (toDelete.isEmpty()) return 0;

        groupRoleRepository.deleteAllInBatch(toDelete);
        return toDelete.size();
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
