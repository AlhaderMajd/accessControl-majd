package com.example.accesscontrol.service;

import com.example.accesscontrol.dto.common.PageResponse;
import com.example.accesscontrol.dto.group.*;
import com.example.accesscontrol.dto.role.RoleResponse;
import com.example.accesscontrol.dto.user.getUsers.UserSummaryResponse;
import com.example.accesscontrol.entity.Group;
import com.example.accesscontrol.exception.ResourceNotFoundException;
import com.example.accesscontrol.repository.GroupRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @Mock private GroupRepository groupRepository;
    @Mock private UserGroupService userGroupService;
    @Mock private GroupRoleService groupRoleService;
    @Mock private UserService userService;
    @Mock private RoleService roleService;

    @InjectMocks private GroupService groupService;

    @Test
    void createGroups_success() {
        CreateGroupRequest a = new CreateGroupRequest(); a.setName("A");
        CreateGroupRequest b = new CreateGroupRequest(); b.setName("B");
        when(groupRepository.findByNameInIgnoreCase(anyList())).thenReturn(List.of());
        when(groupRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        CreateGroupsResponse resp = groupService.createGroups(List.of(a, b));
        assertEquals(2, resp.getCreatedCount());
        assertEquals(List.of("A","B"), resp.getItems().stream().map(GroupResponse::getName).toList());
    }

    @Test
    void createGroups_invalid_throws() {
        assertThrows(IllegalArgumentException.class, () -> groupService.createGroups(List.of()));
        CreateGroupRequest bad = new CreateGroupRequest(); bad.setName("  ");
        assertThrows(IllegalArgumentException.class, () -> groupService.createGroups(List.of(bad)));
    }

    @Test
    void createGroups_duplicates_throws() {
        CreateGroupRequest a = new CreateGroupRequest(); a.setName("A");
        when(groupRepository.findByNameInIgnoreCase(anyList())).thenReturn(List.of(Group.builder().name("A").build()));
        assertThrows(IllegalStateException.class, () -> groupService.createGroups(List.of(a)));
    }

    @Test
    void getGroups_success() {
        Page<Group> pg = new PageImpl<>(List.of(Group.builder().id(1L).name("G").build()), PageRequest.of(0,10), 1);
        when(groupRepository.findByNameContainingIgnoreCase(anyString(), any())).thenReturn(pg);
        PageResponse<GroupResponse> resp = groupService.getGroups("g", 0, 10);
        assertEquals(1, resp.getTotal());
        assertEquals("G", resp.getItems().get(0).getName());
    }

    @Test
    void getGroups_invalid_throws() {
        assertThrows(IllegalArgumentException.class, () -> groupService.getGroups("x", -1, 10));
        assertThrows(IllegalArgumentException.class, () -> groupService.getGroups("x", 0, 0));
    }

    @Test
    void getGroupDetails_success() {
        when(groupRepository.findById(1L)).thenReturn(java.util.Optional.of(Group.builder().id(1L).name("G1").build()));
        when(userGroupService.getUserIdsByGroupId(1L)).thenReturn(List.of(2L));
        when(groupRoleService.getRoleIdsByGroupId(1L)).thenReturn(List.of(3L));
        when(userService.getUserSummariesByIds(List.of(2L))).thenReturn(List.of(UserSummaryResponse.builder().id(2L).email("u").enabled(true).build()));
        when(roleService.getRoleSummariesByIds(List.of(3L))).thenReturn(List.of(RoleResponse.builder().id(3L).name("R").build()));
        GroupDetailsResponse resp = groupService.getGroupDetails(1L);
        assertEquals("G1", resp.getName());
        assertEquals(1, resp.getUsers().size());
        assertEquals(1, resp.getRoles().size());
    }

    @Test
    void getGroupDetails_notFound_throws() {
        when(groupRepository.findById(1L)).thenReturn(java.util.Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> groupService.getGroupDetails(1L));
    }

    @Test
    void updateGroupName_success() {
        Group g = Group.builder().id(1L).name("OLD").build();
        when(groupRepository.findById(1L)).thenReturn(java.util.Optional.of(g));
        UpdateGroupNameRequest req = new UpdateGroupNameRequest();
        req.setName("NEW");
        UpdateGroupNameResponse resp = groupService.updateGroupName(1L, req);
        assertEquals("Group name updated successfully", resp.getMessage());
        verify(groupRepository).save(any(Group.class));
    }

    @Test
    void updateGroupName_invalid_throws() {
        UpdateGroupNameRequest req = new UpdateGroupNameRequest();
        req.setName(" ");
        assertThrows(IllegalArgumentException.class, () -> groupService.updateGroupName(1L, req));
    }

    @Test
    void deleteGroups_success() {
        when(groupRepository.findAllById(List.of(1L))).thenReturn(List.of(Group.builder().id(1L).name("G").build()));
        var resp = groupService.deleteGroups(List.of(1L));
        assertEquals("Group(s) deleted successfully", resp.getMessage());
        verify(userGroupService).deleteByGroupIds(List.of(1L));
        verify(groupRoleService).deleteByGroupIds(List.of(1L));
        verify(groupRepository).deleteAllById(List.of(1L));
    }

    @Test
    void deleteGroups_invalidOrMissing_throws() {
        assertThrows(IllegalArgumentException.class, () -> groupService.deleteGroups(List.of()));
        when(groupRepository.findAllById(List.of(9L))).thenReturn(List.of());
        assertThrows(ResourceNotFoundException.class, () -> groupService.deleteGroups(List.of(9L)));
    }
}
