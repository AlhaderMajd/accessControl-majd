package com.example.accesscontrol.config;

import com.example.accesscontrol.entity.*;
import com.example.accesscontrol.enums.RoleType;
import com.example.accesscontrol.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.*;

@Configuration
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final int MIN_COUNT = 10;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final GroupRepository groupRepository;

    private final UserRoleRepository userRoleRepository;
    private final UserGroupRepository userGroupRepository;
    private final GroupRoleRepository groupRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;

    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        List<Role> roles = ensureFixedRoles();

        List<Permission> perms = ensureMinPermissions(MIN_COUNT);
        List<Group> groups = ensureMinGroups(MIN_COUNT);
        List<User> users = ensureMinUsers(MIN_COUNT);

        ensureMinUserRoles(users, roles, MIN_COUNT);
        ensureMinUserGroups(users, groups, MIN_COUNT);
        ensureMinGroupRoles(groups, roles, MIN_COUNT);
        ensureMinRolePermissions(roles, perms, MIN_COUNT);

        System.out.println("✅ DataInitializer completed.");
    }

    private List<Role> ensureFixedRoles() {
        List<Role> roles = new ArrayList<>();
        for (RoleType type : RoleType.values()) {
            Role role = roleRepository.findByName(type.name()).orElse(null);
            if (role == null) {
                role = Role.builder().name(type.name()).build();
                role = roleRepository.save(role);
            }
            roles.add(role);
        }
        return roles;
    }

    private List<Permission> ensureMinPermissions(int min) {
        List<Permission> existing = permissionRepository.findAll();
        Set<String> names = new HashSet<>();
        existing.forEach(p -> names.add(p.getName().toUpperCase()));

        List<Permission> toInsert = new ArrayList<>();
        for (int i = 1; i <= min; i++) {
            String name = "PERM_" + String.format("%03d", i);
            if (!names.contains(name)) {
                toInsert.add(Permission.builder().name(name).build());
            }
        }
        if (!toInsert.isEmpty()) existing.addAll(permissionRepository.saveAll(toInsert));
        return existing;
    }

    private List<Group> ensureMinGroups(int min) {
        List<Group> existing = groupRepository.findAll();
        Set<String> names = new HashSet<>();
        existing.forEach(g -> names.add(g.getName().toUpperCase()));

        List<Group> toInsert = new ArrayList<>();
        for (int i = 1; i <= min; i++) {
            String name = "Group " + String.format("%02d", i);
            if (!names.contains(name.toUpperCase())) {
                toInsert.add(Group.builder().name(name).build());
            }
        }
        if (!toInsert.isEmpty()) existing.addAll(groupRepository.saveAll(toInsert));
        return existing;
    }

    private List<User> ensureMinUsers(int min) {
        List<User> existing = userRepository.findAll();
        Set<String> emails = new HashSet<>();
        existing.forEach(u -> emails.add(u.getEmail().toLowerCase()));

        List<User> toInsert = new ArrayList<>();
        for (int i = 1; i <= min; i++) {
            String email = "user" + i + "@example.com";
            if (!emails.contains(email)) {
                toInsert.add(User.builder()
                        .email(email)
                        .password(passwordEncoder.encode("123456"))
                        .enabled(true)
                        .build());
            }
        }
        if (!toInsert.isEmpty()) existing.addAll(userRepository.saveAll(toInsert));
        return existing;
    }

    private void ensureMinUserRoles(List<User> users, List<Role> roles, int min) {
        long current = userRoleRepository.count();
        if (current >= min) return;

        int needed = (int) (min - current);
        List<UserRole> toInsert = new ArrayList<>();

        for (int i = 0; i < needed; i++) {
            User u = users.get(i % users.size());
            Role r = roles.get(i % roles.size());
            UserRole.Id id = new UserRole.Id(u.getId(), r.getId());
            if (!userRoleRepository.existsById(id)) {
                toInsert.add(UserRole.builder().id(id).build());
            }
        }
        if (!toInsert.isEmpty()) userRoleRepository.saveAll(toInsert);
    }

    private void ensureMinUserGroups(List<User> users, List<Group> groups, int min) {
        long current = userGroupRepository.count();
        if (current >= min) return;

        int needed = (int) (min - current);
        List<UserGroup> toInsert = new ArrayList<>();

        for (int i = 0; i < needed; i++) {
            User u = users.get(i % users.size());
            Group g = groups.get(i % groups.size());
            UserGroup.Id id = new UserGroup.Id(u.getId(), g.getId());
            if (!userGroupRepository.existsById(id)) {
                toInsert.add(UserGroup.builder().id(id).build());
            }
        }
        if (!toInsert.isEmpty()) userGroupRepository.saveAll(toInsert);
    }

    private void ensureMinGroupRoles(List<Group> groups, List<Role> roles, int min) {
        long current = groupRoleRepository.count();
        if (current >= min) return;

        int needed = (int) (min - current);
        List<GroupRole> toInsert = new ArrayList<>();

        for (int i = 0; i < needed; i++) {
            Group g = groups.get(i % groups.size());
            Role r = roles.get(i % roles.size());
            GroupRole.Id id = new GroupRole.Id(g.getId(), r.getId());
            if (!groupRoleRepository.existsById(id)) {
                toInsert.add(GroupRole.builder().id(id).build());
            }
        }
        if (!toInsert.isEmpty()) groupRoleRepository.saveAll(toInsert);
    }

    private void ensureMinRolePermissions(List<Role> roles, List<Permission> perms, int min) {
        long current = rolePermissionRepository.count();
        if (current >= min) return;

        int needed = (int) (min - current);
        List<RolePermission> toInsert = new ArrayList<>();

        for (int i = 0; i < needed; i++) {
            Role r = roles.get(i % roles.size());
            Permission p = perms.get(i % perms.size());
            RolePermission.Id id = new RolePermission.Id(r.getId(), p.getId());
            if (!rolePermissionRepository.existsById(id)) {
                toInsert.add(RolePermission.builder().id(id).build());
            }
        }
        if (!toInsert.isEmpty()) rolePermissionRepository.saveAll(toInsert);
    }
}
