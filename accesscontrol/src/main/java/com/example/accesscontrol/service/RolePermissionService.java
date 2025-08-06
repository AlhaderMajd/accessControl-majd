package com.example.accesscontrol.service;

import com.example.accesscontrol.entity.RolePermission;
import com.example.accesscontrol.repository.RolePermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RolePermissionService {

    private final RolePermissionRepository rolePermissionRepository;

    public void saveAll(List<RolePermission> rolePermissions) {
        rolePermissionRepository.saveAll(rolePermissions);
    }
}
