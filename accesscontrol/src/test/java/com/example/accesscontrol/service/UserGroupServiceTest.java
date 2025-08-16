package com.example.accesscontrol.service;

import com.example.accesscontrol.dto.user.assignUsersToGroup.AssignUsersToGroupsRequest;
import com.example.accesscontrol.dto.user.assignUsersToGroup.AssignUsersToGroupsResponse;
import com.example.accesscontrol.dto.user.deassignUsersFromGroups.DeassignUsersFromGroupsRequest;
import com.example.accesscontrol.dto.user.deassignUsersFromGroups.DeassignUsersFromGroupsResponse;
import com.example.accesscontrol.entity.Group;
import com.example.accesscontrol.entity.User;
import com.example.accesscontrol.entity.UserGroup;
import com.example.accesscontrol.exception.ResourceNotFoundException;
import com.example.accesscontrol.repository.GroupRepository;
import com.example.accesscontrol.repository.UserGroupRepository;
import com.example.accesscontrol.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class UserGroupServiceTest {

    @Mock private UserGroupRepository userGroupRepository;
    @Mock private UserRepository userRepository;
    @Mock private GroupRepository groupRepository;

    @InjectMocks
    private UserGroupService userGroupService;

    // ---------- assignUsersToGroups ----------

    @Test
    void assignUsersToGroups_success_insertsOnlyMissingPairs() {
        // req: users [1], groups [10, 11]; existing link (1,10) -> should insert only (1,11)
        AssignUsersToGroupsRequest req = new AssignUsersToGroupsRequest();
        req.setUserIds(List.of(1L));
        req.setGroupIds(List.of(10L, 11L));

        // existing users/groups in DB
        when(userRepository.findAllById(List.of(1L)))
                .thenReturn(List.of(User.builder().id(1L).build()));
        when(groupRepository.findAllById(List.of(10L, 11L)))
                .thenReturn(List.of(Group.builder().id(10L).build(), Group.builder().id(11L).build()));

        // existing links: (1,10) already there
        List<UserGroup> existing = List.of(
                UserGroup.builder()
                        .user(User.builder().id(1L).build())
                        .group(Group.builder().id(10L).build())
                        .build()
        );
        when(userGroupRepository.findByUser_IdInAndGroup_IdIn(anyList(), anyList()))
                .thenReturn(existing);

        AssignUsersToGroupsResponse resp = userGroupService.assignUsersToGroups(req);

        assertEquals(1, resp.getAssignedCount());
        assertEquals("Users assigned to groups successfully", resp.getMessage());

        // verify exactly one new pair was saved with user=1, group=11
        ArgumentCaptor<Iterable<UserGroup>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(userGroupRepository).saveAll(captor.capture());
        List<UserGroup> saved = new ArrayList<>();
        captor.getValue().forEach(saved::add);

        assertEquals(1, saved.size());
        assertEquals(1L, saved.get(0).getUser().getId());
        assertEquals(11L, saved.get(0).getGroup().getId());
    }

    @Test
    void assignUsersToGroups_returnsZero_whenAllPairsAlreadyExist() {
        AssignUsersToGroupsRequest req = new AssignUsersToGroupsRequest();
        req.setUserIds(List.of(1L));
        req.setGroupIds(List.of(10L));

        when(userRepository.findAllById(List.of(1L)))
                .thenReturn(List.of(User.builder().id(1L).build()));
        when(groupRepository.findAllById(List.of(10L)))
                .thenReturn(List.of(Group.builder().id(10L).build()));

        // already exists (1,10)
        when(userGroupRepository.findByUser_IdInAndGroup_IdIn(anyList(), anyList()))
                .thenReturn(List.of(
                        UserGroup.builder()
                                .user(User.builder().id(1L).build())
                                .group(Group.builder().id(10L).build())
                                .build()
                ));

        AssignUsersToGroupsResponse resp = userGroupService.assignUsersToGroups(req);

        assertEquals(0, resp.getAssignedCount());
        assertEquals("Users assigned to groups successfully", resp.getMessage());
        verify(userGroupRepository, never()).saveAll(any());
    }

    @Test
    void assignUsersToGroups_nothingToAssign_forNullOrEmptyInputs() {
        // null request
        AssignUsersToGroupsResponse r1 = userGroupService.assignUsersToGroups(null);
        assertEquals(0, r1.getAssignedCount());
        assertEquals("Nothing to assign", r1.getMessage());

        // empty lists
        AssignUsersToGroupsRequest req = new AssignUsersToGroupsRequest();
        req.setUserIds(List.of());
        req.setGroupIds(List.of(1L));
        AssignUsersToGroupsResponse r2 = userGroupService.assignUsersToGroups(req);
        assertEquals(0, r2.getAssignedCount());
        assertEquals("Nothing to assign", r2.getMessage());

        req.setUserIds(List.of(1L));
        req.setGroupIds(List.of());
        AssignUsersToGroupsResponse r3 = userGroupService.assignUsersToGroups(req);
        assertEquals(0, r3.getAssignedCount());
        assertEquals("Nothing to assign", r3.getMessage());

        verifyNoInteractions(userGroupRepository);
    }

    // import java.util.Arrays;

    @Test
    void assignUsersToGroups_filtersInvalidIds_andMayBecomeNothingToAssign() {
        AssignUsersToGroupsRequest req = new AssignUsersToGroupsRequest();

        req.setUserIds(Arrays.asList(null, -1L, 0L));
        req.setGroupIds(Arrays.asList(-5L, null));

        AssignUsersToGroupsResponse resp = userGroupService.assignUsersToGroups(req);

        assertEquals(0, resp.getAssignedCount());
        assertEquals("Nothing to assign", resp.getMessage());
        verifyNoInteractions(userRepository, groupRepository, userGroupRepository);
    }


    @Test
    void assignUsersToGroups_whenSaveThrowsIntegrityViolation_usesAfterCountDelta() {
        AssignUsersToGroupsRequest req = new AssignUsersToGroupsRequest();
        req.setUserIds(List.of(1L, 2L));
        req.setGroupIds(List.of(10L));

        when(userRepository.findAllById(List.of(1L, 2L)))
                .thenReturn(List.of(User.builder().id(1L).build(), User.builder().id(2L).build()));
        when(groupRepository.findAllById(List.of(10L)))
                .thenReturn(List.of(Group.builder().id(10L).build()));

        // Initially only (1,10) exists
        List<UserGroup> existing = List.of(
                UserGroup.builder()
                        .user(User.builder().id(1L).build())
                        .group(Group.builder().id(10L).build())
                        .build()
        );

        // After save attempt, both (1,10) and (2,10) exist
        List<UserGroup> after = List.of(
                UserGroup.builder()
                        .user(User.builder().id(1L).build())
                        .group(Group.builder().id(10L).build())
                        .build(),
                UserGroup.builder()
                        .user(User.builder().id(2L).build())
                        .group(Group.builder().id(10L).build())
                        .build()
        );

        when(userGroupRepository.findByUser_IdInAndGroup_IdIn(anyList(), anyList()))
                .thenReturn(existing)        // first call (before)
                .thenReturn(after);          // second call (fallback after failure)

        doThrow(new DataIntegrityViolationException("dup")).when(userGroupRepository).saveAll(anyList());

        AssignUsersToGroupsResponse resp = userGroupService.assignUsersToGroups(req);

        // delta = after.size - existing.size = 2 - 1 = 1
        assertEquals(1, resp.getAssignedCount());
        assertEquals("Users assigned to groups successfully", resp.getMessage());
    }

    @Test
    void assignUsersToGroups_whenNoUsersOrNoGroupsExistInDb_returnsNothingToAssign() {
        AssignUsersToGroupsRequest req = new AssignUsersToGroupsRequest();
        req.setUserIds(List.of(100L));
        req.setGroupIds(List.of(200L));

        when(userRepository.findAllById(List.of(100L))).thenReturn(List.of()); // none exist
        when(groupRepository.findAllById(List.of(200L))).thenReturn(List.of());

        AssignUsersToGroupsResponse resp = userGroupService.assignUsersToGroups(req);

        assertEquals(0, resp.getAssignedCount());
        assertEquals("Nothing to assign", resp.getMessage());
        verify(userGroupRepository, never()).saveAll(any());
    }

    // ---------- deassignUsersFromGroups ----------

    @Test
    void deassignUsersFromGroups_success() {
        DeassignUsersFromGroupsRequest req = new DeassignUsersFromGroupsRequest();
        req.setUserIds(List.of(1L, 2L));
        req.setGroupIds(List.of(10L, 11L));

        // all groups exist
        when(groupRepository.findAllById(List.of(10L, 11L)))
                .thenReturn(List.of(Group.builder().id(10L).build(), Group.builder().id(11L).build()));

        when(userGroupRepository.deleteByUser_IdInAndGroup_IdIn(List.of(1L, 2L), List.of(10L, 11L)))
                .thenReturn(3);

        DeassignUsersFromGroupsResponse resp = userGroupService.deassignUsersFromGroups(req);

        assertEquals(3, resp.getRemovedCount());
        assertEquals("Users deassigned from groups successfully", resp.getMessage());
    }

    @Test
    void deassignUsersFromGroups_noMembershipsRemoved_returnsZeroAndMessage() {
        DeassignUsersFromGroupsRequest req = new DeassignUsersFromGroupsRequest();
        req.setUserIds(List.of(1L));
        req.setGroupIds(List.of(10L));

        when(groupRepository.findAllById(List.of(10L)))
                .thenReturn(List.of(Group.builder().id(10L).build()));
        when(userGroupRepository.deleteByUser_IdInAndGroup_IdIn(List.of(1L), List.of(10L)))
                .thenReturn(0);

        DeassignUsersFromGroupsResponse resp = userGroupService.deassignUsersFromGroups(req);

        assertEquals(0, resp.getRemovedCount());
        assertEquals("No memberships were removed", resp.getMessage());
    }

    @Test
    void deassignUsersFromGroups_invalidInput_throwsIllegalArgument() {
        // empty users
        DeassignUsersFromGroupsRequest r1 = new DeassignUsersFromGroupsRequest();
        r1.setUserIds(List.of());
        r1.setGroupIds(List.of(1L));
        assertThrows(IllegalArgumentException.class, () -> userGroupService.deassignUsersFromGroups(r1));

        // empty groups
        DeassignUsersFromGroupsRequest r2 = new DeassignUsersFromGroupsRequest();
        r2.setUserIds(List.of(1L));
        r2.setGroupIds(List.of());
        assertThrows(IllegalArgumentException.class, () -> userGroupService.deassignUsersFromGroups(r2));

        // null lists
        DeassignUsersFromGroupsRequest r3 = new DeassignUsersFromGroupsRequest();
        assertThrows(IllegalArgumentException.class, () -> userGroupService.deassignUsersFromGroups(r3));
    }

    @Test
    void deassignUsersFromGroups_missingGroups_throwsNotFound() {
        DeassignUsersFromGroupsRequest req = new DeassignUsersFromGroupsRequest();
        req.setUserIds(List.of(1L));
        req.setGroupIds(List.of(10L, 11L));

        // Only one group exists -> missing one triggers ResourceNotFoundException
        when(groupRepository.findAllById(List.of(10L, 11L)))
                .thenReturn(List.of(Group.builder().id(10L).build()));

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> userGroupService.deassignUsersFromGroups(req)
        );

        assertTrue(ex.getMessage().contains("Some groups not found"));
    }

    // ---------- helpers ----------

    @Test
    void getGroupIdsByUserId_mapsFromUserGroupEntities() {
        when(userGroupRepository.findByUser_Id(1L)).thenReturn(List.of(
                UserGroup.builder().user(User.builder().id(1L).build()).group(Group.builder().id(10L).build()).build(),
                UserGroup.builder().user(User.builder().id(1L).build()).group(Group.builder().id(11L).build()).build()
        ));

        List<Long> ids = userGroupService.getGroupIdsByUserId(1L);
        assertEquals(Set.of(10L, 11L), Set.copyOf(ids));
    }

    @Test
    void getUserIdsByGroupId_mapsFromUserGroupEntities() {
        when(userGroupRepository.findByGroup_Id(7L)).thenReturn(List.of(
                UserGroup.builder().user(User.builder().id(1L).build()).group(Group.builder().id(7L).build()).build(),
                UserGroup.builder().user(User.builder().id(2L).build()).group(Group.builder().id(7L).build()).build()
        ));

        List<Long> ids = userGroupService.getUserIdsByGroupId(7L);
        assertEquals(Set.of(1L, 2L), Set.copyOf(ids));
    }

    @Test
    void getGroupNamesByUserId_returnsRepositoryResultDirectly() {
        when(userGroupRepository.findGroupNamesByUserId(123L))
                .thenReturn(List.of("Admins", "Editors"));

        assertEquals(List.of("Admins", "Editors"), userGroupService.getGroupNamesByUserId(123L));
    }

    @Test
    void deleteByUserIds_and_deleteByGroupIds_skipOnNullOrEmpty_andInvokeOtherwise() {
        // skip null/empty
        userGroupService.deleteByUserIds(null);
        userGroupService.deleteByUserIds(List.of());
        userGroupService.deleteByGroupIds(null);
        userGroupService.deleteByGroupIds(List.of());

        verify(userGroupRepository, never()).deleteByUser_IdIn(anyList());
        verify(userGroupRepository, never()).deleteByGroup_IdIn(anyList());

        // invoke when non-empty
        userGroupService.deleteByUserIds(List.of(1L, 2L));
        userGroupService.deleteByGroupIds(List.of(10L, 11L));

        verify(userGroupRepository, times(1)).deleteByUser_IdIn(List.of(1L, 2L));
        verify(userGroupRepository, times(1)).deleteByGroup_IdIn(List.of(10L, 11L));
    }
}
