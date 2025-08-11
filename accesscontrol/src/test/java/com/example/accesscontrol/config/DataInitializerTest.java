package com.example.accesscontrol.config;

import com.example.accesscontrol.entity.*;
import com.example.accesscontrol.enums.RoleType;
import com.example.accesscontrol.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DataInitializerTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PermissionRepository permissionRepository;
    @Mock
    private GroupRepository groupRepository;
    @Mock
    private UserRoleRepository userRoleRepository;
    @Mock
    private UserGroupRepository userGroupRepository;
    @Mock
    private GroupRoleRepository groupRoleRepository;
    @Mock
    private RolePermissionRepository rolePermissionRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private DataInitializer initializer;

    @BeforeEach
    void setupDefaults() {
        // Roles absent by default; saving echoes argument
        given(roleRepository.findByName(anyString())).willReturn(Optional.empty());
        given(roleRepository.save(any(Role.class))).willAnswer(inv -> inv.getArgument(0));

        // Base tables empty by default
        given(permissionRepository.findAll()).willReturn(new ArrayList<>());
        given(groupRepository.findAll()).willReturn(new ArrayList<>());
        given(userRepository.findAll()).willReturn(new ArrayList<>());

        given(permissionRepository.saveAll(anyList())).willAnswer(inv -> inv.getArgument(0));
        given(groupRepository.saveAll(anyList())).willAnswer(inv -> inv.getArgument(0));
        given(userRepository.saveAll(anyList())).willAnswer(inv -> inv.getArgument(0));

        // Association counts 0 by default; existence checks false (so inserts happen)
        given(userRoleRepository.count()).willReturn(0L);
        given(userGroupRepository.count()).willReturn(0L);
        given(groupRoleRepository.count()).willReturn(0L);
        given(rolePermissionRepository.count()).willReturn(0L);

        given(userRoleRepository.existsByUser_IdAndRole_Id(any(), any())).willReturn(false);
        given(userGroupRepository.existsByUser_IdAndGroup_Id(any(), any())).willReturn(false);
        given(groupRoleRepository.existsByGroup_IdAndRole_Id(any(), any())).willReturn(false);
        given(rolePermissionRepository.existsByRole_IdAndPermission_Id(any(), any())).willReturn(false);

        given(userRoleRepository.saveAll(anyList())).willAnswer(inv -> inv.getArgument(0));
        given(userGroupRepository.saveAll(anyList())).willAnswer(inv -> inv.getArgument(0));
        given(groupRoleRepository.saveAll(anyList())).willAnswer(inv -> inv.getArgument(0));
        given(rolePermissionRepository.saveAll(anyList())).willAnswer(inv -> inv.getArgument(0));

        given(passwordEncoder.encode(anyString())).willReturn("encoded");
    }

    // ---------------------------- run() high-level scenarios ----------------------------

    @Test
    void run_whenEmpty_reseedsBaseAndAssociations() {
        initializer.run();

        // one save per RoleType when none exist
        verify(roleRepository, atLeast(RoleType.values().length)).save(any(Role.class));

        // base entities created
        verify(permissionRepository, atLeastOnce()).saveAll(anyList());
        verify(groupRepository, atLeastOnce()).saveAll(anyList());
        verify(userRepository, atLeastOnce()).saveAll(anyList());
        verify(passwordEncoder, atLeastOnce()).encode(anyString());

        // associations populated
        verify(userRoleRepository, atLeastOnce()).saveAll(anyList());
        verify(userGroupRepository, atLeastOnce()).saveAll(anyList());
        verify(groupRoleRepository, atLeastOnce()).saveAll(anyList());
        verify(rolePermissionRepository, atLeastOnce()).saveAll(anyList());
    }

    @Test
    void run_whenEverythingAlreadyPresent_skipsAllSaves() {
        // roles already exist
        given(roleRepository.findByName(anyString()))
                .willAnswer(inv -> Optional.of(Role.builder().name(inv.getArgument(0)).build()));

        // base tables already at/above MIN with matching names/emails
        List<Permission> perms = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            perms.add(Permission.builder().name(String.format(Locale.ROOT, "PERM_%03d", i)).build());
        }
        List<Group> groups = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            groups.add(Group.builder().name(String.format(Locale.ROOT, "Group %02d", i)).build());
        }
        List<User> users = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            users.add(User.builder().email("user" + i + "@example.com").password("exists").enabled(true).build());
        }
        given(permissionRepository.findAll()).willReturn(perms);
        given(groupRepository.findAll()).willReturn(groups);
        given(userRepository.findAll()).willReturn(users);

        // association counts already >= MIN
        given(userRoleRepository.count()).willReturn(10L);
        given(userGroupRepository.count()).willReturn(10L);
        given(groupRoleRepository.count()).willReturn(10L);
        given(rolePermissionRepository.count()).willReturn(10L);

        initializer.run();

        verify(roleRepository, never()).save(any(Role.class));
        verify(permissionRepository, never()).saveAll(anyList());
        verify(groupRepository, never()).saveAll(anyList());
        verify(userRepository, never()).saveAll(anyList());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRoleRepository, never()).saveAll(anyList());
        verify(userGroupRepository, never()).saveAll(anyList());
        verify(groupRoleRepository, never()).saveAll(anyList());
        verify(rolePermissionRepository, never()).saveAll(anyList());
    }

    @Test
    void run_whenAssociationsWouldDuplicate_skipsAssociationInserts() {
        // keep base empty -> base inserts happen
        // set counts < MIN but every candidate association already exists
        given(userRoleRepository.count()).willReturn(5L);
        given(userGroupRepository.count()).willReturn(5L);
        given(groupRoleRepository.count()).willReturn(5L);
        given(rolePermissionRepository.count()).willReturn(5L);

        given(userRoleRepository.existsByUser_IdAndRole_Id(any(), any())).willReturn(true);
        given(userGroupRepository.existsByUser_IdAndGroup_Id(any(), any())).willReturn(true);
        given(groupRoleRepository.existsByGroup_IdAndRole_Id(any(), any())).willReturn(true);
        given(rolePermissionRepository.existsByRole_IdAndPermission_Id(any(), any())).willReturn(true);

        initializer.run();

        // base saved, password encoded
        verify(permissionRepository, atLeastOnce()).saveAll(anyList());
        verify(groupRepository, atLeastOnce()).saveAll(anyList());
        verify(userRepository, atLeastOnce()).saveAll(anyList());
        verify(passwordEncoder, atLeastOnce()).encode(anyString());

        // no association saveAll because every candidate existed
        verify(userRoleRepository, never()).saveAll(anyList());
        verify(userGroupRepository, never()).saveAll(anyList());
        verify(groupRoleRepository, never()).saveAll(anyList());
        verify(rolePermissionRepository, never()).saveAll(anyList());
    }

    // ---------------------------- Branch coverage for early returns ----------------------------

    // ensureMinUserRoles: current >= min
    @Test
    void ensureMinUserRoles_earlyReturn_whenCountGteMin() {
        given(userRoleRepository.count()).willReturn(10L);
        ReflectionTestUtils.invokeMethod(
                initializer, "ensureMinUserRoles",
                List.of(User.builder().id(1L).build()),
                List.of(Role.builder().id(1L).build()),
                10
        );
        verify(userRoleRepository, never()).saveAll(anyList());
    }

    // ensureMinUserRoles: users.isEmpty()
    @Test
    void ensureMinUserRoles_earlyReturn_whenUsersEmpty() {
        given(userRoleRepository.count()).willReturn(0L);
        ReflectionTestUtils.invokeMethod(
                initializer, "ensureMinUserRoles",
                Collections.<User>emptyList(),
                List.of(Role.builder().id(1L).build()),
                10
        );
        verify(userRoleRepository, never()).saveAll(anyList());
    }

    // ensureMinUserRoles: roles.isEmpty()
    @Test
    void ensureMinUserRoles_earlyReturn_whenRolesEmpty() {
        given(userRoleRepository.count()).willReturn(0L);
        ReflectionTestUtils.invokeMethod(
                initializer, "ensureMinUserRoles",
                List.of(User.builder().id(1L).build()),
                Collections.<Role>emptyList(),
                10
        );
        verify(userRoleRepository, never()).saveAll(anyList());
    }

    // ensureMinUserGroups: current >= min
    @Test
    void ensureMinUserGroups_earlyReturn_whenCountGteMin() {
        given(userGroupRepository.count()).willReturn(10L);
        ReflectionTestUtils.invokeMethod(
                initializer, "ensureMinUserGroups",
                List.of(User.builder().id(1L).build()),
                List.of(Group.builder().id(1L).build()),
                10
        );
        verify(userGroupRepository, never()).saveAll(anyList());
    }

    // ensureMinUserGroups: users.isEmpty()
    @Test
    void ensureMinUserGroups_earlyReturn_whenUsersEmpty() {
        given(userGroupRepository.count()).willReturn(0L);
        ReflectionTestUtils.invokeMethod(
                initializer, "ensureMinUserGroups",
                Collections.<User>emptyList(),
                List.of(Group.builder().id(1L).build()),
                10
        );
        verify(userGroupRepository, never()).saveAll(anyList());
    }

    // ensureMinUserGroups: groups.isEmpty()
    @Test
    void ensureMinUserGroups_earlyReturn_whenGroupsEmpty() {
        given(userGroupRepository.count()).willReturn(0L);
        ReflectionTestUtils.invokeMethod(
                initializer, "ensureMinUserGroups",
                List.of(User.builder().id(1L).build()),
                Collections.<Group>emptyList(),
                10
        );
        verify(userGroupRepository, never()).saveAll(anyList());
    }

    // ensureMinGroupRoles: current >= min
    @Test
    void ensureMinGroupRoles_earlyReturn_whenCountGteMin() {
        given(groupRoleRepository.count()).willReturn(10L);
        ReflectionTestUtils.invokeMethod(
                initializer, "ensureMinGroupRoles",
                List.of(Group.builder().id(1L).build()),
                List.of(Role.builder().id(1L).build()),
                10
        );
        verify(groupRoleRepository, never()).saveAll(anyList());
    }

    // ensureMinGroupRoles: groups.isEmpty()
    @Test
    void ensureMinGroupRoles_earlyReturn_whenGroupsEmpty() {
        given(groupRoleRepository.count()).willReturn(0L);
        ReflectionTestUtils.invokeMethod(
                initializer, "ensureMinGroupRoles",
                Collections.<Group>emptyList(),
                List.of(Role.builder().id(1L).build()),
                10
        );
        verify(groupRoleRepository, never()).saveAll(anyList());
    }

    // ensureMinGroupRoles: roles.isEmpty()
    @Test
    void ensureMinGroupRoles_earlyReturn_whenRolesEmpty() {
        given(groupRoleRepository.count()).willReturn(0L);
        ReflectionTestUtils.invokeMethod(
                initializer, "ensureMinGroupRoles",
                List.of(Group.builder().id(1L).build()),
                Collections.<Role>emptyList(),
                10
        );
        verify(groupRoleRepository, never()).saveAll(anyList());
    }

    // ensureMinRolePermissions: current >= min
    @Test
    void ensureMinRolePermissions_earlyReturn_whenCountGteMin() {
        given(rolePermissionRepository.count()).willReturn(10L);
        ReflectionTestUtils.invokeMethod(
                initializer, "ensureMinRolePermissions",
                List.of(Role.builder().id(1L).build()),
                List.of(Permission.builder().id(1L).build()),
                10
        );
        verify(rolePermissionRepository, never()).saveAll(anyList());
    }

    // ensureMinRolePermissions: roles.isEmpty()
    @Test
    void ensureMinRolePermissions_earlyReturn_whenRolesEmpty() {
        given(rolePermissionRepository.count()).willReturn(0L);
        ReflectionTestUtils.invokeMethod(
                initializer, "ensureMinRolePermissions",
                Collections.<Role>emptyList(),
                List.of(Permission.builder().id(1L).build()),
                10
        );
        verify(rolePermissionRepository, never()).saveAll(anyList());
    }

    // ensureMinRolePermissions: perms.isEmpty()
    @Test
    void ensureMinRolePermissions_earlyReturn_whenPermsEmpty() {
        given(rolePermissionRepository.count()).willReturn(0L);
        ReflectionTestUtils.invokeMethod(
                initializer, "ensureMinRolePermissions",
                List.of(Role.builder().id(1L).build()),
                Collections.<Permission>emptyList(),
                10
        );
        verify(rolePermissionRepository, never()).saveAll(anyList());
    }
}
