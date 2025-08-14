package com.example.accesscontrol.service;

import com.example.accesscontrol.entity.Permission;
import com.example.accesscontrol.entity.Role;
import com.example.accesscontrol.entity.RolePermission;
import com.example.accesscontrol.repository.RolePermissionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RolePermissionServiceTest {

    @Mock
    private RolePermissionRepository rolePermissionRepository;

    @InjectMocks
    private RolePermissionService rolePermissionService;

    @Test
    void saveAll_nullOrEmpty_noCall() {
        rolePermissionService.saveAll(null);
        rolePermissionService.saveAll(new ArrayList<>());
        verify(rolePermissionRepository, never()).saveAll(anyList());
    }

    @Test
    void saveAll_nonEmpty_callsRepository() {
        List<RolePermission> list = List.of(new RolePermission());
        rolePermissionService.saveAll(list);
        verify(rolePermissionRepository).saveAll(list);
    }

    @Test
    void deleteByRoleIds_nullOrEmpty_noCall() {
        rolePermissionService.deleteByRoleIds(null);
        rolePermissionService.deleteByRoleIds(List.of());
        verify(rolePermissionRepository, never()).deleteByRole_IdIn(anyList());
    }

    @Test
    void deleteByRoleIds_valid_callsRepo() {
        rolePermissionService.deleteByRoleIds(List.of(1L, 2L));
        verify(rolePermissionRepository).deleteByRole_IdIn(List.of(1L, 2L));
    }

    @Test
    void deleteByPermissionIds_nullOrEmpty_noCall() {
        rolePermissionService.deleteByPermissionIds(null);
        rolePermissionService.deleteByPermissionIds(List.of());
        verify(rolePermissionRepository, never()).deleteByPermission_IdIn(anyList());
    }

    @Test
    void deleteByPermissionIds_valid_callsRepo() {
        rolePermissionService.deleteByPermissionIds(List.of(10L));
        verify(rolePermissionRepository).deleteByPermission_IdIn(List.of(10L));
    }
}
