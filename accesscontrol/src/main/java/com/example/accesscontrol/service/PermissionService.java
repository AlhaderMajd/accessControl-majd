package com.example.accesscontrol.service;

import com.example.accesscontrol.entity.Permission;
import com.example.accesscontrol.exception.ResourceNotFoundException;
import com.example.accesscontrol.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;

    public List<Permission> getPermissionsByRoleId(Long roleId) {
        return permissionRepository.findByRoleId(roleId);
    }

    public List<Permission> getByIdsOrThrow(List<Long> ids) {
        List<Permission> permissions = permissionRepository.findAllById(ids);
        if (permissions.size() != ids.size()) {
            throw new ResourceNotFoundException("Some permissions not found");
        }
        return permissions;
    }

}
