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

    @Transactional
    public int assignRolesToUsers(List<Long> userIds, List<Long> roleIds) {
        if (userIds == null || userIds.isEmpty() || roleIds == null || roleIds.isEmpty()) return 0;

        var existingPairs = userRoleRepository.findByUser_IdInAndRole_IdIn(userIds, roleIds);
        Set<String> existingKeys = existingPairs.stream()
                .map(ur -> ur.getUser().getId() + "_" + ur.getRole().getId())
                .collect(Collectors.toSet());

        List<UserRole> toInsert = new ArrayList<>();
        for (Long uId : userIds) {
            for (Long rId : roleIds) {
                String key = uId + "_" + rId;
                if (!existingKeys.contains(key)) {
                    UserRole ur = new UserRole();
                    ur.setUser(User.builder().id(uId).build());
                    ur.setRole(Role.builder().id(rId).build());
                    toInsert.add(ur);
                }
            }
        }

        if (!toInsert.isEmpty()) userRoleRepository.saveAll(toInsert);
        return toInsert.size();
    }

    @Transactional
    public DeassignRolesResponse deassignRoles(List<User> users, List<Role> roles) {
        List<Long> userIds = users.stream().map(User::getId).toList();
        List<Long> roleIds = roles.stream().map(Role::getId).toList();
        int removed = userRoleRepository.deleteByUser_IdInAndRole_IdIn(userIds, roleIds);
        return DeassignRolesResponse.builder().message("Roles deassigned successfully").removedCount(removed).build();
    }

    @Transactional
    public void deleteByUserIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return;
        userRoleRepository.deleteByUser_IdIn(userIds);
    }

    @Transactional
    public void deleteByRoleIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) return;
        userRoleRepository.deleteAllByRole_IdIn(roleIds);
    }
}
