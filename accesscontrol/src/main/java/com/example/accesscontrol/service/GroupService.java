package com.example.accesscontrol.service;

import com.example.accesscontrol.dto.common.PageResponse;
import com.example.accesscontrol.dto.group.*;
import com.example.accesscontrol.dto.role.RoleResponse;
import com.example.accesscontrol.dto.user.getUsers.UserSummaryResponse;
import com.example.accesscontrol.entity.Group;
import com.example.accesscontrol.exception.ResourceNotFoundException;
import com.example.accesscontrol.repository.GroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final UserGroupService userGroupService;
    private final GroupRoleService groupRoleService;
    private final UserService userService;
    private final RoleService roleService;

    @Transactional(readOnly = true)
    public Group getByIdOrThrow(Long id) {
        return groupRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Group not found"));
    }

    @Transactional
    public CreateGroupsResponse createGroups(List<CreateGroupRequest> items) {
        if (items == null || items.isEmpty()) throw new IllegalArgumentException("Group names are required");

        List<String> names = items.stream()
                .map(CreateGroupRequest::getName)
                .map(n -> n == null ? "" : n.trim())
                .filter(n -> !n.isBlank())
                .toList();
        if (names.size() != items.size()) throw new IllegalArgumentException("Group names are required");

        var existing = groupRepository.findByNameInIgnoreCase(names).stream()
                .map(Group::getName)
                .collect(Collectors.toSet());
        if (!existing.isEmpty()) throw new IllegalStateException("Some group names already exist: " + existing);

        var saved = groupRepository.saveAll(
                names.stream().map(n -> Group.builder().name(n).build()).toList()
        );

        var itemsResp = saved.stream()
                .map(g -> GroupResponse.builder().id(g.getId()).name(g.getName()).build())
                .toList();

        return CreateGroupsResponse.builder()
                .message("Groups created successfully")
                .createdCount(saved.size())
                .items(itemsResp)
                .build();
    }

    @Transactional(readOnly = true)
    public PageResponse<GroupResponse> getGroups(String search, int page, int size) {
        if (page < 0 || size <= 0) throw new IllegalArgumentException("Invalid pagination parameters");
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<Group> pg = groupRepository.findByNameContainingIgnoreCase(search == null ? "" : search, pageable);

        var items = pg.getContent().stream()
                .map(g -> GroupResponse.builder().id(g.getId()).name(g.getName()).build())
                .toList();

        return PageResponse.<GroupResponse>builder()
                .items(items).page(page).size(size).total(pg.getTotalElements()).build();
    }

    @Transactional(readOnly = true)
    public GroupDetailsResponse getGroupDetails(Long groupId) {
        Group group = getByIdOrThrow(groupId);
        var userIds = userGroupService.getUserIdsByGroupId(groupId);
        var roleIds = groupRoleService.getRoleIdsByGroupId(groupId);

        List<UserSummaryResponse> users = userService.getUserSummariesByIds(userIds);
        List<RoleResponse> roles = roleService.getRoleSummariesByIds(roleIds);

        return GroupDetailsResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .users(users)
                .roles(roles)
                .build();
    }

    @Transactional
    public UpdateGroupNameResponse updateGroupName(Long groupId, UpdateGroupNameRequest request) {
        String newName = request.getName();
        if (newName == null || newName.isBlank()) throw new IllegalArgumentException("Invalid or missing group name");
        Group group = getByIdOrThrow(groupId);
        String old = group.getName();
        group.setName(newName.trim());
        groupRepository.save(group);
        return UpdateGroupNameResponse.builder()
                .message("Group name updated successfully")
                .id(group.getId())
                .oldName(old)
                .newName(group.getName())
                .build();
    }

    @Transactional
    public com.example.accesscontrol.dto.common.MessageResponse deleteGroups(List<Long> groupIds) {
        if (groupIds == null || groupIds.isEmpty())
            throw new IllegalArgumentException("Invalid or empty group IDs list");
        var existingIds = groupRepository.findAllById(groupIds).stream().map(Group::getId).toList();
        if (existingIds.isEmpty()) throw new ResourceNotFoundException("No matching groups found");

        userGroupService.deleteByGroupIds(existingIds);
        groupRoleService.deleteByGroupIds(existingIds);
        groupRepository.deleteAllById(existingIds);

        return com.example.accesscontrol.dto.common.MessageResponse.builder()
                .message("Group(s) deleted successfully")
                .build();
    }
}
