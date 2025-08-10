package com.example.accesscontrol.service;

import com.example.accesscontrol.dto.user.deassignUsersFromUsers.DeassignRolesResponse;
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

    public int assignRolesToUsers(List<Long> userIds, List<Long> roleIds) {
        if (userIds == null || userIds.isEmpty() || roleIds == null || roleIds.isEmpty()) return 0;

        var existingPairs = userRoleRepository.findByIdUserIdInAndIdRoleIdIn(userIds, roleIds);
        Set<String> existingKeys = existingPairs.stream()
                .map(ur -> ur.getId().getUserId() + "_" + ur.getId().getRoleId())
                .collect(Collectors.toSet());

        var newAssignments = userIds.stream()
                .flatMap(u -> roleIds.stream().map(r -> UserRole.builder().id(new UserRole.Id(u, r)).build()))
                .filter(ur -> !existingKeys.contains(ur.getId().getUserId() + "_" + ur.getId().getRoleId()))
                .toList();

        userRoleRepository.saveAll(newAssignments);
        return newAssignments.size();
    }

    public DeassignRolesResponse deassignRoles(List<User> users, List<Role> roles) {
        List<Long> userIds = users.stream().map(User::getId).toList();
        List<Long> roleIds = roles.stream().map(Role::getId).toList();
        int removed = userRoleRepository.deleteByIdUserIdInAndIdRoleIdIn(userIds, roleIds);
        return DeassignRolesResponse.builder().message("Roles deassigned successfully").removedCount(removed).build();
    }

    public void deleteByUserIds(List<Long> userIds) {
        userRoleRepository.deleteByIdUserIdIn(userIds);
    }

    @Transactional
    public void deleteByRoleIds(List<Long> roleIds) {
        userRoleRepository.deleteAllByIdRoleIdIn(roleIds);
    }
}
