package com.example.accesscontrol.service;

import com.example.accesscontrol.dto.group.AssignRolesToGroupsRequest;
import com.example.accesscontrol.dto.permission.PermissionResponse;
import com.example.accesscontrol.dto.role.*;
import com.example.accesscontrol.entity.Permission;
import com.example.accesscontrol.entity.Role;
import com.example.accesscontrol.exception.DuplicateResourceException;
import com.example.accesscontrol.exception.ResourceNotFoundException;
import com.example.accesscontrol.repository.RoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock private RoleRepository roleRepository;
    @Mock private PermissionService permissionService;
    @Mock private RolePermissionService rolePermissionService;
    @Mock private GroupRoleService groupRoleService;
    @Mock private UserRoleService userRoleService;

    @InjectMocks private RoleService roleService;

    @Test
    void getOrCreateRole_createsWhenMissing() {
        when(roleRepository.findByName("MEMBER")).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenAnswer(inv -> {
            Role r = inv.getArgument(0);
            r.setId(1L);
            return r;
        });

        Role r = roleService.getOrCreateRole("MEMBER");

        assertEquals("MEMBER", r.getName());
        assertEquals(1L, r.getId());
    }

    @Test
    void getByIdsOrThrow_throwsWhenMissing() {
        when(roleRepository.findAllById(List.of(1L, 2L)))
                .thenReturn(List.of(Role.builder().id(1L).name("A").build()));

        assertThrows(ResourceNotFoundException.class, () -> roleService.getByIdsOrThrow(List.of(1L, 2L)));
    }

    @Test
    void createRoles_success_withPermissions() {
        CreateRoleRequest r1 = new CreateRoleRequest();
        r1.setName("ADMIN");
        r1.setPermissionIds(List.of(10L, 11L));

        CreateRoleRequest r2 = new CreateRoleRequest();
        r2.setName("USER");

        List<CreateRoleRequest> reqs = List.of(r1, r2);

        when(roleRepository.findExistingNames(anyList())).thenReturn(List.of());
        // Ensure saved roles have IDs for name->id mapping
        when(roleRepository.saveAll(anyList())).thenAnswer(inv -> {
            @SuppressWarnings("unchecked")
            List<Role> in = (List<Role>) inv.getArgument(0);
            long[] seq = {1L};
            in.forEach(role -> role.setId(seq[0]++));
            return in;
        });

        CreateRoleResponse resp = roleService.createRoles(reqs);

        assertEquals("Roles created successfully", resp.getMessage());
        assertEquals(List.of("ADMIN", "USER"), resp.getCreated());
        verify(rolePermissionService).saveAll(anyList());
    }

    @Test
    void createRoles_invalid_throws() {
        assertThrows(IllegalArgumentException.class, () -> roleService.createRoles(List.of()));
        CreateRoleRequest bad = new CreateRoleRequest();
        bad.setName("   ");
        assertThrows(IllegalArgumentException.class, () -> roleService.createRoles(List.of(bad)));
    }

    @Test
    void createRoles_duplicateNames_throws() {
        CreateRoleRequest r = new CreateRoleRequest();
        r.setName("ADMIN");
        when(roleRepository.findExistingNames(anyList())).thenReturn(List.of("ADMIN"));
        assertThrows(DuplicateResourceException.class, () -> roleService.createRoles(List.of(r)));
    }

    @Test
    void getRoles_page() {
        Page<Role> page = new PageImpl<>(
                List.of(Role.builder().id(2L).name("USER").build()),
                PageRequest.of(0, 10),
                1
        );
        when(roleRepository.findByNameContainingIgnoreCase(anyString(), any())).thenReturn(page);

        GetRolesResponse resp = roleService.getRoles("user", 0, 10);

        assertEquals(1, resp.getTotal());
        assertEquals("USER", resp.getRoles().get(0).getName());
    }

    @Test
    void getRoles_invalid_throws() {
        assertThrows(IllegalArgumentException.class, () -> roleService.getRoles("x", -1, 10));
        assertThrows(IllegalArgumentException.class, () -> roleService.getRoles("x", 0, 0));
    }

    @Test
    void getRoleWithPermissions_success() {
        Role r = Role.builder().id(3L).name("ADMIN").build();
        when(roleRepository.findById(3L)).thenReturn(Optional.of(r));
        when(permissionService.getPermissionsByRoleId(3L))
                .thenReturn(List.of(Permission.builder().id(9L).name("READ").build()));

        RoleDetailsResponse resp = roleService.getRoleWithPermissions(3L);

        assertEquals("ADMIN", resp.getName());
        assertEquals(
                List.of(PermissionResponse.builder().id(9L).name("READ").build()).get(0).getName(),
                resp.getPermissions().get(0).getName()
        );
    }

    @Test
    void updateRoleName_success() {
        Role r = Role.builder().id(4L).name("OLD").build();
        when(roleRepository.findById(4L)).thenReturn(Optional.of(r));

        UpdateRoleRequest req = new UpdateRoleRequest();
        req.setName("NEW");

        UpdateRoleResponse resp = roleService.updateRoleName(4L, req);

        assertEquals("Role name updated successfully", resp.getMessage());
        verify(roleRepository).save(any(Role.class));
    }

    @Test
    void updateRoleName_invalid_throws() {
        UpdateRoleRequest req = new UpdateRoleRequest();
        req.setName(" ");
        assertThrows(IllegalArgumentException.class, () -> roleService.updateRoleName(1L, req));
    }

    @Test
    void assignPermissionsToRoles_success() {
        AssignPermissionsToRolesRequest a = new AssignPermissionsToRolesRequest();
        a.setRoleId(1L);
        a.setPermissionIds(List.of(1L, 2L));

        when(roleRepository.findAllById(anyList()))
                .thenReturn(List.of(Role.builder().id(1L).name("R").build()));
        when(permissionService.getExistingPermissionIds(anyList()))
                .thenReturn(List.of(1L, 2L));
        when(rolePermissionService.assignPermissionsToRoles(anyList(), anyList()))
                .thenReturn(2);

        String msg = roleService.assignPermissionsToRoles(List.of(a));
        assertTrue(msg.contains("successfully"));
    }

    @Test
    void assignPermissionsToRoles_skipsWhenNoExistingIds() {
        var req = new AssignPermissionsToRolesRequest();
        req.setRoleId(99L);
        req.setPermissionIds(List.of(1L));

        // Role does not exist -> nothing to assign
        when(roleRepository.findAllById(List.of(99L))).thenReturn(List.of());

        String msg = roleService.assignPermissionsToRoles(List.of(req));

        verify(rolePermissionService, never()).assignPermissionsToRoles(anyList(), anyList());
        assertTrue(msg.contains("0"), "Expected message to indicate 0 assignments but was: " + msg);
    }


    @Test
    void deassignPermissionsFromRoles_success() {
        AssignPermissionsToRolesRequest a = new AssignPermissionsToRolesRequest();
        a.setRoleId(1L);
        a.setPermissionIds(List.of(1L));

        when(rolePermissionService.deassignPermissionsFromRoles(anyList(), anyList()))
                .thenReturn(1);

        String resp = roleService.deassignPermissionsFromRoles(List.of(a));
        assertTrue(resp.contains("removed"));
    }

    @Test
    void assignRolesToGroups_success() {
        AssignRolesToGroupsRequest req = new AssignRolesToGroupsRequest();
        req.setGroupId(1L);
        req.setRoleIds(List.of(1L, 2L));

        when(groupRoleService.getExistingGroupIds(anyList())).thenReturn(List.of(1L));
        when(roleRepository.findAllById(anyList())).thenReturn(List.of(
                Role.builder().id(1L).name("A").build(),
                Role.builder().id(2L).name("B").build()
        ));
        when(groupRoleService.assignRolesToGroups(anyList(), anyList())).thenReturn(2);

        String msg = roleService.assignRolesToGroups(List.of(req));
        assertTrue(msg.startsWith("Roles assigned"));
    }

    @Test
    void deassignRolesFromGroups_success() {
        AssignRolesToGroupsRequest req = new AssignRolesToGroupsRequest();
        req.setGroupId(1L);
        req.setRoleIds(List.of(2L));

        String msg = roleService.deassignRolesFromGroups(List.of(req));
        assertTrue(msg.contains("deassigned"));
        verify(groupRoleService).deassignRolesFromGroups(anyList(), anyList());
    }

    @Test
    void deleteRoles_success() {
        when(roleRepository.findAllById(List.of(5L)))
                .thenReturn(List.of(Role.builder().id(5L).name("R").build()));

        String msg = roleService.deleteRoles(List.of(5L));

        assertTrue(msg.contains("deleted"));
        verify(rolePermissionService).deleteByRoleIds(List.of(5L));
        verify(groupRoleService).deleteByRoleIds(List.of(5L));
        verify(userRoleService).deleteByRoleIds(List.of(5L));
        verify(roleRepository).deleteAllById(List.of(5L));
    }

    @Test
    void deleteRoles_missing_throws() {
        when(roleRepository.findAllById(List.of(6L))).thenReturn(List.of());
        assertThrows(NoSuchElementException.class, () -> roleService.deleteRoles(List.of(6L)));
    }

    @Test
    void getRoleSummariesByIds_success() {
        when(roleRepository.findAllById(List.of(1L)))
                .thenReturn(List.of(Role.builder().id(1L).name("R").build()));

        assertEquals(1, roleService.getRoleSummariesByIds(List.of(1L)).size());
    }
    
    @Test
    void getByIdsOrThrow_success_and_getExistingIds() {
        when(roleRepository.findAllById(List.of(1L, 2L)))
                .thenReturn(List.of(Role.builder().id(1L).name("A").build(), Role.builder().id(2L).name("B").build()));
        assertEquals(2, roleService.getByIdsOrThrow(List.of(1L, 2L)).size());
        assertEquals(List.of(1L, 2L), roleService.getExistingIds(List.of(1L, 2L)));
    }

    @Test
    void assignPermissionsToRoles_invalid_throws() {
        assertThrows(IllegalArgumentException.class, () -> roleService.assignPermissionsToRoles(List.of()));
        assertThrows(IllegalArgumentException.class, () -> roleService.assignPermissionsToRoles(null));
    }

    @Test
    void deassignRolesFromGroups_invalid_throws() {
        assertThrows(IllegalArgumentException.class, () -> roleService.deassignRolesFromGroups(List.of()));
        assertThrows(IllegalArgumentException.class, () -> roleService.deassignRolesFromGroups(null));
    }

    @Test
    void assignRolesToGroups_invalid_throws() {
        assertThrows(IllegalArgumentException.class, () -> roleService.assignRolesToGroups(List.of()));
        assertThrows(IllegalArgumentException.class, () -> roleService.assignRolesToGroups(null));
    }

    @Test
    void deleteRoles_invalid_throws() {
        assertThrows(IllegalArgumentException.class, () -> roleService.deleteRoles(List.of()));
        assertThrows(IllegalArgumentException.class, () -> roleService.deleteRoles(null));
    }
}
