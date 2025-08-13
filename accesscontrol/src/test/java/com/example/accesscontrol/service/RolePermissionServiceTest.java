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
    void assignPermissionsToRoles_nullOrEmpty_returnsZero() {
        assertEquals(0, rolePermissionService.assignPermissionsToRoles(null, List.of(1L)));
        assertEquals(0, rolePermissionService.assignPermissionsToRoles(List.of(1L), null));
        assertEquals(0, rolePermissionService.assignPermissionsToRoles(List.of(), List.of(1L)));
        assertEquals(0, rolePermissionService.assignPermissionsToRoles(List.of(1L), List.of()));
        verify(rolePermissionRepository, never()).saveAll(anyList());
    }

    @Test
    void assignPermissionsToRoles_insertsOnlyMissing_andReturnsCount() {
        List<Long> roleIds = List.of(1L, 2L);
        List<Long> permissionIds = List.of(10L, 11L);

        RolePermission existing = new RolePermission();
        existing.setRole(Role.builder().id(1L).build());
        existing.setPermission(Permission.builder().id(10L).build());

        when(rolePermissionRepository.findByRole_IdInAndPermission_IdIn(roleIds, permissionIds))
                .thenReturn(List.of(existing));

        when(rolePermissionRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        int inserted = rolePermissionService.assignPermissionsToRoles(roleIds, permissionIds);

        assertEquals(3, inserted);
        verify(rolePermissionRepository).saveAll(anyList());
    }

    @Test
    void assignPermissionsToRoles_allExist_savesNothing_returnsZero() {
        List<Long> roleIds = List.of(1L);
        List<Long> permissionIds = List.of(10L);
        RolePermission existing = new RolePermission();
        existing.setRole(Role.builder().id(1L).build());
        existing.setPermission(Permission.builder().id(10L).build());
        when(rolePermissionRepository.findByRole_IdInAndPermission_IdIn(roleIds, permissionIds))
                .thenReturn(List.of(existing));

        int inserted = rolePermissionService.assignPermissionsToRoles(roleIds, permissionIds);
        assertEquals(0, inserted);
        verify(rolePermissionRepository, never()).saveAll(anyList());
    }

    @Test
    void deassignPermissionsFromRoles_nullOrEmpty_returnsZero() {
        assertEquals(0, rolePermissionService.deassignPermissionsFromRoles(null, List.of(1L)));
        assertEquals(0, rolePermissionService.deassignPermissionsFromRoles(List.of(1L), null));
        assertEquals(0, rolePermissionService.deassignPermissionsFromRoles(List.of(), List.of(1L)));
        assertEquals(0, rolePermissionService.deassignPermissionsFromRoles(List.of(1L), List.of()));
        verify(rolePermissionRepository, never()).deleteByRole_IdInAndPermission_IdIn(anyList(), anyList());
    }

    @Test
    void deassignPermissionsFromRoles_valid_delegatesAndReturnsCount() {
        when(rolePermissionRepository.deleteByRole_IdInAndPermission_IdIn(List.of(1L), List.of(2L)))
                .thenReturn(5);
        int count = rolePermissionService.deassignPermissionsFromRoles(List.of(1L), List.of(2L));
        assertEquals(5, count);
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
