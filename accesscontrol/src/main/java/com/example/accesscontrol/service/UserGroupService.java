package com.example.accesscontrol.service;

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

    public List<String> getGroupNamesByUserId(Long userId) {
        List<UserGroup> userGroups = userGroupRepository.findByUserId(userId);

        return userGroups.stream()
                .map(ug -> groupService.getByIdOrThrow(ug.getGroupId()))
                .map(Group::getName)
                .collect(Collectors.toList());
    }
}
