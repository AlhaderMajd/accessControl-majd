package com.example.accesscontrol.service;

import com.example.accesscontrol.dto.DeassignRolesResponse;
import com.example.accesscontrol.entity.Role;
import com.example.accesscontrol.entity.User;
import com.example.accesscontrol.entity.UserRole;
import com.example.accesscontrol.repository.UserRoleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserRoleService {

    private final UserRoleRepository userRoleRepository;

    public List<String> getRoleNamesByUserId(Long userId) {
        return userRoleRepository.findRoleNamesByUserId(userId);
    }

    public Role assignRoleToUser(Long userId, String roleName) {
        // You can optionally keep this logic if you really need RoleService here, but it will reintroduce circular dependency.
        throw new UnsupportedOperationException("This method requires RoleService and should be avoided to prevent circular dependencies.");
    }

    public int assignRolesToUsers(List<Long> userIds, List<Long> roleIds) {
        List<UserRole> existingPairs = userRoleRepository.findByUserIdInAndRoleIdIn(userIds, roleIds);

        Set<String> existingKeys = existingPairs.stream()
                .map(ur -> ur.getUserId() + "_" + ur.getRoleId())
                .collect(Collectors.toSet());

        List<UserRole> newAssignments = userIds.stream()
                .flatMap(u -> roleIds.stream().map(r -> new UserRole(u, r)))
                .filter(ur -> !existingKeys.contains(ur.getUserId() + "_" + ur.getRoleId()))
                .toList();

        userRoleRepository.saveAll(newAssignments);
        return newAssignments.size();
    }

    public DeassignRolesResponse deassignRoles(List<User> users, List<Role> roles) {
        List<Long> userIds = users.stream().map(User::getId).toList();
        List<Long> roleIds = roles.stream().map(Role::getId).toList();

        int removed = userRoleRepository.deleteByUserIdInAndRoleIdIn(userIds, roleIds);

        return DeassignRolesResponse.builder()
                .message("Roles deassigned successfully")
                .removedCount(removed)
                .build();
    }

    public void deleteByUserIds(List<Long> userIds) {
        userRoleRepository.deleteByUserIdIn(userIds);
    }

    @Transactional
    public void deleteByRoleIds(List<Long> roleIds) {
        userRoleRepository.deleteAllByRoleIdIn(roleIds);
    }
}
