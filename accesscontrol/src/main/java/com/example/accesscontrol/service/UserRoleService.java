package com.example.accesscontrol.service;

import com.example.accesscontrol.dto.AssignRolesRequest;
import com.example.accesscontrol.dto.AssignRolesResponse;
import com.example.accesscontrol.entity.Role;
import com.example.accesscontrol.entity.User;
import com.example.accesscontrol.entity.UserRole;
import com.example.accesscontrol.exception.ResourceNotFoundException;
import com.example.accesscontrol.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserRoleService {

    private final UserRoleRepository userRoleRepository;
    private final RoleService roleService;

    public List<String> getRoleNamesByUserId(Long userId) {
        List<UserRole> userRoles = userRoleRepository.findByUserId(userId);

        return userRoles.stream()
                .map(ur -> roleService.getByIdOrThrow(ur.getRoleId()))
                .map(Role::getName)
                .collect(Collectors.toList());
    }

    public Role assignRoleToUser(Long userId, String roleName) {
        Role role = roleService.getOrCreateRole(roleName);
        userRoleRepository.save(new UserRole(userId, role.getId()));
        return role;
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
}
