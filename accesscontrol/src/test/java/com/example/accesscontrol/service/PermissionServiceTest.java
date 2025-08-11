package com.example.accesscontrol.service;

import com.example.accesscontrol.dto.common.MessageResponse;
import com.example.accesscontrol.dto.common.PageResponse;
import com.example.accesscontrol.dto.permission.*;
import com.example.accesscontrol.entity.Permission;
import com.example.accesscontrol.exception.DuplicateResourceException;
import com.example.accesscontrol.exception.ResourceNotFoundException;
import com.example.accesscontrol.repository.PermissionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

    @Mock private PermissionRepository permissionRepository;
    @Mock private RolePermissionService rolePermissionService;

    @InjectMocks private PermissionService permissionService;

    @Test
    void createPermissions_success() {
        CreatePermissionsRequest req = new CreatePermissionsRequest();
        req.setPermissions(List.of("READ", "WRITE", "READ", "  "));

        when(permissionRepository.findByNameInIgnoreCase(anyList())).thenReturn(List.of());
        when(permissionRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        CreatePermissionsResponse resp = permissionService.createPermissions(req);

        assertEquals("Permissions created successfully", resp.getMessage());
        assertEquals(2, resp.getCreatedCount());
        assertEquals(List.of("READ", "WRITE"),
                resp.getItems().stream().map(PermissionResponse::getName).toList());
    }

    @Test
    void createPermissions_empty_throws() {
        // case 1: empty list
        CreatePermissionsRequest emptyReq = new CreatePermissionsRequest();
        emptyReq.setPermissions(List.of());
        assertThrows(DuplicateResourceException.class, () -> permissionService.createPermissions(emptyReq));

        // case 2: blanks and nulls (Arrays.asList allows nulls; List.of does not)
        CreatePermissionsRequest blankReq = new CreatePermissionsRequest();
        blankReq.setPermissions(Arrays.asList("  ", null));
        assertThrows(DuplicateResourceException.class, () -> permissionService.createPermissions(blankReq));
    }

    @Test
    void createPermissions_duplicates_throws() {
        CreatePermissionsRequest req = new CreatePermissionsRequest();
        req.setPermissions(List.of("READ"));

        when(permissionRepository.findByNameInIgnoreCase(anyList()))
                .thenReturn(List.of(Permission.builder().id(1L).name("READ").build()));

        assertThrows(DuplicateResourceException.class, () -> permissionService.createPermissions(req));
    }

    @Test
    void getPermissions_success() {
        List<Permission> perms = List.of(Permission.builder().id(1L).name("READ").build());
        Page<Permission> page = new PageImpl<>(perms, PageRequest.of(0, 10), 1);

        when(permissionRepository.findByNameContainingIgnoreCase(anyString(), any(Pageable.class)))
                .thenReturn(page);

        PageResponse<PermissionResponse> resp = permissionService.getPermissions("re", 0, 10);

        assertEquals(1, resp.getTotal());
        assertEquals("READ", resp.getItems().get(0).getName());
    }

    @Test
    void getPermissions_invalidParams_throws() {
        assertThrows(IllegalArgumentException.class, () -> permissionService.getPermissions("x", -1, 10));
        assertThrows(IllegalArgumentException.class, () -> permissionService.getPermissions("x", 0, 0));
    }

    @Test
    void getPermissionDetails_success() {
        when(permissionRepository.findById(5L))
                .thenReturn(Optional.of(Permission.builder().id(5L).name("ADMIN").build()));

        PermissionResponse resp = permissionService.getPermissionDetails(5L);

        assertEquals(5L, resp.getId());
        assertEquals("ADMIN", resp.getName());
    }

    @Test
    void getPermissionDetails_notFound_throws() {
        when(permissionRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> permissionService.getPermissionDetails(99L));
    }

    @Test
    void updatePermissionName_success() {
        Permission p = Permission.builder().id(7L).name("OLD").build();
        when(permissionRepository.findById(7L)).thenReturn(Optional.of(p));

        UpdatePermissionNameRequest req = new UpdatePermissionNameRequest();
        req.setName("NEW");

        UpdatePermissionNameResponse resp = permissionService.updatePermissionName(7L, req);

        assertEquals("Permission updated successfully", resp.getMessage());
        assertEquals("OLD", resp.getOldName());
        assertEquals("NEW", resp.getNewName());
        verify(permissionRepository).save(any(Permission.class));
    }

    @Test
    void updatePermissionName_invalid_throws() {
        UpdatePermissionNameRequest bad = new UpdatePermissionNameRequest();
        bad.setName("   ");
        assertThrows(IllegalArgumentException.class, () -> permissionService.updatePermissionName(1L, bad));
    }

    @Test
    void updatePermissionName_notFound_throws() {
        when(permissionRepository.findById(1L)).thenReturn(Optional.empty());
        UpdatePermissionNameRequest ok = new UpdatePermissionNameRequest();
        ok.setName("X");
        assertThrows(ResourceNotFoundException.class, () -> permissionService.updatePermissionName(1L, ok));
    }

    @Test
    void deletePermissions_success() {
        when(permissionRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(
                Permission.builder().id(1L).name("A").build(),
                Permission.builder().id(2L).name("B").build()
        ));

        MessageResponse resp = permissionService.deletePermissions(List.of(1L, 2L));

        assertEquals("Permissions deleted successfully", resp.getMessage());
        verify(rolePermissionService).deleteByPermissionIds(List.of(1L, 2L));
        verify(permissionRepository).deleteAllById(List.of(1L, 2L));
    }

    @Test
    void deletePermissions_invalid_throws() {
        assertThrows(IllegalArgumentException.class, () -> permissionService.deletePermissions(List.of()));
    }

    @Test
    void deletePermissions_notFound_throws() {
        when(permissionRepository.findAllById(anyList())).thenReturn(List.of());
        assertThrows(ResourceNotFoundException.class, () -> permissionService.deletePermissions(List.of(9L)));
    }

    @Test
    void deletePermissions_noExisting_throws() {
        when(permissionRepository.findAllById(List.of(7L))).thenReturn(List.of());
        assertThrows(ResourceNotFoundException.class, () -> permissionService.deletePermissions(List.of(7L)));
    }

    @Test
    void queryHelpers() {
        when(permissionRepository.findByRoleId(3L)).thenReturn(List.of());
        when(permissionRepository.findAllById(List.of(1L)))
                .thenReturn(List.of(Permission.builder().id(1L).name("X").build()));

        assertEquals(0, permissionService.getPermissionsByRoleId(3L).size());
        assertEquals(List.of(1L), permissionService.getExistingPermissionIds(List.of(1L)));
    }
}
