package com.example.accesscontrol.service;

import com.example.accesscontrol.entity.Group;
import com.example.accesscontrol.entity.GroupRole;
import com.example.accesscontrol.entity.Role;
import com.example.accesscontrol.repository.GroupRepository;
import com.example.accesscontrol.repository.GroupRoleRepository;
import com.example.accesscontrol.repository.RoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupRoleServiceTest {

    @Mock private GroupRoleRepository groupRoleRepository;
    @Mock private GroupRepository groupRepository;
    @Mock private RoleRepository roleRepository;

    @InjectMocks private GroupRoleService groupRoleService;

    @Test
    void assignRolesToGroups_nullOrEmpty_returnsZero() {
        assertEquals(0, groupRoleService.assignRolesToGroups(null, List.of(1L)));
        assertEquals(0, groupRoleService.assignRolesToGroups(List.of(), List.of(1L)));
        assertEquals(0, groupRoleService.assignRolesToGroups(List.of(1L), null));
        assertEquals(0, groupRoleService.assignRolesToGroups(List.of(1L), List.of()));
        verify(groupRoleRepository, never()).saveAll(anyList());
    }

    @Test
    void assignRolesToGroups_insertsOnlyMissing_andReturnsCount() {
        List<Long> groupIds = List.of(1L, 2L);
        List<Long> roleIds = List.of(10L, 11L);

        GroupRole existing = new GroupRole();
        existing.setGroup(Group.builder().id(1L).build());
        existing.setRole(Role.builder().id(10L).build());
        when(groupRoleRepository.findByGroup_IdInAndRole_IdIn(groupIds, roleIds))
                .thenReturn(List.of(existing));
        when(groupRoleRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        int inserted = groupRoleService.assignRolesToGroups(groupIds, roleIds);
        assertEquals(3, inserted);
        verify(groupRoleRepository).saveAll(anyList());
    }

    @Test
    void deassignRolesFromGroups_nullOrEmpty_noCall() {
        groupRoleService.deassignRolesFromGroups(null, List.of(1L));
        groupRoleService.deassignRolesFromGroups(List.of(), List.of(1L));
        groupRoleService.deassignRolesFromGroups(List.of(1L), null);
        groupRoleService.deassignRolesFromGroups(List.of(1L), List.of());
        verify(groupRoleRepository, never()).deleteByGroup_IdInAndRole_IdIn(anyList(), anyList());
    }

    @Test
    void deassignRolesFromGroups_valid_callsRepo() {
        groupRoleService.deassignRolesFromGroups(List.of(1L), List.of(2L));
        verify(groupRoleRepository).deleteByGroup_IdInAndRole_IdIn(List.of(1L), List.of(2L));
    }

    @Test
    void deleteByRoleIds_nullOrEmpty_noCall() {
        groupRoleService.deleteByRoleIds(null);
        groupRoleService.deleteByRoleIds(List.of());
        verify(groupRoleRepository, never()).deleteByRole_IdIn(anyList());
    }

    @Test
    void deleteByRoleIds_valid_callsRepo() {
        groupRoleService.deleteByRoleIds(List.of(1L));
        verify(groupRoleRepository).deleteByRole_IdIn(List.of(1L));
    }

    @Test
    void deleteByGroupIds_nullOrEmpty_noCall() {
        groupRoleService.deleteByGroupIds(null);
        groupRoleService.deleteByGroupIds(List.of());
        verify(groupRoleRepository, never()).deleteByGroup_IdIn(anyList());
    }

    @Test
    void deleteByGroupIds_valid_callsRepo() {
        groupRoleService.deleteByGroupIds(List.of(1L));
        verify(groupRoleRepository).deleteByGroup_IdIn(List.of(1L));
    }

    @Test
    void getRoleIdsByGroupId_mapsEntities() {
        GroupRole gr = new GroupRole();
        gr.setGroup(Group.builder().id(7L).build());
        gr.setRole(Role.builder().id(8L).build());
        when(groupRoleRepository.findByGroup_Id(7L)).thenReturn(List.of(gr));
        assertEquals(List.of(8L), groupRoleService.getRoleIdsByGroupId(7L));
    }

    @Test
    void getExistingGroupIds_delegatesToRepository() {
        when(groupRepository.findAllById(List.of(1L))).thenReturn(List.of(Group.builder().id(1L).build()));
        assertEquals(List.of(1L), groupRoleService.getExistingGroupIds(List.of(1L)));
    }

    @Test
    void getExistingRoleIds_delegatesToRepository() {
        when(roleRepository.findAllById(List.of(2L))).thenReturn(List.of(Role.builder().id(2L).build()));
        assertEquals(List.of(2L), groupRoleService.getExistingRoleIds(List.of(2L)));
    }
}
