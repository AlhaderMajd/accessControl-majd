package com.example.accesscontrol.service;

import com.example.accesscontrol.dto.user.assignUsersToGroup.AssignUsersToGroupsRequest;
import com.example.accesscontrol.dto.user.assignUsersToGroup.AssignUsersToGroupsResponse;
import com.example.accesscontrol.dto.user.deassignUsersFromGroups.DeassignUsersFromGroupsRequest;
import com.example.accesscontrol.dto.user.deassignUsersFromGroups.DeassignUsersFromGroupsResponse;
import com.example.accesscontrol.entity.Group;
import com.example.accesscontrol.entity.UserGroup;
import com.example.accesscontrol.repository.GroupRepository;
import com.example.accesscontrol.repository.UserGroupRepository;
import com.example.accesscontrol.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserGroupService {

    private final UserGroupRepository userGroupRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;

    @Transactional
    public AssignUsersToGroupsResponse assignUsersToGroups(AssignUsersToGroupsRequest request) {
        if (request.getUserIds() == null || request.getUserIds().isEmpty()
                || request.getGroupIds() == null || request.getGroupIds().isEmpty()) {
            return AssignUsersToGroupsResponse.builder().message("Nothing to assign").assignedCount(0).build();
        }

        var existing = userGroupRepository.findByUser_IdInAndGroup_IdIn(request.getUserIds(), request.getGroupIds());
        Set<String> existingKeys = existing.stream()
                .map(ug -> ug.getUser().getId() + "_" + ug.getGroup().getId())
                .collect(Collectors.toSet());

        List<UserGroup> toInsert = new ArrayList<>();
        for (Long uId : request.getUserIds()) {
            if (!userRepository.existsById(uId)) continue;
            for (Long gId : request.getGroupIds()) {
                if (!groupRepository.existsById(gId)) continue;
                String key = uId + "_" + gId;
                if (!existingKeys.contains(key)) {
                    UserGroup ug = new UserGroup();
                    ug.setUser(com.example.accesscontrol.entity.User.builder().id(uId).build());
                    ug.setGroup(com.example.accesscontrol.entity.Group.builder().id(gId).build());
                    toInsert.add(ug);
                }
            }
        }

        if (!toInsert.isEmpty()) userGroupRepository.saveAll(toInsert);

        return AssignUsersToGroupsResponse.builder()
                .message("Users assigned to groups successfully")
                .assignedCount(toInsert.size())
                .build();
    }

    @Transactional
    public DeassignUsersFromGroupsResponse deassignUsersFromGroups(DeassignUsersFromGroupsRequest request) {
        int deletedCount = userGroupRepository.deleteByUser_IdInAndGroup_IdIn(
                request.getUserIds(), request.getGroupIds());
        return DeassignUsersFromGroupsResponse.builder()
                .message("Users deassigned from groups successfully")
                .removedCount(deletedCount)
                .build();
    }

    @Transactional(readOnly = true)
    public List<Long> getGroupIdsByUserId(Long userId) {
        return userGroupRepository.findByUser_Id(userId).stream()
                .map(ug -> ug.getGroup().getId())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Long> getUserIdsByGroupId(Long groupId) {
        return userGroupRepository.findByGroup_Id(groupId).stream()
                .map(ug -> ug.getUser().getId())
                .toList();
    }

    @Transactional
    public void deleteByUserIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return;
        userGroupRepository.deleteByUser_IdIn(userIds);
    }

    @Transactional
    public void deleteByGroupIds(List<Long> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) return;
        userGroupRepository.deleteByGroup_IdIn(groupIds);
    }

    @Transactional(readOnly = true)
    public List<String> getGroupNamesByUserId(Long userId) {
        var ids = getGroupIdsByUserId(userId);
        return groupRepository.findAllById(ids).stream()
                .map(Group::getName)
                .collect(Collectors.toList());
    }
}
