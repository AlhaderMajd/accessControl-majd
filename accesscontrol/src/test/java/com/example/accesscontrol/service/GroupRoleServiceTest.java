package com.example.accesscontrol.service;

import com.example.accesscontrol.entity.Group;
import com.example.accesscontrol.entity.GroupRole;
import com.example.accesscontrol.entity.Role;
import com.example.accesscontrol.repository.GroupRepository;
import com.example.accesscontrol.repository.GroupRoleRepository;
import com.example.accesscontrol.repository.RoleRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupRoleServiceTest {

    @Mock private GroupRoleRepository groupRoleRepository;
    @Mock private GroupRepository groupRepository;
    @Mock private RoleRepository roleRepository;

    @InjectMocks private GroupRoleService service;

    private static GroupRole gr(long gId, long rId) {
        GroupRole gr = new GroupRole();
        gr.setGroup(Group.builder().id(gId).build());
        gr.setRole(Role.builder().id(rId).build());
        return gr;
    }

    @Nested
    class AssignPairs {

        @Test
        void assign_skipsExisting_andInsertsNew_returnsCount() {
            // Order-agnostic stub
            when(groupRoleRepository.findByGroup_IdInAndRole_IdIn(anyList(), anyList()))
                    .thenReturn(List.of(gr(1, 10)));

            Map<Long, Set<Long>> wanted = new HashMap<>();
            wanted.put(1L, Set.of(10L, 11L)); // (1,10) exists, (1,11) new
            wanted.put(2L, Set.of(10L));      // (2,10) new

            int inserted = service.assignGroupRolePairs(wanted);

            assertThat(inserted).isEqualTo(2);

            ArgumentCaptor<List<GroupRole>> cap = ArgumentCaptor.forClass(List.class);
            verify(groupRoleRepository).saveAll(cap.capture());
            List<GroupRole> saved = cap.getValue();
            assertThat(saved)
                    .extracting(x -> x.getGroup().getId() + "_" + x.getRole().getId())
                    .containsExactlyInAnyOrder("1_11", "2_10");

            verify(groupRoleRepository).findByGroup_IdInAndRole_IdIn(anyList(), anyList());
            verifyNoMoreInteractions(groupRoleRepository);
            verifyNoInteractions(groupRepository, roleRepository);
        }

        @Test
        void assign_whenNullOrEmpty_returns0_andNoRepoCalls() {
            assertThat(service.assignGroupRolePairs(null)).isZero();
            assertThat(service.assignGroupRolePairs(Collections.emptyMap())).isZero();

            verifyNoInteractions(groupRoleRepository, groupRepository, roleRepository);
        }

        @Test
        void assign_whenIntegrityViolation_usesDeltaBetweenBeforeAndAfter() {
            // before -> only (1,10)
            // after  -> (1,10) & (1,11)
            when(groupRoleRepository.findByGroup_IdInAndRole_IdIn(anyList(), anyList()))
                    .thenReturn(List.of(gr(1, 10)))
                    .thenReturn(List.of(gr(1, 10), gr(1, 11)));

            doThrow(new DataIntegrityViolationException("dup"))
                    .when(groupRoleRepository).saveAll(anyList());

            Map<Long, Set<Long>> wanted = Map.of(1L, Set.of(10L, 11L));

            int inserted = service.assignGroupRolePairs(wanted);

            assertThat(inserted).isEqualTo(1);

            verify(groupRoleRepository, times(2))
                    .findByGroup_IdInAndRole_IdIn(anyList(), anyList());
            verify(groupRoleRepository).saveAll(anyList());
            verifyNoMoreInteractions(groupRoleRepository);
            verifyNoInteractions(groupRepository, roleRepository);
        }
    }

    @Nested
    class DeletePairs {
        @Test
        void delete_whenMatchesExisting_deletesAndReturnsCount() {
            when(groupRoleRepository.findByGroup_IdInAndRole_IdIn(anyList(), anyList()))
                    .thenReturn(List.of(gr(1, 10), gr(1, 11), gr(2, 10)));

            Map<Long, Set<Long>> wanted = new HashMap<>();
            wanted.put(1L, Set.of(11L)); // delete (1,11)
            wanted.put(2L, Set.of(10L)); // delete (2,10)

            int removed = service.deleteGroupRolePairs(wanted);
            assertThat(removed).isEqualTo(2);

            ArgumentCaptor<List<GroupRole>> cap = ArgumentCaptor.forClass(List.class);
            verify(groupRoleRepository).deleteAllInBatch(cap.capture());
            assertThat(cap.getValue())
                    .extracting(x -> x.getGroup().getId() + "_" + x.getRole().getId())
                    .containsExactlyInAnyOrder("1_11", "2_10");

            verify(groupRoleRepository).findByGroup_IdInAndRole_IdIn(anyList(), anyList());
            verifyNoMoreInteractions(groupRoleRepository);
            verifyNoInteractions(groupRepository, roleRepository);
        }

        @Test
        void delete_whenNullOrEmpty_returns0_andNoRepoCalls() {
            assertThat(service.deleteGroupRolePairs(null)).isZero();
            assertThat(service.deleteGroupRolePairs(Collections.emptyMap())).isZero();
            verifyNoInteractions(groupRoleRepository, groupRepository, roleRepository);
        }
    }

    @Nested
    class CascadeDeletes {
        @Test
        void deleteByRoleIds_nullOrEmpty_noop() {
            service.deleteByRoleIds(null);
            service.deleteByRoleIds(Collections.emptyList());
            verifyNoInteractions(groupRoleRepository);
        }

        @Test
        void deleteByRoleIds_callsRepo() {
            service.deleteByRoleIds(List.of(1L, 2L));
            verify(groupRoleRepository).deleteByRole_IdIn(List.of(1L, 2L));
            verifyNoMoreInteractions(groupRoleRepository);
        }

        @Test
        void deleteByGroupIds_nullOrEmpty_noop() {
            service.deleteByGroupIds(null);
            service.deleteByGroupIds(Collections.emptyList());
            verifyNoInteractions(groupRoleRepository);
        }

        @Test
        void deleteByGroupIds_callsRepo() {
            service.deleteByGroupIds(List.of(3L, 4L));
            verify(groupRoleRepository).deleteByGroup_IdIn(List.of(3L, 4L));
            verifyNoMoreInteractions(groupRoleRepository);
        }
    }

    @Nested
    class Reads {
        @Test
        void getRoleIdsByGroupId_mapsIds() {
            when(groupRoleRepository.findByGroup_Id(7L))
                    .thenReturn(List.of(gr(7, 100), gr(7, 101), gr(7, 101)));

            List<Long> ids = service.getRoleIdsByGroupId(7L);
            assertThat(ids).containsExactly(100L, 101L, 101L);

            verify(groupRoleRepository).findByGroup_Id(7L);
            verifyNoMoreInteractions(groupRoleRepository);
        }

        @Test
        void getExistingGroupIds_returnsIdsFromRepo() {
            when(groupRepository.findAllById(List.of(1L, 9L, 8L)))
                    .thenReturn(List.of(Group.builder().id(1L).build(), Group.builder().id(8L).build()));

            List<Long> existing = service.getExistingGroupIds(List.of(1L, 9L, 8L));
            assertThat(existing).containsExactlyInAnyOrder(1L, 8L);

            verify(groupRepository).findAllById(List.of(1L, 9L, 8L));
            verifyNoMoreInteractions(groupRepository);
        }

        @Test
        void getExistingRoleIds_returnsIdsFromRepo() {
            when(roleRepository.findAllById(List.of(10L, 11L)))
                    .thenReturn(List.of(Role.builder().id(10L).build()));

            List<Long> existing = service.getExistingRoleIds(List.of(10L, 11L));
            assertThat(existing).containsExactly(10L);

            verify(roleRepository).findAllById(List.of(10L, 11L));
            verifyNoMoreInteractions(roleRepository);
        }
    }
}
