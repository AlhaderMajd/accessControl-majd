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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRoleServiceTest {

    @Mock private UserRoleRepository userRoleRepository;
    @InjectMocks private UserRoleService userRoleService;

    @Captor private ArgumentCaptor<Iterable<UserRole>> userRoleIterableCaptor;

    @Test
    void assignRolesToUsers_insertsOnlyMissing() {
        // Service will call with userIds=[1], roleIds=[10,11]
        when(userRoleRepository.findByUser_IdInAndRole_IdIn(eq(List.of(1L)), eq(List.of(10L, 11L))))
                .thenReturn(List.of(UserRole.builder()
                        .user(User.builder().id(1L).build())
                        .role(Role.builder().id(10L).build())
                        .build()));

        int inserted = userRoleService.assignRolesToUsers(List.of(1L), List.of(10L, 11L));

        assertEquals(1, inserted);
        verify(userRoleRepository).saveAll(userRoleIterableCaptor.capture());

        int size = 0;
        for (UserRole ignored : userRoleIterableCaptor.getValue()) size++;
        assertEquals(1, size); // only (1,11) should be inserted
    }

    @Test
    void deassignRoles_buildsResponse() {
        when(userRoleRepository.deleteByUser_IdInAndRole_IdIn(anyList(), anyList()))
                .thenReturn(2);

        DeassignRolesResponse resp = userRoleService.deassignRoles(
                List.of(User.builder().id(1L).build()),
                List.of(Role.builder().id(2L).build())
        );

        assertEquals(2, resp.getRemovedCount());
        assertTrue(resp.getMessage().contains("deassigned"));
    }

    @Test
    void deleteByHelpers_ignoreNullOrEmpty() {
        userRoleService.deleteByUserIds(List.of());
        userRoleService.deleteByRoleIds(null);

        verify(userRoleRepository, never()).deleteByUser_IdIn(anyList());
        verify(userRoleRepository, never()).deleteAllByRole_IdIn(anyList());
    }
}
