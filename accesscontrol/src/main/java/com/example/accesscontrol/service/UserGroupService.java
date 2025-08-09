package com.example.accesscontrol.service;

import com.example.accesscontrol.dto.user.AssignUsersToGroupsRequest;
import com.example.accesscontrol.dto.user.AssignUsersToGroupsResponse;
import com.example.accesscontrol.dto.user.DeassignUsersFromGroupsRequest;
import com.example.accesscontrol.dto.user.DeassignUsersFromGroupsResponse;
import com.example.accesscontrol.entity.Group;
import com.example.accesscontrol.entity.UserGroup;
import com.example.accesscontrol.repository.GroupRepository;
import com.example.accesscontrol.repository.UserGroupRepository;
import com.example.accesscontrol.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserGroupService {

    private final UserGroupRepository userGroupRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;

    public AssignUsersToGroupsResponse assignUsersToGroups(AssignUsersToGroupsRequest request) {
        int assignedCount = 0;
        for (Long userId : request.getUserIds()) {
            for (Long groupId : request.getGroupIds()) {
                if (!userRepository.existsById(userId) || !groupRepository.existsById(groupId)) continue;
                boolean exists = userGroupRepository.existsByIdUserIdAndIdGroupId(userId, groupId);
                if (!exists) {
                    userGroupRepository.save(UserGroup.builder().id(new UserGroup.Id(userId, groupId)).build());
                    assignedCount++;
                }
            }
        }
        return AssignUsersToGroupsResponse.builder().message("Users assigned to groups successfully").assignedCount(assignedCount).build();
    }

    @Transactional
    public DeassignUsersFromGroupsResponse deassignUsersFromGroups(DeassignUsersFromGroupsRequest request) {
        int deletedCount = userGroupRepository.deleteByIdUserIdInAndIdGroupIdIn(request.getUserIds(), request.getGroupIds());
        return DeassignUsersFromGroupsResponse.builder().message("Users deassigned from groups successfully").removedCount(deletedCount).build();
    }

    public List<Long> getGroupIdsByUserId(Long userId) {
        return userGroupRepository.findByIdUserId(userId).stream().map(ug -> ug.getId().getGroupId()).toList();
    }

    public List<Long> getUserIdsByGroupId(Long groupId) {
        return userGroupRepository.findByIdGroupId(groupId).stream().map(ug -> ug.getId().getUserId()).toList();
    }

    @Transactional
    public void deleteByUserIds(List<Long> userIds) {
        userGroupRepository.deleteByIdUserIdIn(userIds);
    }

    @Transactional
    public void deleteByGroupIds(List<Long> groupIds) {
        userGroupRepository.deleteByIdGroupIdIn(groupIds);
    }

    public List<String> getGroupNamesByUserId(Long userId) {
        var ids = getGroupIdsByUserId(userId);
        return groupRepository.findAllById(ids).stream().map(Group::getName).collect(Collectors.toList());
    }
}
