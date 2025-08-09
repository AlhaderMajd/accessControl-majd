package com.example.accesscontrol.service;

import com.example.accesscontrol.entity.GroupRole;
import com.example.accesscontrol.repository.GroupRepository;
import com.example.accesscontrol.repository.GroupRoleRepository;
import com.example.accesscontrol.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupRoleService {

    private final GroupRoleRepository groupRoleRepository;
    private final GroupRepository groupRepository;
    private final RoleRepository roleRepository;

    @Transactional
    public int assignRolesToGroups(List<Long> groupIds, List<Long> roleIds) {
        if (groupIds == null || groupIds.isEmpty() || roleIds == null || roleIds.isEmpty()) return 0;
        List<Long> existingGroupIds = groupRepository.findAllById(groupIds).stream().map(g -> g.getId()).toList();
        List<Long> existingRoleIds = roleRepository.findAllById(roleIds).stream().map(r -> r.getId()).toList();
        if (existingGroupIds.isEmpty() || existingRoleIds.isEmpty()) return 0;

        Set<GroupRole.Id> candidate = new HashSet<>();
        for (Long g : existingGroupIds) for (Long r : existingRoleIds) candidate.add(new GroupRole.Id(g, r));

        Set<GroupRole.Id> already = groupRoleRepository
                .findByIdGroupIdInAndIdRoleIdIn(existingGroupIds, existingRoleIds)
                .stream().map(GroupRole::getId).collect(Collectors.toSet());

        candidate.removeAll(already);
        if (candidate.isEmpty()) return 0;

        var toInsert = candidate.stream().map(id -> GroupRole.builder().id(id).build()).toList();
        groupRoleRepository.saveAll(toInsert);
        return toInsert.size();
    }

    @Transactional
    public void deassignRolesFromGroups(List<Long> groupIds, List<Long> roleIds) {
        if (groupIds == null || groupIds.isEmpty() || roleIds == null || roleIds.isEmpty()) return;
        groupRoleRepository.deleteAllByIdGroupIdInAndIdRoleIdIn(new HashSet<>(groupIds), new HashSet<>(roleIds));
    }

    @Transactional
    public void deassignExactPairs(Collection<GroupRole.Id> ids) {
        if (ids == null || ids.isEmpty()) return;
        groupRoleRepository.deleteAllById(ids);
    }

    @Transactional
    public void deleteByRoleIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) return;
        groupRoleRepository.deleteAllByIdRoleIdIn(roleIds);
    }

    @Transactional
    public void deleteByGroupIds(List<Long> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) return;
        groupRoleRepository.deleteAllByIdGroupIdIn(groupIds);
    }

    public List<Long> getRoleIdsByGroupId(Long groupId) {
        return groupRoleRepository.findByIdGroupId(groupId).stream()
                .map(gr -> gr.getId().getRoleId()).toList();
    }

    public List<Long> getExistingGroupIds(List<Long> groupIds) {
        return groupRepository.findAllById(groupIds).stream().map(g -> g.getId()).toList();
    }
}
