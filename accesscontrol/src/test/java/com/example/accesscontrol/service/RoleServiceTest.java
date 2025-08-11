package com.example.accesscontrol.service;

import com.example.accesscontrol.dto.group.AssignRolesToGroupsRequest;
import com.example.accesscontrol.dto.role.*;
import com.example.accesscontrol.entity.Permission;
import com.example.accesscontrol.entity.Role;
import com.example.accesscontrol.entity.RolePermission;
import com.example.accesscontrol.exception.DuplicateResourceException;
import com.example.accesscontrol.exception.ResourceNotFoundException;
import com.example.accesscontrol.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RoleServiceTest {

    @Mock private RoleRepository roleRepository;
    @Mock private PermissionService permissionService;
    @Mock private RolePermissionService rolePermissionService;
    @Mock private GroupRoleService groupRoleService;
    @Mock private UserRoleService userRoleService;

    @InjectMocks private RoleService roleService;

    @BeforeEach
    void defaults() {
        when(roleRepository.findAllById(anyList())).thenReturn(List.of());
        when(permissionService.getExistingPermissionIds(anyList())).thenReturn(List.of());
    }

    // ----------------------- getOrCreateRole -----------------------

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
        verify(roleRepository).save(any(Role.class));
    }

    @Test
    void getOrCreateRole_returnsExisting() {
        Role existing = Role.builder().id(9L).name("ADMIN").build();
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(existing));

        Role r = roleService.getOrCreateRole("ADMIN");
        assertSame(existing, r);
        verify(roleRepository, never()).save(any());
    }

    // ----------------------- getByIdsOrThrow / getExistingIds -----------------------

    @Test
    void getByIdsOrThrow_success_and_getExistingIds() {
        when(roleRepository.findAllById(List.of(1L, 2L)))
                .thenReturn(List.of(
                        Role.builder().id(1L).name("A").build(),
                        Role.builder().id(2L).name("B").build()
                ));

        assertEquals(2, roleService.getByIdsOrThrow(List.of(1L, 2L)).size());
        assertEquals(List.of(1L, 2L), roleService.getExistingIds(List.of(1L, 2L)));
    }

    @Test
    void getByIdsOrThrow_emptyInput_returnsEmpty() {
        assertTrue(roleService.getByIdsOrThrow(List.of()).isEmpty());
        assertTrue(roleService.getExistingIds(List.of()).isEmpty());
    }

    @Test
    void getByIdsOrThrow_throwsWhenAnyMissing() {
        when(roleRepository.findAllById(List.of(1L, 2L)))
                .thenReturn(List.of(Role.builder().id(1L).name("A").build()));
        assertThrows(ResourceNotFoundException.class, () -> roleService.getByIdsOrThrow(List.of(1L, 2L)));
    }

    // ----------------------- createRoles (validations, rp building) -----------------------

    @Test
    void createRoles_invalid_whenNullOrEmptyRequests_throws() {
        assertThrows(IllegalArgumentException.class, () -> roleService.createRoles(null));
        assertThrows(IllegalArgumentException.class, () -> roleService.createRoles(List.of()));
    }

    @Test
    void createRoles_invalid_whenAnyNameBlankOrNull_throws() {
        CreateRoleRequest r1 = new CreateRoleRequest(); r1.setName("OK");
        CreateRoleRequest r2 = new CreateRoleRequest(); r2.setName("  ");
        assertThrows(IllegalArgumentException.class, () -> roleService.createRoles(List.of(r1, r2)));

        CreateRoleRequest r3 = new CreateRoleRequest(); r3.setName(null);
        assertThrows(IllegalArgumentException.class, () -> roleService.createRoles(List.of(r3)));
    }

    @Test
    void createRoles_duplicateNames_throws() {
        CreateRoleRequest r = new CreateRoleRequest(); r.setName("ADMIN");
        when(roleRepository.findExistingNames(anyList())).thenReturn(List.of("ADMIN"));
        assertThrows(DuplicateResourceException.class, () -> roleService.createRoles(List.of(r)));
    }

    @Test
    void createRoles_mixedPermissions_buildsRpOnlyForProvided_onSaveAllCalled() {
        CreateRoleRequest r1 = new CreateRoleRequest();
        r1.setName("R1"); r1.setPermissionIds(List.of(10L, 11L));
        CreateRoleRequest r2 = new CreateRoleRequest();
        r2.setName("R2"); r2.setPermissionIds(null);
        CreateRoleRequest r3 = new CreateRoleRequest();
        r3.setName("R3"); r3.setPermissionIds(List.of());

        when(roleRepository.findExistingNames(anyList())).thenReturn(List.of());
        when(roleRepository.saveAll(anyList())).thenAnswer(inv -> {
            List<Role> roles = inv.getArgument(0);
            long id = 100;
            for (Role ro : roles) ro.setId(id++);
            return roles;
        });

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<RolePermission>> rpCaptor = ArgumentCaptor.forClass(List.class);

        CreateRoleResponse resp = roleService.createRoles(List.of(r1, r2, r3));

        assertEquals("Roles created successfully", resp.getMessage());
        assertEquals(List.of("R1", "R2", "R3"), resp.getCreated());

        verify(rolePermissionService).saveAll(rpCaptor.capture());
        List<RolePermission> rpList = rpCaptor.getValue();
        assertEquals(2, rpList.size());
        Set<Long> permIds = rpList.stream()
                .map(rp -> rp.getPermission().getId())
                .collect(Collectors.toSet());
        assertTrue(permIds.containsAll(List.of(10L, 11L)));
    }

    @Test
    void createRoles_noPermissionsAnywhere_skipsSaveAll() {
        CreateRoleRequest a = new CreateRoleRequest(); a.setName("A");
        CreateRoleRequest b = new CreateRoleRequest(); b.setName("B");

        when(roleRepository.findExistingNames(anyList())).thenReturn(List.of());
        when(roleRepository.saveAll(anyList())).thenAnswer(inv -> {
            List<Role> roles = inv.getArgument(0);
            long id = 1;
            for (Role ro : roles) ro.setId(id++);
            return roles;
        });

        CreateRoleResponse resp = roleService.createRoles(List.of(a, b));
        assertEquals("Roles created successfully", resp.getMessage());
        verify(rolePermissionService, never()).saveAll(anyList());
    }

    // ----------------------- getRoles (search == null; empty page) -----------------------

    @Test
    void getRoles_nullSearch_usesEmptyString() {
        Page<Role> page = new PageImpl<>(
                List.of(Role.builder().id(1L).name("ADMIN").build()),
                PageRequest.of(0, 5), 1
        );
        when(roleRepository.findByNameContainingIgnoreCase(eq(""), any())).thenReturn(page);

        GetRolesResponse resp = roleService.getRoles(null, 0, 5);
        assertEquals(1, resp.getTotal());
        assertEquals("ADMIN", resp.getRoles().get(0).getName());
    }

    @Test
    void getRoles_emptyPage_ok() {
        Page<Role> page = new PageImpl<>(List.of(), PageRequest.of(1, 5), 0);
        when(roleRepository.findByNameContainingIgnoreCase(anyString(), any())).thenReturn(page);

        GetRolesResponse resp = roleService.getRoles("x", 1, 5);
        assertEquals(0, resp.getTotal());
        assertTrue(resp.getRoles().isEmpty());
    }

    @Test
    void getRoles_invalidPagination_throws() {
        assertThrows(IllegalArgumentException.class, () -> roleService.getRoles("q", -1, 10));
        assertThrows(IllegalArgumentException.class, () -> roleService.getRoles("q", 0, 0));
    }

    // ----------------------- getRoleWithPermissions (DTO mapping line) -----------------------

    @Test
    void getRoleWithPermissions_mapsToPermissionResponseBuilderLine() {
        Role r = Role.builder().id(3L).name("ADMIN").build();
        when(roleRepository.findById(3L)).thenReturn(Optional.of(r));
        when(permissionService.getPermissionsByRoleId(3L)).thenReturn(List.of(
                Permission.builder().id(9L).name("READ").build(),
                Permission.builder().id(10L).name("WRITE").build()
        ));

        RoleDetailsResponse resp = roleService.getRoleWithPermissions(3L);

        assertEquals("ADMIN", resp.getName());
        assertEquals(2, resp.getPermissions().size());
        assertEquals(9L, resp.getPermissions().get(0).getId());
        assertEquals("READ", resp.getPermissions().get(0).getName());
    }

    @Test
    void getRoleWithPermissions_notFound_throws() {
        when(roleRepository.findById(404L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> roleService.getRoleWithPermissions(404L));
    }

    // ----------------------- updateRoleName (null/blank guard line) -----------------------

    @Test
    void updateRoleName_invalid_whenNullOrBlank_throws() {
        UpdateRoleRequest r1 = new UpdateRoleRequest(); r1.setName(null);
        assertThrows(IllegalArgumentException.class, () -> roleService.updateRoleName(1L, r1));

        UpdateRoleRequest r2 = new UpdateRoleRequest(); r2.setName("   ");
        assertThrows(IllegalArgumentException.class, () -> roleService.updateRoleName(1L, r2));
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
    void updateRoleName_notFound_throws() {
        when(roleRepository.findById(77L)).thenReturn(Optional.empty());
        UpdateRoleRequest req = new UpdateRoleRequest(); req.setName("X");
        assertThrows(ResourceNotFoundException.class, () -> roleService.updateRoleName(77L, req));
    }

    // ----------------------- assignPermissionsToRoles (filters & flatMap) -----------------------

    @Test
    void assignPermissionsToRoles_filtersInvalidIds_andSkipsNullPermissionLists() {
        // Inputs: one valid role + mixed/invalid permission IDs across requests
        AssignPermissionsToRolesRequest valid = new AssignPermissionsToRolesRequest();
        valid.setRoleId(7L);
        // USE Arrays.asList because it allows null elements
        valid.setPermissionIds(Arrays.asList(1L, null, 0L, -5L, 2L)); // -> keep 1,2

        AssignPermissionsToRolesRequest nullPerms = new AssignPermissionsToRolesRequest();
        nullPerms.setRoleId(0L);             // filtered out
        nullPerms.setPermissionIds(null);    // flatMap(null) path

        AssignPermissionsToRolesRequest negRole = new AssignPermissionsToRolesRequest();
        negRole.setRoleId(-3L);              // filtered out
        negRole.setPermissionIds(List.of(99L));

        AssignPermissionsToRolesRequest nullRole = new AssignPermissionsToRolesRequest();
        nullRole.setRoleId(null);            // filtered out
        nullRole.setPermissionIds(List.of(5L)); // would survive if any role survives

        // Safe stubs
        doReturn(List.of(Role.builder().id(7L).name("R7").build()))
                .when(roleRepository).findAllById(anyList());
        doReturn(List.of(1L, 2L, 5L)) // treat only these as existing
                .when(permissionService).getExistingPermissionIds(anyList());
        doReturn(3)
                .when(rolePermissionService).assignPermissionsToRoles(anyList(), anyList());

        String msg = roleService.assignPermissionsToRoles(List.of(valid, nullPerms, negRole, nullRole));

        verify(rolePermissionService, times(1)).assignPermissionsToRoles(anyList(), anyList());
        assertTrue(msg.contains("Total assignments: 3"));
    }


    @Test
    void assignPermissionsToRoles_allFilteredOut_returnsNoAssignmentsMessage() {
        AssignPermissionsToRolesRequest a = new AssignPermissionsToRolesRequest();
        a.setRoleId(0L);
        // USE Arrays.asList because it allows null elements
        a.setPermissionIds(Arrays.asList(null, 0L, -1L));

        AssignPermissionsToRolesRequest b = new AssignPermissionsToRolesRequest();
        b.setRoleId(null);
        b.setPermissionIds(null); // flatMap(null) path

        // Ensure downstream lookups return nothing (and never null)
        doReturn(List.of()).when(roleRepository).findAllById(anyList());
        doReturn(List.of()).when(permissionService).getExistingPermissionIds(anyList());

        String msg = roleService.assignPermissionsToRoles(List.of(a, b));

        verify(rolePermissionService, never()).assignPermissionsToRoles(anyList(), anyList());
        assertTrue(msg.startsWith("No permissions assigned"));
        assertTrue(msg.contains("(0)"));
    }


    @Test
    void assignPermissionsToRoles_noExistingPermissions_skipsCall() {
        AssignPermissionsToRolesRequest r = new AssignPermissionsToRolesRequest();
        r.setRoleId(1L);
        r.setPermissionIds(List.of(100L));

        when(roleRepository.findAllById(List.of(1L)))
                .thenReturn(List.of(Role.builder().id(1L).name("R1").build()));
        when(permissionService.getExistingPermissionIds(List.of(100L))).thenReturn(List.of());

        String msg = roleService.assignPermissionsToRoles(List.of(r));
        verify(rolePermissionService, never()).assignPermissionsToRoles(anyList(), anyList());
        assertTrue(msg.startsWith("No permissions assigned"));
    }

    @Test
    void assignPermissionsToRoles_invalidInput_throws() {
        assertThrows(IllegalArgumentException.class, () -> roleService.assignPermissionsToRoles(null));
        assertThrows(IllegalArgumentException.class, () -> roleService.assignPermissionsToRoles(List.of()));
    }

    // ----------------------- deassignPermissionsFromRoles -----------------------

    @Test
    void deassignPermissionsFromRoles_removed_gt0() {
        AssignPermissionsToRolesRequest a = new AssignPermissionsToRolesRequest();
        a.setRoleId(1L);
        a.setPermissionIds(List.of(1L));

        when(rolePermissionService.deassignPermissionsFromRoles(anyList(), anyList())).thenReturn(1);
        String resp = roleService.deassignPermissionsFromRoles(List.of(a));
        assertTrue(resp.contains("removed successfully"));
    }

    @Test
    void deassignPermissionsFromRoles_noneRemoved_returnsNoRemovedMessage() {
        AssignPermissionsToRolesRequest a = new AssignPermissionsToRolesRequest();
        a.setRoleId(1L);
        a.setPermissionIds(List.of(1L));

        when(rolePermissionService.deassignPermissionsFromRoles(anyList(), anyList())).thenReturn(0);
        String resp = roleService.deassignPermissionsFromRoles(List.of(a));
        assertTrue(resp.contains("No permissions were removed"));
    }

    @Test
    void deassignPermissionsFromRoles_invalid_throws() {
        assertThrows(IllegalArgumentException.class, () -> roleService.deassignPermissionsFromRoles(null));
        assertThrows(IllegalArgumentException.class, () -> roleService.deassignPermissionsFromRoles(List.of()));
    }

    // ----------------------- assignRolesToGroups / deassignRolesFromGroups -----------------------

    @Test
    void assignRolesToGroups_success_insertsCount() {
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
        assertTrue(msg.contains("Inserted: 2"));
    }

    @Test
    void assignRolesToGroups_rolesMissing_triggersGetByIdsOrThrowException() {
        AssignRolesToGroupsRequest req = new AssignRolesToGroupsRequest();
        req.setGroupId(1L);
        req.setRoleIds(List.of(10L, 20L));

        when(groupRoleService.getExistingGroupIds(anyList())).thenReturn(List.of(1L));
        when(roleRepository.findAllById(anyList()))
                .thenReturn(List.of(Role.builder().id(10L).name("OnlyOne").build()));

        assertThrows(ResourceNotFoundException.class, () -> roleService.assignRolesToGroups(List.of(req)));
    }

    @Test
    void assignRolesToGroups_invalid_throws() {
        assertThrows(IllegalArgumentException.class, () -> roleService.assignRolesToGroups(null));
        assertThrows(IllegalArgumentException.class, () -> roleService.assignRolesToGroups(List.of()));
    }

    @Test
    void deassignRolesFromGroups_success_distinctsApplied() {
        AssignRolesToGroupsRequest r1 = new AssignRolesToGroupsRequest();
        r1.setGroupId(1L);
        r1.setRoleIds(List.of(1L, 2L));

        AssignRolesToGroupsRequest r2 = new AssignRolesToGroupsRequest();
        r2.setGroupId(1L);
        r2.setRoleIds(List.of(2L, 3L));

        String msg = roleService.deassignRolesFromGroups(List.of(r1, r2));
        assertTrue(msg.contains("deassigned"));
        verify(groupRoleService).deassignRolesFromGroups(eq(List.of(1L)), eq(List.of(1L, 2L, 3L)));
    }

    @Test
    void deassignRolesFromGroups_invalid_throws() {
        assertThrows(IllegalArgumentException.class, () -> roleService.deassignRolesFromGroups(null));
        assertThrows(IllegalArgumentException.class, () -> roleService.deassignRolesFromGroups(List.of()));
    }

    // ----------------------- deleteRoles -----------------------

    @Test
    void deleteRoles_success_cascades_thenDeletes() {
        when(roleRepository.findAllById(List.of(5L)))
                .thenReturn(List.of(Role.builder().id(5L).name("R").build()));

        String msg = roleService.deleteRoles(List.of(5L));
        assertTrue(msg.contains("Roles deleted successfully"));

        verify(rolePermissionService).deleteByRoleIds(List.of(5L));
        verify(groupRoleService).deleteByRoleIds(List.of(5L));
        verify(userRoleService).deleteByRoleIds(List.of(5L));
        verify(roleRepository).deleteAllById(List.of(5L));
    }

    @Test
    void deleteRoles_partialMissing_throws() {
        when(roleRepository.findAllById(List.of(1L, 2L)))
                .thenReturn(List.of(Role.builder().id(1L).name("A").build()));
        assertThrows(NoSuchElementException.class, () -> roleService.deleteRoles(List.of(1L, 2L)));
    }

    @Test
    void deleteRoles_invalid_throws() {
        assertThrows(IllegalArgumentException.class, () -> roleService.deleteRoles(null));
        assertThrows(IllegalArgumentException.class, () -> roleService.deleteRoles(List.of()));
    }

    // ----------------------- getRoleSummariesByIds -----------------------

    @Test
    void getRoleSummariesByIds_success_andEmpty() {
        when(roleRepository.findAllById(List.of(1L))).thenReturn(List.of(Role.builder().id(1L).name("R").build()));
        assertEquals(1, roleService.getRoleSummariesByIds(List.of(1L)).size());

        when(roleRepository.findAllById(List.of(99L))).thenReturn(List.of());
        assertTrue(roleService.getRoleSummariesByIds(List.of(99L)).isEmpty());
    }
}