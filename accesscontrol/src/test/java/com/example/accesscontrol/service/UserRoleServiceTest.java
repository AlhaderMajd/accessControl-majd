package com.example.accesscontrol.service;

import com.example.accesscontrol.dto.user.deassignUsersFromUsers.DeassignRolesResponse;
import com.example.accesscontrol.entity.Role;
import com.example.accesscontrol.entity.User;
import com.example.accesscontrol.entity.UserRole;
import com.example.accesscontrol.repository.UserRoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class UserRoleServiceTest {

    @Mock private UserRoleRepository userRoleRepository;
    @InjectMocks private UserRoleService userRoleService;

    @Captor private ArgumentCaptor<Iterable<UserRole>> iterableCaptor;

    // ---------- assignRolesToUsers ----------

    @Test
    void assignRolesToUsers_insertsOnlyMissingPairs() {
        // users: [1], roles: [10,11]; (1,10) exists -> should insert only (1,11)
        when(userRoleRepository.findByUser_IdInAndRole_IdIn(List.of(1L), List.of(10L, 11L)))
                .thenReturn(List.of(
                        UserRole.builder()
                                .user(User.builder().id(1L).build())
                                .role(Role.builder().id(10L).build())
                                .build()
                ));

        int inserted = userRoleService.assignRolesToUsers(List.of(1L), List.of(10L, 11L));

        assertEquals(1, inserted);
        verify(userRoleRepository).saveAll(iterableCaptor.capture());

        var saved = new ArrayList<UserRole>();
        iterableCaptor.getValue().forEach(saved::add);

        assertEquals(1, saved.size());
        assertEquals(1L, saved.get(0).getUser().getId());
        assertEquals(11L, saved.get(0).getRole().getId());
    }

    @Test
    void assignRolesToUsers_returnsZero_whenAllPairsAlreadyExist() {
        when(userRoleRepository.findByUser_IdInAndRole_IdIn(List.of(1L), List.of(10L)))
                .thenReturn(List.of(
                        UserRole.builder()
                                .user(User.builder().id(1L).build())
                                .role(Role.builder().id(10L).build())
                                .build()
                ));

        int inserted = userRoleService.assignRolesToUsers(List.of(1L), List.of(10L));

        assertEquals(0, inserted);
        verify(userRoleRepository, never()).saveAll(any());
    }

    @Test
    void assignRolesToUsers_returnsZero_whenInputsEmptyOrNull() {
        assertEquals(0, userRoleService.assignRolesToUsers(null, List.of(1L)));
        assertEquals(0, userRoleService.assignRolesToUsers(List.of(), List.of(1L)));
        assertEquals(0, userRoleService.assignRolesToUsers(List.of(1L), null));
        assertEquals(0, userRoleService.assignRolesToUsers(List.of(1L), List.of()));

        verifyNoInteractions(userRoleRepository);
    }

    @Test
    void assignRolesToUsers_whenSaveAllThrowsIntegrityViolation_returnsDeltaBasedOnRecount() {
        var users = List.of(1L, 2L);
        var roles = List.of(10L);

        // Before: only (1,10) exists
        var existingBefore = List.of(
                UserRole.builder().user(User.builder().id(1L).build()).role(Role.builder().id(10L).build()).build()
        );
        // After failed save: (1,10) and (2,10) exist
        var existingAfter = List.of(
                UserRole.builder().user(User.builder().id(1L).build()).role(Role.builder().id(10L).build()).build(),
                UserRole.builder().user(User.builder().id(2L).build()).role(Role.builder().id(10L).build()).build()
        );

        when(userRoleRepository.findByUser_IdInAndRole_IdIn(users, roles))
                .thenReturn(existingBefore)   // first call (before)
                .thenReturn(existingAfter);   // second call (after)

        doThrow(new DataIntegrityViolationException("duplicate"))
                .when(userRoleRepository).saveAll(anyList());

        int inserted = userRoleService.assignRolesToUsers(users, roles);

        // delta = 2 - 1 = 1
        assertEquals(1, inserted);
    }

    // ---------- deassignRoles ----------

    @Test
    void deassignRoles_success_buildsResponse() {
        when(userRoleRepository.deleteByUser_IdInAndRole_IdIn(List.of(1L), List.of(2L)))
                .thenReturn(3);

        DeassignRolesResponse resp = userRoleService.deassignRoles(
                List.of(User.builder().id(1L).build()),
                List.of(Role.builder().id(2L).build())
        );

        assertEquals(3, resp.getRemovedCount());
        assertEquals("Roles deassigned successfully", resp.getMessage());
    }

    @Test
    void deassignRoles_zeroRemoved_returnsProperMessage() {
        when(userRoleRepository.deleteByUser_IdInAndRole_IdIn(List.of(1L), List.of(2L)))
                .thenReturn(0);

        DeassignRolesResponse resp = userRoleService.deassignRoles(
                List.of(User.builder().id(1L).build()),
                List.of(Role.builder().id(2L).build())
        );

        assertEquals(0, resp.getRemovedCount());
        assertEquals("No roles were deassigned", resp.getMessage());
    }

    // ---------- helpers ----------

    @Test
    void getRoleNamesByUserId_returnsRepositoryValue() {
        when(userRoleRepository.findRoleNamesByUserId(99L))
                .thenReturn(List.of("ADMIN", "AUTHOR"));

        assertEquals(List.of("ADMIN", "AUTHOR"), userRoleService.getRoleNamesByUserId(99L));
    }

    @Test
    void deleteByUserIds_and_deleteByRoleIds_guardAgainstNullOrEmpty_andCallWhenNonEmpty() {
        // guard (no calls)
        userRoleService.deleteByUserIds(null);
        userRoleService.deleteByUserIds(List.of());
        userRoleService.deleteByRoleIds(null);
        userRoleService.deleteByRoleIds(List.of());

        verify(userRoleRepository, never()).deleteByUser_IdIn(anyList());
        verify(userRoleRepository, never()).deleteAllByRole_IdIn(anyList());

        // calls when non-empty
        userRoleService.deleteByUserIds(List.of(1L, 2L));
        userRoleService.deleteByRoleIds(List.of(10L, 11L));

        verify(userRoleRepository, times(1)).deleteByUser_IdIn(List.of(1L, 2L));
        verify(userRoleRepository, times(1)).deleteAllByRole_IdIn(List.of(10L, 11L));
    }
}
