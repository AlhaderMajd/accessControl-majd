package com.example.accesscontrol.service;

import com.example.accesscontrol.dto.common.MessageResponse;
import com.example.accesscontrol.dto.common.PageResponse;
import com.example.accesscontrol.dto.group.CreateGroupRequest;
import com.example.accesscontrol.dto.group.CreateGroupsResponse;
import com.example.accesscontrol.dto.group.GroupDetailsResponse;
import com.example.accesscontrol.dto.group.GroupResponse;
import com.example.accesscontrol.dto.group.UpdateGroupNameRequest;
import com.example.accesscontrol.dto.group.UpdateGroupNameResponse;
import com.example.accesscontrol.dto.role.RoleResponse;
import com.example.accesscontrol.dto.user.getUsers.UserSummaryResponse;
import com.example.accesscontrol.entity.Group;
import com.example.accesscontrol.exception.DuplicateResourceException;
import com.example.accesscontrol.exception.ResourceNotFoundException;
import com.example.accesscontrol.repository.GroupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @Mock private GroupRepository groupRepository;
    @Mock private UserGroupService userGroupService;
    @Mock private GroupRoleService groupRoleService;
    @Mock private UserService userService;
    @Mock private RoleService roleService;

    @InjectMocks private GroupService service;

    private static CreateGroupRequest req(String name) {
        CreateGroupRequest r = new CreateGroupRequest();
        r.setName(name);
        return r;
    }

    @Nested
    class CreateGroups {
        @Test
        void create_success_savesAll_andReturnsItems() {
            List<CreateGroupRequest> items = List.of(req("Team A"), req("Team B"));

            when(groupRepository.findByNameInIgnoreCase(List.of("Team A", "Team B"))).thenReturn(List.of());
            when(groupRepository.saveAll(anyList())).thenAnswer(inv -> {
                List<Group> toSave = inv.getArgument(0);
                long id = 1L;
                for (Group g : toSave) {
                    g.setId(id++);
                }
                return toSave;
            });

            CreateGroupsResponse resp = service.createGroups(items);

            assertThat(resp.getMessage()).isEqualTo("Groups created successfully");
            assertThat(resp.getCreatedCount()).isEqualTo(2);
            assertThat(resp.getItems())
                    .extracting(GroupResponse::getName)
                    .containsExactlyInAnyOrder("Team A", "Team B");
            assertThat(resp.getItems())
                    .allSatisfy(gr -> assertThat(gr.getId()).isNotNull());

            verify(groupRepository).findByNameInIgnoreCase(List.of("Team A", "Team B"));
            ArgumentCaptor<List<Group>> cap = ArgumentCaptor.forClass(List.class);
            verify(groupRepository).saveAll(cap.capture());
            assertThat(cap.getValue()).hasSize(2);
            verifyNoMoreInteractions(groupRepository);
            verifyNoInteractions(userGroupService, groupRoleService, userService, roleService);
        }

        @Test
        void create_nullOrEmpty_throwsIllegalArg() {
            assertThatThrownBy(() -> service.createGroups(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Group names are required");
            assertThatThrownBy(() -> service.createGroups(List.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Group names are required");
            verifyNoInteractions(groupRepository);
        }

        @Test
        void create_blankOrMissingName_throwsIllegalArg() {
            List<CreateGroupRequest> items = List.of(req("  "), req("X"));
            assertThatThrownBy(() -> service.createGroups(items))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Group names are required");
        }

        @Test
        void create_duplicateInRequest_throwsDuplicate() {
            List<CreateGroupRequest> items = List.of(req("Alpha"), req("alpha"));
            assertThatThrownBy(() -> service.createGroups(items))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("Duplicate group names in request");
            verifyNoInteractions(groupRepository);
        }

        @Test
        void create_existingInDb_throwsDuplicate() {
            List<CreateGroupRequest> items = List.of(req("A"), req("B"));
            when(groupRepository.findByNameInIgnoreCase(List.of("A", "B")))
                    .thenReturn(List.of(Group.builder().id(7L).name("A").build()));

            assertThatThrownBy(() -> service.createGroups(items))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("Some group names already exist");

            verify(groupRepository).findByNameInIgnoreCase(List.of("A", "B"));
            verifyNoMoreInteractions(groupRepository);
        }

        @Test
        void create_integrityViolation_rechecksAndThrowsDuplicate() {
            List<CreateGroupRequest> items = List.of(req("A"), req("B"));

            when(groupRepository.findByNameInIgnoreCase(List.of("A", "B")))
                    .thenReturn(List.of()) // first (before save)
                    .thenReturn(List.of(Group.builder().id(1L).name("A").build())); // second (after failure)

            when(groupRepository.saveAll(anyList()))
                    .thenThrow(new DataIntegrityViolationException("dupe"));

            assertThatThrownBy(() -> service.createGroups(items))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("Some group names already exist");

            verify(groupRepository, times(2)).findByNameInIgnoreCase(List.of("A", "B"));
            verify(groupRepository).saveAll(anyList());
        }
    }

    @Nested
    class GetGroups {
        @Test
        void getGroups_mapsPage_andNormalizesPaging() {
            Page<Group> page = new PageImpl<>(
                    List.of(Group.builder().id(3L).name("g3").build(),
                            Group.builder().id(1L).name("g1").build()),
                    PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "id")), 2
            );

            when(groupRepository.findByNameContainingIgnoreCase(anyString(), any(Pageable.class)))
                    .thenReturn(page);

            PageResponse<GroupResponse> resp = service.getGroups(" q ", 0, 2);

            assertThat(resp.getItems())
                    .extracting(GroupResponse::getId, GroupResponse::getName)
                    .containsExactlyInAnyOrder(tuple(3L, "g3"), tuple(1L, "g1"));
            assertThat(resp.getPage()).isEqualTo(0);
            assertThat(resp.getSize()).isEqualTo(2);
            assertThat(resp.getTotal()).isEqualTo(2);

            verify(groupRepository).findByNameContainingIgnoreCase(eq("q"), any(Pageable.class));
        }


    }

    @Nested
    class GetGroupDetails {
        @Test
        void details_fetchesIds_buildsAndSortsUsersAndRoles() {
            Group g = Group.builder().id(5L).name("ENG").build();
            when(groupRepository.findById(5L)).thenReturn(Optional.of(g));

            when(userGroupService.getUserIdsByGroupId(5L)).thenReturn(List.of(2L, 1L));
            when(groupRoleService.getRoleIdsByGroupId(5L)).thenReturn(List.of(10L, 9L));

            // Unsorted users (by email then id)
            var u1 = UserSummaryResponse.builder().id(2L).email("z@x.com").build();
            var u2 = UserSummaryResponse.builder().id(1L).email("a@x.com").build();
            when(userService.getUserSummariesByIds(List.of(2L, 1L)))
                    .thenReturn(List.of(u1, u2));

            // Unsorted roles (by name then id)
            var r1 = RoleResponse.builder().id(10L).name("Zeta").build();
            var r2 = RoleResponse.builder().id(9L).name("Alpha").build();
            when(roleService.getRoleSummariesByIds(List.of(10L, 9L)))
                    .thenReturn(List.of(r1, r2));

            GroupDetailsResponse resp = service.getGroupDetails(5L);

            assertThat(resp.getId()).isEqualTo(5L);
            assertThat(resp.getName()).isEqualTo("ENG");
            assertThat(resp.getUsers()).extracting(UserSummaryResponse::getEmail)
                    .containsExactly("a@x.com", "z@x.com");
            assertThat(resp.getRoles()).extracting(RoleResponse::getName)
                    .containsExactly("Alpha", "Zeta");

            verify(groupRepository).findById(5L);
            verify(userGroupService).getUserIdsByGroupId(5L);
            verify(groupRoleService).getRoleIdsByGroupId(5L);
            verify(userService).getUserSummariesByIds(List.of(2L, 1L));
            verify(roleService).getRoleSummariesByIds(List.of(10L, 9L));
            verifyNoMoreInteractions(groupRepository, userGroupService, groupRoleService, userService, roleService);
        }

        @Test
        void details_groupNotFound_throws() {
            when(groupRepository.findById(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.getGroupDetails(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Group not found");
        }
    }

    @Nested
    class UpdateGroupName {
        @Test
        void nullOrBlank_throwsIllegalArg() {
            assertThatThrownBy(() -> service.updateGroupName(1L, null))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> service.updateGroupName(1L, new UpdateGroupNameRequest()))
                    .isInstanceOf(IllegalArgumentException.class);
            UpdateGroupNameRequest r = new UpdateGroupNameRequest();
            r.setName("   ");
            assertThatThrownBy(() -> service.updateGroupName(1L, r))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void sameNameIgnoringCase_noChange_returnsOk_andDoesNotSave() {
            Group g = Group.builder().id(7L).name("DevOps").build();
            when(groupRepository.findById(7L)).thenReturn(Optional.of(g));

            UpdateGroupNameRequest r = new UpdateGroupNameRequest();
            r.setName("devops");

            UpdateGroupNameResponse resp = service.updateGroupName(7L, r);

            assertThat(resp.getId()).isEqualTo(7L);
            assertThat(resp.getOldName()).isEqualTo("DevOps");
            assertThat(resp.getNewName()).isEqualTo("DevOps");
            verify(groupRepository, never()).save(any());
        }

        @Test
        void duplicateNameInDb_throwsDuplicate() {
            Group g = Group.builder().id(7L).name("Old").build();
            when(groupRepository.findById(7L)).thenReturn(Optional.of(g));
            when(groupRepository.findByNameInIgnoreCase(List.of("New")))
                    .thenReturn(List.of(Group.builder().id(99L).name("New").build()));

            UpdateGroupNameRequest r = new UpdateGroupNameRequest();
            r.setName("New");

            assertThatThrownBy(() -> service.updateGroupName(7L, r))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessage("Group name already exists");

            verify(groupRepository, never()).save(any());
        }

        @Test
        void saveThrowsIntegrityViolation_wrappedAsDuplicate() {
            Group g = Group.builder().id(7L).name("Old").build();
            when(groupRepository.findById(7L)).thenReturn(Optional.of(g));
            when(groupRepository.findByNameInIgnoreCase(List.of("New"))).thenReturn(List.of());
            when(groupRepository.save(any(Group.class)))
                    .thenThrow(new DataIntegrityViolationException("dupe"));

            UpdateGroupNameRequest r = new UpdateGroupNameRequest();
            r.setName("New");
            assertThatThrownBy(() -> service.updateGroupName(7L, r))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessage("Group name already exists");
        }

        @Test
        void success_updates_andReturnsResponse() {
            Group g = Group.builder().id(5L).name("Old").build();
            when(groupRepository.findById(5L)).thenReturn(Optional.of(g));
            when(groupRepository.findByNameInIgnoreCase(List.of("New"))).thenReturn(List.of());
            when(groupRepository.save(any(Group.class))).thenAnswer(inv -> inv.getArgument(0));

            UpdateGroupNameRequest r = new UpdateGroupNameRequest();
            r.setName("New");

            UpdateGroupNameResponse resp = service.updateGroupName(5L, r);

            assertThat(resp.getMessage()).isEqualTo("Group name updated successfully");
            assertThat(resp.getId()).isEqualTo(5L);
            assertThat(resp.getOldName()).isEqualTo("Old");
            assertThat(resp.getNewName()).isEqualTo("New");

            verify(groupRepository).save(argThat(gr -> gr.getId().equals(5L) && gr.getName().equals("New")));
        }
    }

    @Nested
    class DeleteGroups {
        @Test
        void nullOrEmpty_throwsIllegalArg() {
            assertThatThrownBy(() -> service.deleteGroups(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid or empty group IDs list");
            assertThatThrownBy(() -> service.deleteGroups(List.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid or empty group IDs list");
        }

        @Test
        void invalidIdsAfterFiltering_throwsIllegalArg() {
            assertThatThrownBy(() -> service.deleteGroups(Arrays.asList(null, -1L, 0L)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid or empty group IDs list");
        }

        @Test
        void someMissing_throwsResourceNotFound() {
            List<Long> ids = List.of(1L, 2L, 3L);
            when(groupRepository.findAllById(ids))
                    .thenReturn(List.of(Group.builder().id(1L).build(), Group.builder().id(3L).build()));

            assertThatThrownBy(() -> service.deleteGroups(ids))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Some groups not found");

            verify(groupRepository).findAllById(ids);
            verifyNoMoreInteractions(groupRepository);
            verifyNoInteractions(userGroupService, groupRoleService);
        }

        @Test
        void success_cascadesThenDeletes_andReturnsMessage() {
            List<Long> ids = List.of(5L, 6L);
            when(groupRepository.findAllById(ids))
                    .thenReturn(List.of(Group.builder().id(5L).build(), Group.builder().id(6L).build()));

            MessageResponse resp = service.deleteGroups(ids);

            assertThat(resp.getMessage()).isEqualTo("Group(s) deleted successfully");
            verify(userGroupService).deleteByGroupIds(ids);
            verify(groupRoleService).deleteByGroupIds(ids);
            verify(groupRepository).deleteAllByIdInBatch(ids);
        }

        @Test
        void integrityViolation_wrappedIntoIllegalArg() {
            List<Long> ids = List.of(5L);
            when(groupRepository.findAllById(ids))
                    .thenReturn(List.of(Group.builder().id(5L).build()));

            doThrow(new DataIntegrityViolationException("fk fails"))
                    .when(groupRepository).deleteAllByIdInBatch(ids);

            assertThatThrownBy(() -> service.deleteGroups(ids))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cannot delete groups due to existing references");

            verify(userGroupService).deleteByGroupIds(ids);
            verify(groupRoleService).deleteByGroupIds(ids);
            verify(groupRepository).deleteAllByIdInBatch(ids);
        }
    }

    @Nested
    class GetById {
        @Test
        void found_returnsEntity() {
            Group g = Group.builder().id(9L).name("X").build();
            when(groupRepository.findById(9L)).thenReturn(Optional.of(g));

            Group out = service.getByIdOrThrow(9L);

            assertThat(out).isSameAs(g);
        }

        @Test
        void notFound_throws() {
            when(groupRepository.findById(404L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.getByIdOrThrow(404L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Group not found");
        }
    }
}
