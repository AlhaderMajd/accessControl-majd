package com.example.accesscontrol.service;

import com.example.accesscontrol.dto.user.deassignUsersFromUsers.DeassignRolesResponse;
import com.example.accesscontrol.entity.Role;
import com.example.accesscontrol.entity.User;
import com.example.accesscontrol.entity.UserRole;
import com.example.accesscontrol.repository.UserRoleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
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

        var existingBefore = userRoleRepository.findByUser_IdInAndRole_IdIn(userIds, roleIds);
        var existingKeys = existingBefore.stream()
                .map(ur -> ur.getUser().getId() + "_" + ur.getRole().getId())
                .collect(java.util.stream.Collectors.toSet());

        List<UserRole> toInsert = new java.util.ArrayList<>();
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

        if (toInsert.isEmpty()) return 0;

        try {
            userRoleRepository.saveAll(toInsert);
            return toInsert.size();
        } catch (DataIntegrityViolationException ex) {
            log.info("users.roles.assign race_detected: unique_violation during saveAll, recomputing delta");
            var existingAfter = userRoleRepository.findByUser_IdInAndRole_IdIn(userIds, roleIds);
            int delta = Math.max(0, existingAfter.size() - existingBefore.size());
            return delta;
        }
    }

    @Transactional
    public DeassignRolesResponse deassignRoles(List<User> users, List<Role> roles) {
        List<Long> userIds = users.stream().map(User::getId).toList();
        List<Long> roleIds = roles.stream().map(Role::getId).toList();
        int removed = userRoleRepository.deleteByUser_IdInAndRole_IdIn(userIds, roleIds);

        log.info("users.roles.deassign repo removed={}", removed);

        return DeassignRolesResponse.builder()
                .message(removed > 0 ? "Roles deassigned successfully" : "No roles were deassigned")
                .removedCount(removed)
                .build();
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
