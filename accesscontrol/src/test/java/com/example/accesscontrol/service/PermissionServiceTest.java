package com.example.accesscontrol.service;

import com.example.accesscontrol.dto.common.MessageResponse;
import com.example.accesscontrol.dto.common.PageResponse;
import com.example.accesscontrol.dto.permission.CreatePermissionsRequest;
import com.example.accesscontrol.dto.permission.CreatePermissionsResponse;
import com.example.accesscontrol.dto.permission.PermissionResponse;
import com.example.accesscontrol.dto.permission.UpdatePermissionNameRequest;
import com.example.accesscontrol.dto.permission.UpdatePermissionNameResponse;
import com.example.accesscontrol.entity.Permission;
import com.example.accesscontrol.exception.DuplicateResourceException;
import com.example.accesscontrol.exception.ResourceNotFoundException;
import com.example.accesscontrol.repository.PermissionRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

    @Mock private PermissionRepository permissionRepository;
    @Mock private RolePermissionService rolePermissionService;

    @InjectMocks private PermissionService permissionService;

    // -------- helper factories that adapt to constructor vs setter DTOs --------

    private static UpdatePermissionNameRequest mkUpdateReq(String name) {
        try {
            return UpdatePermissionNameRequest.class
                    .getConstructor(String.class)
                    .newInstance(name);
        } catch (ReflectiveOperationException ignored) {
            try {
                UpdatePermissionNameRequest req =
                        UpdatePermissionNameRequest.class.getDeclaredConstructor().newInstance();
                UpdatePermissionNameRequest.class.getMethod("setName", String.class).invoke(req, name);
                return req;
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("UpdatePermissionNameRequest needs ctor(String) or setName(String)");
            }
        }
    }

    private static CreatePermissionsRequest mkCreateReq(List<String> names) {
        try {
            return CreatePermissionsRequest.class
                    .getConstructor(List.class)
                    .newInstance(names);
        } catch (ReflectiveOperationException ignored) {
            try {
                CreatePermissionsRequest req =
                        CreatePermissionsRequest.class.getDeclaredConstructor().newInstance();
                CreatePermissionsRequest.class.getMethod("setPermissions", List.class).invoke(req, names);
                return req;
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("CreatePermissionsRequest needs ctor(List) or setPermissions(List)");
            }
        }
    }

    // ---------------- createPermissions ----------------

    @Test
    void createPermissions_nullOrEmpty_throwsIllegalArg() {
        assertThrows(IllegalArgumentException.class, () -> permissionService.createPermissions(null));
        assertThrows(IllegalArgumentException.class, () -> permissionService.createPermissions(mkCreateReq(List.of())));
        assertThrows(IllegalArgumentException.class, () -> permissionService.createPermissions(mkCreateReq(List.of(" ", "\t"))));
    }

    @Test
    void createPermissions_duplicateInPayload_throwsDuplicate() {
        var req = mkCreateReq(List.of("READ", "read")); // duplicate ignoring case
        assertThrows(DuplicateResourceException.class, () -> permissionService.createPermissions(req));
        verifyNoInteractions(permissionRepository);
    }

    @Test
    void createPermissions_alreadyExists_throwsDuplicate() {
        var req = mkCreateReq(List.of("A", "B"));
        when(permissionRepository.findByNameInIgnoreCase(List.of("A", "B")))
                .thenReturn(List.of(Permission.builder().id(10L).name("A").build()));

        assertThrows(DuplicateResourceException.class, () -> permissionService.createPermissions(req));

        verify(permissionRepository, times(1)).findByNameInIgnoreCase(List.of("A", "B"));
        verify(permissionRepository, never()).saveAll(any());
    }

    @Test
    void createPermissions_success_sortsAndReturnsItems() {
        var req = mkCreateReq(List.of("WRITE", "READ"));
        when(permissionRepository.findByNameInIgnoreCase(List.of("WRITE", "READ")))
                .thenReturn(List.of());
        var saved = List.of(
                Permission.builder().id(2L).name("WRITE").build(),
                Permission.builder().id(1L).name("READ").build()
        );
        when(permissionRepository.saveAll(any())).thenReturn(saved);

        CreatePermissionsResponse resp = permissionService.createPermissions(req);

        assertEquals("Permissions created successfully", resp.getMessage());
        assertEquals(2, resp.getCreatedCount());
        assertEquals(List.of("READ", "WRITE"), resp.getItems().stream().map(PermissionResponse::getName).toList());

        verify(permissionRepository).findByNameInIgnoreCase(List.of("WRITE", "READ"));
        verify(permissionRepository).saveAll(argThat((Iterable<Permission> it) -> {
            List<String> names = new ArrayList<>();
            for (Permission p : it) names.add(p.getName());
            return names.size() == 2 && names.containsAll(List.of("READ", "WRITE"));
        }));
    }

    @Test
    void createPermissions_integrityViolation_rechecksAndThrowsDuplicate() {
        var req = mkCreateReq(List.of("A", "B"));

        // first pre-check => empty, second (after integrity violation) => A exists
        when(permissionRepository.findByNameInIgnoreCase(List.of("A", "B")))
                .thenReturn(List.of())
                .thenReturn(List.of(Permission.builder().id(1L).name("A").build()));

        when(permissionRepository.saveAll(any()))
                .thenThrow(new DataIntegrityViolationException("dup"));

        assertThrows(DuplicateResourceException.class, () -> permissionService.createPermissions(req));

        verify(permissionRepository, times(2)).findByNameInIgnoreCase(List.of("A", "B"));
        verify(permissionRepository).saveAll(any());
    }

    // ---------------- getPermissions (page) ----------------

    @Test
    void getPermissions_mapsPage_andNormalizesPaging() {
        Permission p1 = Permission.builder().id(3L).name("READ").build();
        Permission p2 = Permission.builder().id(4L).name("WRITE").build();
        Page<Permission> page = new PageImpl<>(
                List.of(p1, p2),
                PageRequest.of(0, 2, Sort.by("id").descending()),
                7
        );

        when(permissionRepository.findByNameContainingIgnoreCase(eq("q"), any(Pageable.class)))
                .thenReturn(page);

        PageResponse<PermissionResponse> resp = permissionService.getPermissions(" q ", -1, 1000);

        assertEquals(0, resp.getPage());
        assertEquals(100, resp.getSize()); // clamped by service
        assertEquals(7, resp.getTotal());
        assertEquals(List.of("READ", "WRITE"), resp.getItems().stream().map(PermissionResponse::getName).toList());

        verify(permissionRepository).findByNameContainingIgnoreCase(eq("q"), any(Pageable.class));
    }

    // ---------------- getPermissionDetails ----------------

    @Test
    void getPermissionDetails_found_returnsDto() {
        when(permissionRepository.findById(5L))
                .thenReturn(java.util.Optional.of(Permission.builder().id(5L).name("X").build()));

        var resp = permissionService.getPermissionDetails(5L);
        assertEquals(5L, resp.getId());
        assertEquals("X", resp.getName());
    }

    @Test
    void getPermissionDetails_notFound_throws() {
        when(permissionRepository.findById(9L)).thenReturn(java.util.Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> permissionService.getPermissionDetails(9L));
    }

    // ---------------- updatePermissionName ----------------

    @Test
    void updatePermissionName_invalidName_throwsIllegalArg() {
        assertThrows(IllegalArgumentException.class, () -> permissionService.updatePermissionName(1L, mkUpdateReq("  ")));
        String longName = "x".repeat(101);
        assertThrows(IllegalArgumentException.class, () -> permissionService.updatePermissionName(1L, mkUpdateReq(longName)));
        assertThrows(IllegalArgumentException.class, () -> permissionService.updatePermissionName(1L, mkUpdateReq(null)));
    }

    @Test
    void updatePermissionName_notFound_throws() {
        when(permissionRepository.findById(1L)).thenReturn(java.util.Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> permissionService.updatePermissionName(1L, mkUpdateReq("NEW")));
    }

    @Test
    void updatePermissionName_noChange_returnsSameNames() {
        Permission p = Permission.builder().id(7L).name("DevOps").build();
        when(permissionRepository.findById(7L)).thenReturn(java.util.Optional.of(p));

        UpdatePermissionNameResponse resp = permissionService.updatePermissionName(7L, mkUpdateReq("devops"));

        assertEquals("Permission updated successfully", resp.getMessage());
        assertEquals(7L, resp.getId());
        assertEquals("DevOps", resp.getOldName());
        assertEquals("DevOps", resp.getNewName());
        verify(permissionRepository, never()).save(any());
    }

    @Test
    void updatePermissionName_duplicatePrecheck_throws() {
        Permission p = Permission.builder().id(5L).name("Old").build();
        when(permissionRepository.findById(5L)).thenReturn(java.util.Optional.of(p));
        when(permissionRepository.findByNameInIgnoreCase(List.of("New")))
                .thenReturn(List.of(Permission.builder().id(6L).name("New").build()));

        assertThrows(DuplicateResourceException.class, () -> permissionService.updatePermissionName(5L, mkUpdateReq("New")));
        verify(permissionRepository, never()).save(any());
    }

    @Test
    void updatePermissionName_saveIntegrityViolation_throwsDuplicate() {
        Permission p = Permission.builder().id(5L).name("Old").build();
        when(permissionRepository.findById(5L)).thenReturn(java.util.Optional.of(p));
        when(permissionRepository.findByNameInIgnoreCase(List.of("New"))).thenReturn(List.of());
        when(permissionRepository.save(any())).thenThrow(new DataIntegrityViolationException("dup"));

        assertThrows(DuplicateResourceException.class, () -> permissionService.updatePermissionName(5L, mkUpdateReq("New")));
        verify(permissionRepository).save(argThat(arg -> Objects.equals(arg.getId(), 5L) && "New".equals(arg.getName())));
    }

    @Test
    void updatePermissionName_success_savesAndReturns() {
        Permission p = Permission.builder().id(5L).name("Old").build();
        when(permissionRepository.findById(5L)).thenReturn(java.util.Optional.of(p));
        when(permissionRepository.findByNameInIgnoreCase(List.of("New"))).thenReturn(List.of());
        when(permissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdatePermissionNameResponse resp = permissionService.updatePermissionName(5L, mkUpdateReq(" New "));

        assertEquals("Permission updated successfully", resp.getMessage());
        assertEquals(5L, resp.getId());
        assertEquals("Old", resp.getOldName());
        assertEquals("New", resp.getNewName());
        verify(permissionRepository).save(argThat(arg -> Objects.equals(arg.getId(), 5L) && "New".equals(arg.getName())));
    }

    // ---------------- deletePermissions ----------------

    @Nested
    class DeletePermissions {

        @Test
        void invalidInputs_throwIllegalArg() {
            assertThrows(IllegalArgumentException.class, () -> permissionService.deletePermissions(null));
            assertThrows(IllegalArgumentException.class, () -> permissionService.deletePermissions(List.of()));

            // Use a null-permitting list (List.of(null, ...) would NPE before method call)
            ArrayList<Long> bad = new ArrayList<>(Arrays.asList(null, -1L, 0L, null));
            assertThrows(IllegalArgumentException.class, () -> permissionService.deletePermissions(bad));
        }

        @Test
        void someMissing_throwNotFound() {
            when(permissionRepository.findAllById(List.of(1L, 2L, 3L)))
                    .thenReturn(List.of(
                            Permission.builder().id(1L).name("A").build(),
                            Permission.builder().id(3L).name("C").build()
                    ));

            assertThrows(ResourceNotFoundException.class, () -> permissionService.deletePermissions(List.of(1L, 2L, 3L)));
        }

        @Test
        void success_deletesRelationsThenEntities_returnsMessage() {
            when(permissionRepository.findAllById(List.of(4L, 5L)))
                    .thenReturn(List.of(
                            Permission.builder().id(4L).name("X").build(),
                            Permission.builder().id(5L).name("Y").build()
                    ));

            MessageResponse resp = permissionService.deletePermissions(List.of(4L, 5L));

            assertEquals("Permissions deleted successfully", resp.getMessage());
            verify(rolePermissionService).deleteByPermissionIds(List.of(4L, 5L));
            verify(permissionRepository).deleteAllByIdInBatch(List.of(4L, 5L));
        }

        @Test
        void integrityViolation_wrapsWithIllegalArgAndMessage() {
            when(permissionRepository.findAllById(List.of(7L)))
                    .thenReturn(List.of(Permission.builder().id(7L).name("Z").build()));
            doThrow(new DataIntegrityViolationException("fk"))
                    .when(permissionRepository).deleteAllByIdInBatch(List.of(7L));

            IllegalArgumentException ex =
                    assertThrows(IllegalArgumentException.class, () -> permissionService.deletePermissions(List.of(7L)));
            assertTrue(ex.getMessage().startsWith("Cannot delete permissions due to existing references:"));
        }
    }

    // ---------------- passthrough helpers ----------------

    @Test
    void getPermissionsByRoleId_returnsFromRepository() {
        List<Permission> expected = List.of(
                Permission.builder().id(1L).name("READ").build(),
                Permission.builder().id(2L).name("WRITE").build()
        );
        when(permissionRepository.findByRoleId(9L)).thenReturn(expected);

        assertEquals(expected, permissionService.getPermissionsByRoleId(9L));
        verify(permissionRepository).findByRoleId(9L);
    }

    @Test
    void getExistingPermissionIds_mapsIds() {
        List<Permission> perms = List.of(
                Permission.builder().id(1L).name("A").build(),
                Permission.builder().id(3L).name("C").build()
        );
        when(permissionRepository.findAllById(List.of(1L, 2L, 3L))).thenReturn(perms);

        assertEquals(List.of(1L, 3L), permissionService.getExistingPermissionIds(List.of(1L, 2L, 3L)));
    }
}
