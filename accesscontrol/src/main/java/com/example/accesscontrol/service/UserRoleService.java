package com.example.accesscontrol.service;

import com.example.accesscontrol.entity.Role;
import com.example.accesscontrol.entity.User;
import com.example.accesscontrol.entity.UserRole;
import com.example.accesscontrol.exception.ResourceNotFoundException;
import com.example.accesscontrol.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserRoleService {

    private final UserRoleRepository userRoleRepository;
    private final RoleService roleService;

    // ✅ Used in UserService → getUserDetails
    public List<String> getRoleNamesByUserId(Long userId) {
        List<UserRole> userRoles = userRoleRepository.findByUserId(userId);

        return userRoles.stream()
                .map(ur -> roleService.getByIdOrThrow(ur.getRoleId()))
                .map(Role::getName)
                .collect(Collectors.toList());
    }

    // ✅ Used in UserService → createUsers (assign MEMBER role)
    public Role assignRoleToUser(Long userId, String roleName) {
        Role role = roleService.getOrCreateRole(roleName);

        UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(role.getId());

        userRoleRepository.save(userRole);
        return role;
    }
}
