package com.example.accesscontrol.service;

import com.example.accesscontrol.dto.group.AssignRolesToGroupsRequest;
import com.example.accesscontrol.dto.role.*;
import com.example.accesscontrol.entity.Permission;
import com.example.accesscontrol.entity.Role;
import com.example.accesscontrol.entity.RolePermission;
import com.example.accesscontrol.exception.DuplicateResourceException;
import com.example.accesscontrol.exception.ResourceNotFoundException;
import com.example.accesscontrol.repository.RoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;

import java.util.*;
import java.util.stream.Collectors;

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

    // ---------- getOrCreateRole ----------

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

    // ---------- getByIdsOrThrow & helpers ----------

    @Test
    void getByIdsOrThrow_success_and_getExistingIds() {
        when(roleRepository.findAllById(List.of(1L, 2L)))
                .thenReturn(List.of(
                        Role.builder().id(1L).name("A").build(),
                        Role.builder().id(2L).name("B").build()
                ));

        assertEquals(2, roleService.getByIdsOrThrow(List.of(1L, 2L)).size());

        when(roleRepository.findAllById(List.of(1L, 2L)))
                .thenReturn(List.of(
                        Role.builder().id(1L).name("A").build(),
                        Role.builder().id(2L).name("B").build()
                ));
        assertEquals(List.of(1L, 2L), roleService.getExistingIds(List.of(1L, 2L)));
    }

    @Test
    void getByIdsOrThrow_emptyInput_returnsEmpty() {
        // No stubbing needed: repository not called for empty list
        assertTrue(roleService.getByIdsOrThrow(List.of()).isEmpty());
        assertTrue(roleService.getExistingIds(List.of()).isEmpty());
    }

    @Test
    void getByIdsOrThrow_throwsWhenAnyMissing() {
        when(roleRepository.findAllById(List.of(1L, 2L)))
                .thenReturn(List.of(Role.builder().id(1L).name("A").build()));
        assertThrows(ResourceNotFoundException.class, () -> roleService.getByIdsOrThrow(List.of(1L, 2L)));
    }

    // ---------- createRoles ----------

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
        when(roleRepository.findExistingNames(List.of("ADMIN"))).thenReturn(List.of("ADMIN"));
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

        when(roleRepository.findExistingNames(List.of("R1", "R2", "R3"))).thenReturn(List.of());
        when(permissionService.getExistingPermissionIds(List.of(10L, 11L))).thenReturn(List.of(10L, 11L));

        when(roleRepository.saveAll(anyList())).thenAnswer(inv -> {
            @SuppressWarnings("unchecked")
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

        when(roleRepository.findExistingNames(List.of("A", "B"))).thenReturn(List.of());
        when(roleRepository.saveAll(anyList())).thenAnswer(inv -> {
            @SuppressWarnings("unchecked")
            List<Role> roles = inv.getArgument(0);
            long id = 1;
            for (Role ro : roles) ro.setId(id++);
            return roles;
        });

        CreateRoleResponse resp = roleService.createRoles(List.of(a, b));
        assertEquals("Roles created successfully", resp.getMessage());
        verify(rolePermissionService, never()).saveAll(anyList());
    }

    @Test
    void createRoles_integrityViolation_rechecksAndThrowsDuplicate() {
        CreateRoleRequest a = new CreateRoleRequest(); a.setName("A");
        CreateRoleRequest b = new CreateRoleRequest(); b.setName("B");

        // first check: none exist, second check after integrity violation: "A" exists
        when(roleRepository.findExistingNames(List.of("A", "B")))
                .thenReturn(List.of(), List.of("A"));
        when(roleRepository.saveAll(anyList())).thenThrow(new DataIntegrityViolationException("dup"));

        assertThrows(DuplicateResourceException.class, () -> roleService.createRoles(List.of(a, b)));

        // verify it was called twice with the same argument
        verify(roleRepository, times(2)).findExistingNames(List.of("A", "B"));
    }

    // ---------- getRoles (pagination normalized) ----------

    @Test
    void getRoles_nullSearch_usesEmptyString() {
        Page<Role> page = new PageImpl<>(
                List.of(Role.builder().id(1L).name("ADMIN").build()),
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "id")), 1
        );
        when(roleRepository.findByNameContainingIgnoreCase(eq(""), any(Pageable.class))).thenReturn(page);

        GetRolesResponse resp = roleService.getRoles(null, 0, 5);
        assertEquals(1, resp.getTotal());
        assertEquals("ADMIN", resp.getRoles().get(0).getName());
    }

    @Test
    void getRoles_emptyPage_ok() {
        Page<Role> page = new PageImpl<>(List.of(), PageRequest.of(1, 5), 0);
        when(roleRepository.findByNameContainingIgnoreCase(anyString(), any(Pageable.class))).thenReturn(page);

        GetRolesResponse resp = roleService.getRoles("x", 1, 5);
        assertEquals(0, resp.getTotal());
        assertTrue(resp.getRoles().isEmpty());
    }

    @Test
    void getRoles_normalizesInvalidPagination() {
        when(roleRepository.findByNameContainingIgnoreCase(anyString(), any(Pageable.class)))
                .thenAnswer(inv -> {
                    Pageable p = inv.getArgument(1);
                    assertEquals(0, p.getPageNumber()); // normalized
                    assertEquals(1, p.getPageSize());   // normalized
                    return new PageImpl<Role>(List.of(), p, 0);
                });

        GetRolesResponse resp = roleService.getRoles("q", -5, 0);
        assertEquals(0, resp.getPage());
    }

    // ---------- getRoleWithPermissions ----------

    @Test
    void getRoleWithPermissions_mapsToPermissionResponse() {
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

    // ---------- updateRoleName ----------

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
        when(roleRepository.findByName("NEW")).thenReturn(Optional.empty());

        UpdateRoleRequest req = new UpdateRoleRequest();
        req.setName("NEW");

        UpdateRoleResponse resp = roleService.updateRoleName(4L, req);
        assertEquals("Role name updated successfully", resp.getMessage());
        verify(roleRepository).save(any(Role.class));
    }

    @Test
    void updateRoleName_duplicate_throws() {
        Role r = Role.builder().id(4L).name("OLD").build();
        when(roleRepository.findById(4L)).thenReturn(Optional.of(r));
        when(roleRepository.findByName("NEW")).thenReturn(Optional.of(Role.builder().id(99L).name("NEW").build()));

        UpdateRoleRequest req = new UpdateRoleRequest();
        req.setName("NEW");

        assertThrows(DuplicateResourceException.class, () -> roleService.updateRoleName(4L, req));
    }

    // ---------- assign/deassign permissions ----------

    @Test
    void assignPermissionsToRoles_invalidInput_throws() {
        assertThrows(IllegalArgumentException.class, () -> roleService.assignPermissionsToRoles(null));
        assertThrows(IllegalArgumentException.class, () -> roleService.assignPermissionsToRoles(List.of()));
    }

    @Test
    void assignPermissionsToRoles_success_callsServicesAndReturnsMessage() {
        var item = new AssignPermissionsToRolesRequest();
        item.setRoleId(1L);
        item.setPermissionIds(List.of(10L, 11L));

        when(roleRepository.findAllById(List.of(1L)))
                .thenReturn(List.of(Role.builder().id(1L).name("R").build()));
        when(permissionService.getExistingPermissionIds(List.of(10L, 11L)))
                .thenReturn(List.of(10L, 11L));
        when(rolePermissionService.assignRolePermissionPairs(anyMap())).thenReturn(2);

        String msg = roleService.assignPermissionsToRoles(List.of(item));
        assertTrue(msg.contains("Permissions assigned successfully"));
        verify(rolePermissionService).assignRolePermissionPairs(anyMap());
    }

    @Test
    void deassignPermissionsFromRoles_invalid_throws() {
        assertThrows(IllegalArgumentException.class, () -> roleService.deassignPermissionsFromRoles(null));
        assertThrows(IllegalArgumentException.class, () -> roleService.deassignPermissionsFromRoles(List.of()));
    }

    @Test
    void deassignPermissionsFromRoles_success_returnsMessage() {
        var item = new AssignPermissionsToRolesRequest();
        item.setRoleId(1L);
        item.setPermissionIds(List.of(10L));

        when(roleRepository.findAllById(List.of(1L)))
                .thenReturn(List.of(Role.builder().id(1L).name("R").build()));
        when(permissionService.getExistingPermissionIds(List.of(10L)))
                .thenReturn(List.of(10L));
        when(rolePermissionService.deleteRolePermissionPairs(anyMap())).thenReturn(1);

        String msg = roleService.deassignPermissionsFromRoles(List.of(item));
        assertTrue(msg.contains("Permissions removed successfully"));
        verify(rolePermissionService).deleteRolePermissionPairs(anyMap());
    }

    // ---------- assign/deassign roles to groups ----------

    @Test
    void assignRolesToGroups_rolesMissing_triggersGetByIdsOrThrowException() {
        AssignRolesToGroupsRequest req = new AssignRolesToGroupsRequest();
        req.setGroupId(1L);
        req.setRoleIds(List.of(10L, 20L));

        // group exists
        when(groupRoleService.getExistingGroupIds(List.of(1L))).thenReturn(List.of(1L));

        // IMPORTANT: don't fix the order; accept any list
        when(roleRepository.findAllById(anyList()))
                .thenReturn(List.of(Role.builder().id(10L).name("OnlyOne").build())); // intentionally missing 20L

        assertThrows(ResourceNotFoundException.class, () -> roleService.assignRolesToGroups(List.of(req)));
    }


    @Test
    void assignRolesToGroups_success_returnsMessage() {
        AssignRolesToGroupsRequest req = new AssignRolesToGroupsRequest();
        req.setGroupId(5L);
        req.setRoleIds(List.of(1L, 2L));

        when(groupRoleService.getExistingGroupIds(List.of(5L))).thenReturn(List.of(5L));
        when(roleRepository.findAllById(List.of(1L, 2L)))
                .thenReturn(List.of(
                        Role.builder().id(1L).name("A").build(),
                        Role.builder().id(2L).name("B").build()
                ));
        when(groupRoleService.assignGroupRolePairs(anyMap())).thenReturn(2);

        String msg = roleService.assignRolesToGroups(List.of(req));
        assertTrue(msg.contains("Roles assigned to groups successfully"));
        verify(groupRoleService).assignGroupRolePairs(anyMap());
    }

    @Test
    void deassignRolesFromGroups_invalid_throws() {
        assertThrows(IllegalArgumentException.class, () -> roleService.deassignRolesFromGroups(null));
        assertThrows(IllegalArgumentException.class, () -> roleService.deassignRolesFromGroups(List.of()));
    }

    @Test
    void deassignRolesFromGroups_success_returnsMessage() {
        AssignRolesToGroupsRequest req = new AssignRolesToGroupsRequest();
        req.setGroupId(9L);
        req.setRoleIds(List.of(7L));

        when(groupRoleService.getExistingGroupIds(List.of(9L))).thenReturn(List.of(9L));
        when(roleRepository.findAllById(List.of(7L)))
                .thenReturn(List.of(Role.builder().id(7L).name("X").build()));
        when(groupRoleService.deleteGroupRolePairs(anyMap())).thenReturn(1);

        String msg = roleService.deassignRolesFromGroups(List.of(req));
        assertTrue(msg.contains("Roles deassigned from groups successfully"));
        verify(groupRoleService).deleteGroupRolePairs(anyMap());
    }

    // ---------- deleteRoles ----------

    @Test
    void deleteRoles_success_cascades_thenDeletes() {
        when(roleRepository.findAllById(List.of(5L)))
                .thenReturn(List.of(Role.builder().id(5L).name("R").build()));

        String msg = roleService.deleteRoles(List.of(5L));
        assertTrue(msg.contains("Roles deleted successfully"));

        verify(rolePermissionService).deleteByRoleIds(List.of(5L));
        verify(groupRoleService).deleteByRoleIds(List.of(5L));
        verify(userRoleService).deleteByRoleIds(List.of(5L));
        verify(roleRepository).deleteAllByIdInBatch(List.of(5L));
    }

    @Test
    void deleteRoles_partialMissing_throwsResourceNotFound() {
        when(roleRepository.findAllById(List.of(1L, 2L)))
                .thenReturn(List.of(Role.builder().id(1L).name("A").build()));
        assertThrows(ResourceNotFoundException.class, () -> roleService.deleteRoles(List.of(1L, 2L)));
    }

    @Test
    void deleteRoles_invalid_throws() {
        assertThrows(IllegalArgumentException.class, () -> roleService.deleteRoles(null));
        assertThrows(IllegalArgumentException.class, () -> roleService.deleteRoles(List.of()));
    }

    // ---------- summaries ----------

    @Test
    void getRoleSummariesByIds_success_andEmpty() {
        when(roleRepository.findAllById(List.of(1L))).thenReturn(List.of(Role.builder().id(1L).name("R").build()));
        List<RoleResponse> ok = roleService.getRoleSummariesByIds(List.of(1L));
        assertEquals(1, ok.size());
        assertEquals("R", ok.get(0).getName());

        when(roleRepository.findAllById(List.of(99L))).thenReturn(List.of());
        assertTrue(roleService.getRoleSummariesByIds(List.of(99L)).isEmpty());
    }
}
