package com.example.accesscontrol.service;

import org.apache.commons.lang3.tuple.Pair;
import com.example.accesscontrol.entity.GroupRole;
import com.example.accesscontrol.entity.GroupRoleId;
import com.example.accesscontrol.repository.GroupRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupRoleService {

    private final GroupRoleRepository groupRoleRepository;

    @Transactional
    public int assignRolesToGroups(List<Long> groupIds, List<Long> roleIds) {
        Set<GroupRoleId> idsToInsert = new HashSet<>();
        for (Long groupId : groupIds) {
            for (Long roleId : roleIds) {
                idsToInsert.add(new GroupRoleId(groupId, roleId));
            }
        }

        List<GroupRoleId> existing = groupRoleRepository.findAllById(idsToInsert)
                .stream()
                .map(gr -> new GroupRoleId(gr.getGroupId(), gr.getRoleId()))
                .toList();

        idsToInsert.removeAll(existing);

        if (idsToInsert.isEmpty()) return 0;

        List<GroupRole> toSave = idsToInsert.stream()
                .map(id -> new GroupRole(id.getGroupId(), id.getRoleId()))
                .toList();

        groupRoleRepository.saveAll(toSave);
        return toSave.size();
    }

    @Transactional
    public int deassignRolesFromGroups(List<Long> groupIds, List<Long> roleIds) {
        groupRoleRepository.deleteAllByGroupIdInAndRoleIdIn(
                new HashSet<>(groupIds),
                new HashSet<>(roleIds)
        );
        return groupIds.size() * roleIds.size(); // estimate
    }

    public Set<Pair<Long, Long>> getAllGroupRolePairs() {
        return groupRoleRepository.findAll().stream()
                .map(gr -> Pair.of(gr.getGroupId(), gr.getRoleId()))
                .collect(Collectors.toSet());
    }



    public void saveAll(List<GroupRole> groupRoles) {
        groupRoleRepository.saveAll(groupRoles);
    }


}
