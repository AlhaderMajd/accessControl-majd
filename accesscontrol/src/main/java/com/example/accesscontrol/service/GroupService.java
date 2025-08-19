package com.example.accesscontrol.service;

import com.example.accesscontrol.dto.common.MessageResponse;
import com.example.accesscontrol.dto.common.PageResponse;
import com.example.accesscontrol.dto.group.*;
import com.example.accesscontrol.dto.role.RoleResponse;
import com.example.accesscontrol.dto.user.getUsers.UserSummaryResponse;
import com.example.accesscontrol.entity.Group;
import com.example.accesscontrol.entity.Role;
import com.example.accesscontrol.entity.User;
import com.example.accesscontrol.exception.ResourceNotFoundException;
import com.example.accesscontrol.repository.GroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Collator;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;

    @Transactional
    public CreateGroupsResponse createGroups(List<CreateGroupRequest> items) {
        if (items == null || items.isEmpty())
            throw new IllegalArgumentException("Group names are required");

        List<String> names = items.stream()
                .map(CreateGroupRequest::getName)
                .map(n -> n == null ? "" : n.trim())
                .filter(n -> !n.isBlank())
                .toList();
        if (names.size() != items.size())
            throw new IllegalArgumentException("Group names are required");

        var dupNames = names.stream()
                .collect(java.util.stream.Collectors.groupingBy(s -> s.toLowerCase(java.util.Locale.ROOT),
                        java.util.stream.Collectors.counting()))
                .entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .map(Map.Entry::getKey)
                .toList();
        if (!dupNames.isEmpty()) {
            throw new com.example.accesscontrol.exception.DuplicateResourceException(
                    "Duplicate group names in request: " + dupNames);
        }

        var existing = groupRepository.findByNameInIgnoreCase(
                names.stream().map(s -> s.toLowerCase(Locale.ROOT)).toList()
        ).stream().map(Group::getName).collect(java.util.stream.Collectors.toSet());
        if (!existing.isEmpty()) {
            throw new com.example.accesscontrol.exception.DuplicateResourceException(
                    "Some group names already exist: " + existing);
        }

        List<Group> saved;
        try {
            saved = groupRepository.saveAll(
                    names.stream().map(n -> Group.builder().name(n).build()).toList()
            );
        } catch (DataIntegrityViolationException e) {
            var nowExisting = groupRepository.findByNameInIgnoreCase(
                    names.stream().map(s -> s.toLowerCase(Locale.ROOT)).toList()
            ).stream().map(Group::getName).collect(java.util.stream.Collectors.toSet());
            throw new com.example.accesscontrol.exception.DuplicateResourceException(
                    "Some group names already exist: " + nowExisting);
        }

        var itemsResp = saved.stream()
                .map(g -> GroupResponse.builder().id(g.getId()).name(g.getName()).build())
                .toList();

        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String actor = auth == null ? "unknown" : auth.getName();
        log.info("groups.create success actor={} created={}", mask(actor), saved.size());

        return CreateGroupsResponse.builder()
                .message("Groups created successfully")
                .createdCount(saved.size())
                .items(itemsResp)
                .build();
    }

    @Transactional(readOnly = true)
    public PageResponse<GroupResponse> getGroups(String search, int page, int size) {
        final String q = (search == null ? "" : search.trim());
        final int pageSafe = Math.max(0, page);
        final int sizeSafe = Math.min(Math.max(1, size), 100);
        final Pageable pageable = PageRequest.of(pageSafe, sizeSafe, Sort.by(Sort.Direction.DESC, "id"));

        Page<Group> pg = groupRepository.findByNameContainingIgnoreCase(q, pageable);

        var items = pg.getContent().stream()
                .map(g -> GroupResponse.builder().id(g.getId()).name(g.getName()).build())
                .toList();

        return PageResponse.<GroupResponse>builder()
                .items(items)
                .page(pageSafe)
                .size(sizeSafe)
                .total(pg.getTotalElements())
                .build();
    }

    @Transactional(readOnly = true)
    public GroupDetailsResponse getGroupDetails(Long groupId) {
        Group group = groupRepository.findWithUsersAndRolesById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));

        List<UserSummaryResponse> users = group.getUsers().stream()
                .map(u -> UserSummaryResponse.builder()
                        .id(u.getId())
                        .email(u.getEmail())
                        .enabled(u.isEnabled())
                        .build())
                .sorted(Comparator
                        .comparing(UserSummaryResponse::getEmail, Collator.getInstance())
                        .thenComparing(UserSummaryResponse::getId))
                .toList();

        List<RoleResponse> roles = group.getRoles().stream()
                .map(r -> RoleResponse.builder().id(r.getId()).name(r.getName()).build())
                .sorted(Comparator
                        .comparing(RoleResponse::getName, Collator.getInstance())
                        .thenComparing(RoleResponse::getId))
                .toList();

        log.info("groups.details success groupId={} users={} roles={}",
                group.getId(), users.size(), roles.size());

        return GroupDetailsResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .users(users)
                .roles(roles)
                .build();
    }

    @Transactional
    public UpdateGroupNameResponse updateGroupName(Long groupId, UpdateGroupNameRequest request) {
        String newName = (request == null || request.getName() == null) ? null : request.getName().trim();
        if (newName == null || newName.isEmpty()) {
            throw new IllegalArgumentException("Invalid or missing group name");
        }

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));

        String old = group.getName();
        if (old != null && old.equalsIgnoreCase(newName)) {
            log.info("groups.update_name no_change groupId={} name='{}'", groupId, old);
            return UpdateGroupNameResponse.builder()
                    .message("Group name updated successfully")
                    .id(group.getId())
                    .oldName(old)
                    .newName(old)
                    .build();
        }

        var dup = groupRepository.findByNameInIgnoreCase(List.of(newName.toLowerCase(Locale.ROOT)))
                .stream().filter(g -> !g.getId().equals(groupId)).findFirst();
        if (dup.isPresent()) {
            throw new com.example.accesscontrol.exception.DuplicateResourceException("Group name already exists");
        }

        group.setName(newName);
        try {
            groupRepository.save(group);
        } catch (DataIntegrityViolationException e) {
            throw new com.example.accesscontrol.exception.DuplicateResourceException("Group name already exists");
        }

        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String actor = auth == null ? "unknown" : auth.getName();
        log.info("groups.update_name success actor={} groupId={} old='{}' new='{}'",
                mask(actor), groupId, old, newName);

        return UpdateGroupNameResponse.builder()
                .message("Group name updated successfully")
                .id(group.getId())
                .oldName(old)
                .newName(group.getName())
                .build();
    }

    @Transactional
    public MessageResponse deleteGroups(List<Long> groupIds) {
        if (groupIds == null || groupIds.isEmpty())
            throw new IllegalArgumentException("Invalid or empty group IDs list");

        var ids = groupIds.stream().filter(Objects::nonNull).filter(id -> id > 0).distinct().toList();
        if (ids.isEmpty())
            throw new IllegalArgumentException("Invalid or empty group IDs list");

        var existing = groupRepository.findAllById(ids);
        if (existing.size() != ids.size()) {
            var found = existing.stream().map(Group::getId).collect(java.util.stream.Collectors.toSet());
            var missing = ids.stream().filter(id -> !found.contains(id)).toList();
            throw new com.example.accesscontrol.exception.ResourceNotFoundException("Some groups not found: " + missing);
        }

        try {
            for (Group g : existing) {
                for (User u : new ArrayList<>(g.getUsers())) {
                    u.getGroups().remove(g);
                }
                for (Role r : new ArrayList<>(g.getRoles())) {
                    r.getGroups().remove(g);
                }
            }
            groupRepository.deleteAllInBatch(existing);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("Cannot delete groups due to existing references: " +
                    (ex.getMostSpecificCause() == null ? ex.getMessage() : ex.getMostSpecificCause().getMessage()));
        }

        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String actor = auth == null ? "unknown" : auth.getName();
        log.info("groups.delete success actor={} deleted={}", mask(actor), ids.size());

        return MessageResponse.builder().message("Group(s) deleted successfully").build();
    }

    private String mask(String email) {
        if (email == null || !email.contains("@")) return "unknown";
        String[] p = email.split("@", 2);
        return (p[0].isEmpty() ? "*" : p[0].substring(0,1)) + "***@" + p[1];
    }
}
