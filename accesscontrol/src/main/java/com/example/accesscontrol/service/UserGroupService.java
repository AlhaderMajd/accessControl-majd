package com.example.accesscontrol.service;

import com.example.accesscontrol.dto.AssignUsersToGroupsRequest;
import com.example.accesscontrol.dto.AssignUsersToGroupsResponse;
import com.example.accesscontrol.entity.Group;
import com.example.accesscontrol.entity.UserGroup;
import com.example.accesscontrol.exception.ResourceNotFoundException;
import com.example.accesscontrol.repository.UserGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserGroupService {

    private final UserGroupRepository userGroupRepository;
    private final GroupService groupService;

    public AssignUsersToGroupsResponse assignUsersToGroups(AssignUsersToGroupsRequest request) {
        int assignedCount = 0;

        for (Long userId : request.getUserIds()) {
            for (Long groupId : request.getGroupIds()) {
                // Check if the mapping already exists
                boolean exists = userGroupRepository.existsByUserIdAndGroupId(userId, groupId);
                if (!exists) {
                    UserGroup userGroup = new UserGroup(userId, groupId);
                    userGroupRepository.save(userGroup);
                    assignedCount++;
                }
            }
        }

        return AssignUsersToGroupsResponse.builder()
                .message("Users assigned to groups successfully")
                .assignedCount(assignedCount)
                .build();
    }

    public List<String> getGroupNamesByUserId(Long userId) {
        List<UserGroup> userGroups = userGroupRepository.findByUserId(userId);

        return userGroups.stream()
                .map(ug -> groupService.getByIdOrThrow(ug.getGroupId()))
                .map(Group::getName)
                .collect(Collectors.toList());
    }

}
