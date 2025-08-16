package com.example.accesscontrol.service;

import com.example.accesscontrol.entity.Permission;
import com.example.accesscontrol.entity.Role;
import com.example.accesscontrol.entity.RolePermission;
import com.example.accesscontrol.repository.RolePermissionRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RolePermissionServiceTest {

    @Mock
    private RolePermissionRepository rolePermissionRepository;

    @InjectMocks
    private RolePermissionService rolePermissionService;

    // ---------- helpers ----------
    private static RolePermission rp(long roleId, long permId) {
        RolePermission rp = new RolePermission();
        rp.setRole(Role.builder().id(roleId).build());
        rp.setPermission(Permission.builder().id(permId).build());
        return rp;
    }

    private static boolean sameIdsIgnoreOrder(List<Long> actual, Long... expected) {
        return new HashSet<>(actual).equals(new HashSet<>(Arrays.asList(expected)));
    }

    // ---------- saveAll ----------

    @Test
    void saveAll_nullOrEmpty_noCall() {
        rolePermissionService.saveAll(null);
        rolePermissionService.saveAll(new ArrayList<>());
        verify(rolePermissionRepository, never()).saveAll(any());
    }

    @Test
    void saveAll_nonEmpty_callsRepository() {
        List<RolePermission> list = List.of(new RolePermission());
        rolePermissionService.saveAll(list);
        verify(rolePermissionRepository).saveAll(list);
    }

    // ---------- assignRolePermissionPairs ----------

    @Test
    void assign_skipsExisting_andInsertsNew_returnsCount() {
        // wanted: (1,10), (1,11), (2,11) ; existing has (1,10)
        Map<Long, Set<Long>> wanted = new HashMap<>();
        wanted.put(1L, new HashSet<>(List.of(10L, 11L)));
        wanted.put(2L, new HashSet<>(List.of(11L)));

        when(rolePermissionRepository.findByRole_IdInAndPermission_IdIn(
                argThat(r -> sameIdsIgnoreOrder(r, 1L, 2L)),
                argThat(p -> sameIdsIgnoreOrder(p, 10L, 11L))
        )).thenReturn(List.of(rp(1, 10)));

        // happy path -> one call to saveAll, no exception
        when(rolePermissionRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        int inserted = rolePermissionService.assignRolePermissionPairs(wanted);

        // new pairs should be (1,11) and (2,11) -> 2
        assertEquals(2, inserted);

        verify(rolePermissionRepository).findByRole_IdInAndPermission_IdIn(
                argThat(r -> sameIdsIgnoreOrder(r, 1L, 2L)),
                argThat(p -> sameIdsIgnoreOrder(p, 10L, 11L))
        );
        verify(rolePermissionRepository).saveAll(argThat(iter -> {
            List<RolePermission> collected = new ArrayList<>();
            iter.forEach(collected::add);
            Set<String> keys = new HashSet<>();
            for (RolePermission x : collected) {
                keys.add(x.getRole().getId() + "_" + x.getPermission().getId());
            }
            return keys.equals(Set.of("1_11", "2_11"));
        }));
    }

    @Test
    void assign_whenIntegrityViolation_usesDeltaBetweenBeforeAndAfter() {
        // wanted: (1,10), (1,11)
        Map<Long, Set<Long>> wanted = Map.of(1L, new HashSet<>(List.of(10L, 11L)));

        // before: nothing
        // after: only (1,10) exists (e.g., DB inserted one row before violation)
        when(rolePermissionRepository.findByRole_IdInAndPermission_IdIn(
                argThat(r -> sameIdsIgnoreOrder(r, 1L)),
                argThat(p -> sameIdsIgnoreOrder(p, 10L, 11L))
        )).thenReturn(List.of()) // first call (before)
                .thenReturn(List.of(rp(1, 10))); // second call (after)

        when(rolePermissionRepository.saveAll(any()))
                .thenThrow(new DataIntegrityViolationException("dup"));

        int result = rolePermissionService.assignRolePermissionPairs(wanted);

        // delta = after.size(1) - before.size(0) = 1
        assertEquals(1, result);

        verify(rolePermissionRepository, times(2)).findByRole_IdInAndPermission_IdIn(
                argThat(r -> sameIdsIgnoreOrder(r, 1L)),
                argThat(p -> sameIdsIgnoreOrder(p, 10L, 11L))
        );
        verify(rolePermissionRepository).saveAll(any());
    }

    // ---------- deleteRolePermissionPairs ----------

    @Test
    void delete_whenMatchesExisting_deletesAndReturnsCount() {
        // wanted: (1,10), (2,11)
        Map<Long, Set<Long>> wanted = new HashMap<>();
        wanted.put(1L, Set.of(10L));
        wanted.put(2L, Set.of(11L));

        // existing includes superset: (1,10), (1,11), (2,11)
        List<RolePermission> existing = List.of(
                rp(1, 10), rp(1, 11), rp(2, 11)
        );

        when(rolePermissionRepository.findByRole_IdInAndPermission_IdIn(
                argThat(r -> sameIdsIgnoreOrder(r, 1L, 2L)),
                argThat(p -> sameIdsIgnoreOrder(p, 10L, 11L))
        )).thenReturn(existing);

        int deleted = rolePermissionService.deleteRolePermissionPairs(wanted);

        assertEquals(2, deleted); // (1,10) and (2,11)

        verify(rolePermissionRepository).findByRole_IdInAndPermission_IdIn(
                argThat(r -> sameIdsIgnoreOrder(r, 1L, 2L)),
                argThat(p -> sameIdsIgnoreOrder(p, 10L, 11L))
        );
        verify(rolePermissionRepository).deleteAllInBatch(argThat(list -> {
            Set<String> keys = new HashSet<>();
            for (RolePermission x : list) {
                keys.add(x.getRole().getId() + "_" + x.getPermission().getId());
            }
            return keys.equals(Set.of("1_10", "2_11"));
        }));
    }

    @Test
    void delete_whenNothingToDelete_returnsZero_noDeleteCall() {
        Map<Long, Set<Long>> wanted = Map.of(3L, Set.of(12L));

        when(rolePermissionRepository.findByRole_IdInAndPermission_IdIn(
                argThat(r -> sameIdsIgnoreOrder(r, 3L)),
                argThat(p -> sameIdsIgnoreOrder(p, 12L))
        )).thenReturn(List.of()); // nothing existing

        int deleted = rolePermissionService.deleteRolePermissionPairs(wanted);
        assertEquals(0, deleted);
        verify(rolePermissionRepository, never()).deleteAllInBatch(anyList());
    }

    // ---------- simple delete helpers ----------

    @Nested
    class DeleteHelpers {
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
}
