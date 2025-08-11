package com.example.accesscontrol.service;

import com.example.accesscontrol.dto.user.assignUsersToGroup.AssignUsersToGroupsRequest;
import com.example.accesscontrol.dto.user.assignUsersToGroup.AssignUsersToGroupsResponse;
import com.example.accesscontrol.dto.user.deassignUsersFromGroups.DeassignUsersFromGroupsRequest;
import com.example.accesscontrol.dto.user.deassignUsersFromGroups.DeassignUsersFromGroupsResponse;
import com.example.accesscontrol.entity.Group;
import com.example.accesscontrol.entity.UserGroup;
import com.example.accesscontrol.repository.GroupRepository;
import com.example.accesscontrol.repository.UserGroupRepository;
import com.example.accesscontrol.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class UserGroupServiceTest {

    @Mock
    private UserGroupRepository userGroupRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private GroupRepository groupRepository;

    @InjectMocks
    private UserGroupService userGroupService;

    @Test
    void assignUsersToGroups_success_skipsExisting() {
        AssignUsersToGroupsRequest req = new AssignUsersToGroupsRequest();
        req.setUserIds(List.of(1L));
        req.setGroupIds(List.of(10L, 11L));
        when(userGroupRepository.findByUser_IdInAndGroup_IdIn(anyList(), anyList())).thenReturn(List.of(
                UserGroup.builder().user(com.example.accesscontrol.entity.User.builder().id(1L).build()).group(Group.builder().id(10L).build()).build()
        ));
        when(userRepository.existsById(1L)).thenReturn(true);
        when(groupRepository.existsById(10L)).thenReturn(true);
        when(groupRepository.existsById(11L)).thenReturn(true);
        AssignUsersToGroupsResponse resp = userGroupService.assignUsersToGroups(req);
        assertEquals(1, resp.getAssignedCount());
        verify(userGroupRepository).saveAll(argThat(iter -> (iter instanceof java.util.List) && ((java.util.List<?>) iter).size() == 1));
    }

    @Test
    void assignUsersToGroups_returnsZero_whenNothingValid() {
        var req = new AssignUsersToGroupsRequest();
        req.setUserIds(List.of(1L));
        req.setGroupIds(List.of(10L));

        lenient().when(userGroupRepository.findByUser_IdInAndGroup_IdIn(anyList(), anyList()))
                .thenReturn(List.of());
        lenient().when(userRepository.existsById(anyLong())).thenReturn(false);
        lenient().when(groupRepository.existsById(anyLong())).thenReturn(false);

        var resp = userGroupService.assignUsersToGroups(req);

        assertEquals(0, resp.getAssignedCount());
        verify(userGroupRepository, never()).saveAll(any());
    }


    @Test
    void deassignUsersFromGroups_success() {
        DeassignUsersFromGroupsRequest req = new DeassignUsersFromGroupsRequest();
        req.setUserIds(List.of(1L));
        req.setGroupIds(List.of(2L));
        when(userGroupRepository.deleteByUser_IdInAndGroup_IdIn(anyList(), anyList())).thenReturn(3);
        DeassignUsersFromGroupsResponse resp = userGroupService.deassignUsersFromGroups(req);
        assertEquals(3, resp.getRemovedCount());
    }

    @Test
    void helpers() {
        when(userGroupRepository.findByUser_Id(1L)).thenReturn(List.of(UserGroup.builder().group(Group.builder().id(2L).build()).build()));
        when(userGroupRepository.findByGroup_Id(2L)).thenReturn(List.of(UserGroup.builder().user(com.example.accesscontrol.entity.User.builder().id(3L).build()).build()));
        when(groupRepository.findAllById(List.of(2L))).thenReturn(List.of(Group.builder().id(2L).name("G").build()));
        assertEquals(List.of(2L), userGroupService.getGroupIdsByUserId(1L));
        assertEquals(List.of(3L), userGroupService.getUserIdsByGroupId(2L));
        assertEquals(List.of("G"), userGroupService.getGroupNamesByUserId(1L));
        userGroupService.deleteByUserIds(List.of());
        userGroupService.deleteByGroupIds(null);
        verify(userGroupRepository, never()).deleteByUser_IdIn(anyList());
        verify(userGroupRepository, never()).deleteByGroup_IdIn(anyList());
    }
}
