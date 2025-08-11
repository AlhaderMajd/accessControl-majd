package com.example.accesscontrol.config;

import com.example.accesscontrol.entity.*;
import com.example.accesscontrol.enums.RoleType;
import com.example.accesscontrol.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional
    public void run(String... args) {
        // 1) Seed fixed roles from enum
        List<Role> roles = ensureFixedRoles();

        // 2) Seed minimum base data
        List<Permission> permissions = ensureMinPermissions(MIN_COUNT);
        List<Group> groups = ensureMinGroups(MIN_COUNT);
        List<User> users = ensureMinUsers(MIN_COUNT);

        // 3) Seed association tables to at least MIN_COUNT rows each
        ensureMinUserRoles(users, roles, MIN_COUNT);
        ensureMinUserGroups(users, groups, MIN_COUNT);
        ensureMinGroupRoles(groups, roles, MIN_COUNT);
        ensureMinRolePermissions(roles, permissions, MIN_COUNT);

        System.out.println("✅ DataInitializer completed.");
    }

    /* =========================
       Base data creators
       ========================= */

    private List<Role> ensureFixedRoles() {
        List<Role> out = new ArrayList<>();
        for (RoleType type : RoleType.values()) {
            Role role = roleRepository.findByName(type.name()).orElse(null);
            if (role == null) {
                role = roleRepository.save(Role.builder().name(type.name()).build());
            }
            out.add(role);
        }
        return out;
    }

    private List<Permission> ensureMinPermissions(int min) {
        List<Permission> existing = permissionRepository.findAll();
        Set<String> namesUpper = new HashSet<>();
        existing.forEach(p -> namesUpper.add(p.getName().toUpperCase(Locale.ROOT)));

        List<Permission> toInsert = new ArrayList<>();
        for (int i = 1; i <= min; i++) {
            String name = "PERM_" + String.format("%03d", i);
            if (!namesUpper.contains(name.toUpperCase(Locale.ROOT))) {
                toInsert.add(Permission.builder().name(name).build());
            }
        }
        if (!toInsert.isEmpty()) {
            existing.addAll(permissionRepository.saveAll(toInsert));
        }
        return existing;
    }

    private List<Group> ensureMinGroups(int min) {
        List<Group> existing = groupRepository.findAll();
        Set<String> namesUpper = new HashSet<>();
        existing.forEach(g -> namesUpper.add(g.getName().toUpperCase(Locale.ROOT)));

        List<Group> toInsert = new ArrayList<>();
        for (int i = 1; i <= min; i++) {
            String name = "Group " + String.format("%02d", i);
            if (!namesUpper.contains(name.toUpperCase(Locale.ROOT))) {
                toInsert.add(Group.builder().name(name).build());
            }
        }
        if (!toInsert.isEmpty()) {
            existing.addAll(groupRepository.saveAll(toInsert));
        }
        return existing;
    }

    private List<User> ensureMinUsers(int min) {
        List<User> existing = userRepository.findAll();
        Set<String> emailsLower = new HashSet<>();
        existing.forEach(u -> emailsLower.add(u.getEmail().toLowerCase(Locale.ROOT)));

        List<User> toInsert = new ArrayList<>();
        for (int i = 1; i <= min; i++) {
            String email = "user" + i + "@example.com";
            if (!emailsLower.contains(email.toLowerCase(Locale.ROOT))) {
                toInsert.add(User.builder()
                        .email(email)
                        .password(passwordEncoder.encode("123456"))
                        .enabled(true)
                        .build());
            }
        }
        if (!toInsert.isEmpty()) {
            existing.addAll(userRepository.saveAll(toInsert));
        }
        return existing;
    }

    /* =========================
       Association creators
       (use simple IDs + existsByXxx_IdAndYyy_Id)
       ========================= */

    private void ensureMinUserRoles(List<User> users, List<Role> roles, int min) {
        long current = userRoleRepository.count();
        if (current >= min || users.isEmpty() || roles.isEmpty()) return;

        int needed = (int) (min - current);
        List<UserRole> toInsert = new ArrayList<>(needed);

        for (int i = 0; i < needed; i++) {
            User u = users.get(i % users.size());
            Role r = roles.get(i % roles.size());

            if (!userRoleRepository.existsByUser_IdAndRole_Id(u.getId(), r.getId())) {
                toInsert.add(UserRole.builder()
                        .user(u)
                        .role(r)
                        .build());
            }
        }

        if (!toInsert.isEmpty()) userRoleRepository.saveAll(toInsert);
    }

    private void ensureMinUserGroups(List<User> users, List<Group> groups, int min) {
        long current = userGroupRepository.count();
        if (current >= min || users.isEmpty() || groups.isEmpty()) return;

        int needed = (int) (min - current);
        List<UserGroup> toInsert = new ArrayList<>(needed);

        for (int i = 0; i < needed; i++) {
            User u = users.get(i % users.size());
            Group g = groups.get(i % groups.size());

            if (!userGroupRepository.existsByUser_IdAndGroup_Id(u.getId(), g.getId())) {
                toInsert.add(UserGroup.builder()
                        .user(u)
                        .group(g)
                        .build());
            }
        }

        if (!toInsert.isEmpty()) userGroupRepository.saveAll(toInsert);
    }

    private void ensureMinGroupRoles(List<Group> groups, List<Role> roles, int min) {
        long current = groupRoleRepository.count();
        if (current >= min || groups.isEmpty() || roles.isEmpty()) return;

        int needed = (int) (min - current);
        List<GroupRole> toInsert = new ArrayList<>(needed);

        for (int i = 0; i < needed; i++) {
            Group g = groups.get(i % groups.size());
            Role r = roles.get(i % roles.size());

            if (!groupRoleRepository.existsByGroup_IdAndRole_Id(g.getId(), r.getId())) {
                toInsert.add(GroupRole.builder()
                        .group(g)
                        .role(r)
                        .build());
            }
        }

        if (!toInsert.isEmpty()) groupRoleRepository.saveAll(toInsert);
    }

    private void ensureMinRolePermissions(List<Role> roles, List<Permission> perms, int min) {
        long current = rolePermissionRepository.count();
        if (current >= min || roles.isEmpty() || perms.isEmpty()) return;

        int needed = (int) (min - current);
        List<RolePermission> toInsert = new ArrayList<>(needed);

        for (int i = 0; i < needed; i++) {
            Role r = roles.get(i % roles.size());
            Permission p = perms.get(i % perms.size());

            if (!rolePermissionRepository.existsByRole_IdAndPermission_Id(r.getId(), p.getId())) {
                toInsert.add(RolePermission.builder()
                        .role(r)
                        .permission(p)
                        .build());
            }
        }

        if (!toInsert.isEmpty()) rolePermissionRepository.saveAll(toInsert);
    }
}
