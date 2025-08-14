package com.example.accesscontrol.service;

import com.example.accesscontrol.dto.user.assignUsersToGroup.AssignUsersToGroupsRequest;
import com.example.accesscontrol.dto.user.assignUsersToGroup.AssignUsersToGroupsResponse;
import com.example.accesscontrol.dto.user.deassignUsersFromGroups.DeassignUsersFromGroupsRequest;
import com.example.accesscontrol.dto.user.deassignUsersFromGroups.DeassignUsersFromGroupsResponse;
import com.example.accesscontrol.entity.Group;
import com.example.accesscontrol.entity.User;
import com.example.accesscontrol.entity.UserGroup;
import com.example.accesscontrol.repository.GroupRepository;
import com.example.accesscontrol.repository.UserGroupRepository;
import com.example.accesscontrol.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserGroupService {

    private final UserGroupRepository userGroupRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;

    @Transactional(readOnly = true)
    public List<String> getGroupNamesByUserId(Long userId) {
        return userGroupRepository.findGroupNamesByUserId(userId);
    }

    @Transactional
    public AssignUsersToGroupsResponse assignUsersToGroups(AssignUsersToGroupsRequest request) {
        if (request == null
                || request.getUserIds() == null || request.getUserIds().isEmpty()
                || request.getGroupIds() == null || request.getGroupIds().isEmpty()) {
            return AssignUsersToGroupsResponse.builder()
                    .message("Nothing to assign")
                    .assignedCount(0)
                    .build();
        }

        var userIds = request.getUserIds().stream()
                .filter(Objects::nonNull).filter(id -> id > 0)
                .distinct().toList();
        var groupIds = request.getGroupIds().stream()
                .filter(Objects::nonNull).filter(id -> id > 0)
                .distinct().toList();

        if (userIds.isEmpty() || groupIds.isEmpty()) {
            return AssignUsersToGroupsResponse.builder()
                    .message("Nothing to assign")
                    .assignedCount(0)
                    .build();
        }

        var existingUserIds = userRepository.findAllById(userIds).stream()
                .map(User::getId).collect(java.util.stream.Collectors.toSet());
        var existingGroupIds = groupRepository.findAllById(groupIds).stream()
                .map(Group::getId).collect(java.util.stream.Collectors.toSet());

        if (existingUserIds.isEmpty() || existingGroupIds.isEmpty()) {
            return AssignUsersToGroupsResponse.builder()
                    .message("Nothing to assign")
                    .assignedCount(0)
                    .build();
        }

        var existingLinks = userGroupRepository.findByUser_IdInAndGroup_IdIn(
                new java.util.ArrayList<>(existingUserIds),
                new java.util.ArrayList<>(existingGroupIds));

        var existingKeys = existingLinks.stream()
                .map(ug -> ug.getUser().getId() + "_" + ug.getGroup().getId())
                .collect(java.util.stream.Collectors.toSet());

        List<UserGroup> toInsert = new java.util.ArrayList<>();
        for (Long uId : existingUserIds) {
            for (Long gId : existingGroupIds) {
                String key = uId + "_" + gId;
                if (!existingKeys.contains(key)) {
                    UserGroup ug = new UserGroup();
                    ug.setUser(User.builder().id(uId).build());
                    ug.setGroup(Group.builder().id(gId).build());
                    toInsert.add(ug);
                }
            }
        }

        if (toInsert.isEmpty()) {
            log.info("users.groups.assign no_new_pairs users={} groups={} inserted=0",
                    existingUserIds.size(), existingGroupIds.size());
            return AssignUsersToGroupsResponse.builder()
                    .message("Users assigned to groups successfully")
                    .assignedCount(0)
                    .build();
        }

        int inserted;
        try {
            userGroupRepository.saveAll(toInsert);
            inserted = toInsert.size();
        } catch (DataIntegrityViolationException ex) {
            var after = userGroupRepository.findByUser_IdInAndGroup_IdIn(
                    new java.util.ArrayList<>(existingUserIds),
                    new java.util.ArrayList<>(existingGroupIds));
            inserted = Math.max(0, after.size() - existingLinks.size());
        }

        log.info("users.groups.assign success users={} groups={} inserted={}",
                existingUserIds.size(), existingGroupIds.size(), inserted);

        return AssignUsersToGroupsResponse.builder()
                .message("Users assigned to groups successfully")
                .assignedCount(inserted)
                .build();
    }

    @Transactional
    public DeassignUsersFromGroupsResponse deassignUsersFromGroups(DeassignUsersFromGroupsRequest request) {
        var userIds = request.getUserIds();
        var groupIds = request.getGroupIds();

        if (userIds == null || userIds.isEmpty() || groupIds == null || groupIds.isEmpty())
            throw new IllegalArgumentException("User or group list is invalid");

        var groups = groupRepository.findAllById(groupIds);
        if (groups.size() != groupIds.size()) {
            var found = groups.stream().map(Group::getId).collect(java.util.stream.Collectors.toSet());
            var missingGroups = groupIds.stream().filter(id -> !found.contains(id)).toList();
            throw new com.example.accesscontrol.exception.ResourceNotFoundException(
                    "Some groups not found: " + missingGroups);
        }

        int deletedCount = userGroupRepository.deleteByUser_IdInAndGroup_IdIn(userIds, groupIds);

        return DeassignUsersFromGroupsResponse.builder()
                .message(deletedCount > 0 ? "Users deassigned from groups successfully"
                        : "No memberships were removed")
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

    private String mask(String email) {
        if (email == null || !email.contains("@")) return "unknown";
        String[] p = email.split("@", 2);
        return (p[0].isEmpty() ? "*" : p[0].substring(0,1)) + "***@" + p[1];
    }
}
