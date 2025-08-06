package com.example.accesscontrol.service;

import com.example.accesscontrol.entity.Role;
import com.example.accesscontrol.entity.User;
import com.example.accesscontrol.entity.UserRole;
import com.example.accesscontrol.exception.ResourceNotFoundException;
import com.example.accesscontrol.repository.RoleRepository;
import com.example.accesscontrol.repository.UserRepository;
import com.example.accesscontrol.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserRoleService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;

    public Role assignRoleToUser(Long userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        UserRole userRole = new UserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(role.getId());

        userRoleRepository.save(userRole);
        return role;
    }
}
