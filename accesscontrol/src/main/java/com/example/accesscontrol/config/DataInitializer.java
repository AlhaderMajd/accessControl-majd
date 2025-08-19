package com.example.accesscontrol.config;

import com.example.accesscontrol.entity.Group;
import com.example.accesscontrol.entity.Permission;
import com.example.accesscontrol.entity.Role;
import com.example.accesscontrol.entity.User;
import com.example.accesscontrol.repository.GroupRepository;
import com.example.accesscontrol.repository.PermissionRepository;
import com.example.accesscontrol.repository.RoleRepository;
import com.example.accesscontrol.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final GroupRepository groupRepository;
    private final PasswordEncoder passwordEncoder;

    private static final List<String> ROLE_NAMES = List.of("ADMIN", "MEMBER");

    private static final List<String> PERMISSION_NAMES = List.of(
            "USER_CREATE", "USER_UPDATE", "USER_DELETE", "USER_VIEW",
            "ROLE_CREATE", "ROLE_UPDATE", "ROLE_DELETE", "ROLE_VIEW",
            "GROUP_CREATE", "GROUP_UPDATE", "GROUP_DELETE", "GROUP_VIEW",
            "PERMISSION_VIEW"
    );

    private static final List<String> GROUP_NAMES = List.of("Engineering", "Marketing", "HR", "Students", "Staff");

    @Override
    @Transactional
    public void run(String... args) {
        final String sentinelEmail = "user1@local";
        if (userRepository.findByEmail(sentinelEmail).isPresent()) {
            log.info("DataInitializer: seed data already present. Skipping.");
            return;
        }

        log.info("DataInitializer: seeding default data…");

        Map<String, Role> roles = ensureRoles(ROLE_NAMES);
        Map<String, Permission> perms = ensurePermissions(PERMISSION_NAMES);
        Map<String, Group> groups = ensureGroups(GROUP_NAMES);

        wireRolePermissions(roles, perms);

        List<User> users = IntStream.rangeClosed(1, 10)
                .mapToObj(i -> {
                    String email = "user" + i + "@example.com";
                    String pwd = "password" + i;
                    boolean enabled = (i % 2 == 0);
                    if (i == 1) enabled = true;
                    return ensureUser(email, pwd, enabled);
                })
                .collect(Collectors.toList());

        User admin = users.get(0);
        addUserRole(admin, roles.get("ADMIN"));
        users.forEach(u -> addUserRole(u, roles.get("MEMBER")));
        userRepository.saveAll(users);

        List<Group> groupList = new ArrayList<>(groups.values());
        for (int i = 0; i < users.size(); i++) {
            addUserGroup(users.get(i), groupList.get(i % groupList.size()));
        }
        userRepository.saveAll(users);

        for (Group g : groupList) {
            addGroupRole(g, roles.get("MEMBER"));
        }
        roleRepository.saveAll(List.of(roles.get("ADMIN"), roles.get("MEMBER")));

        log.info("DataInitializer: seeding complete.");
    }


    private Map<String, Role> ensureRoles(Collection<String> names) {
        Map<String, Role> out = new LinkedHashMap<>();
        for (String n : names) {
            Role r = roleRepository.findByName(n)
                    .orElseGet(() -> roleRepository.save(Role.builder().name(n).build()));
            out.put(n, r);
        }
        return out;
    }

    private Map<String, Permission> ensurePermissions(Collection<String> names) {
        Set<String> lower = names.stream().map(s -> s.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
        List<Permission> existing = permissionRepository.findByNameInIgnoreCase(lower);

        Map<String, Permission> byName = existing.stream()
                .collect(Collectors.toMap(Permission::getName, p -> p, (a, b) -> a, LinkedHashMap::new));

        for (String n : names) {
            boolean exists = byName.keySet().stream().anyMatch(x -> x.equalsIgnoreCase(n));
            if (!exists) {
                Permission saved = permissionRepository.save(Permission.builder().name(n).build());
                byName.put(saved.getName(), saved);
            }
        }

        Map<String, Permission> out = new LinkedHashMap<>();
        byName.values().forEach(p -> out.put(p.getName(), p));
        return out;
    }

    private Map<String, Group> ensureGroups(Collection<String> names) {
        Set<String> lower = names.stream().map(s -> s.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
        List<Group> existing = groupRepository.findByNameInIgnoreCase(lower);

        Map<String, Group> byName = existing.stream()
                .collect(Collectors.toMap(Group::getName, g -> g, (a, b) -> a, LinkedHashMap::new));

        for (String n : names) {
            boolean exists = byName.keySet().stream().anyMatch(x -> x.equalsIgnoreCase(n));
            if (!exists) {
                Group saved = groupRepository.save(Group.builder().name(n).build());
                byName.put(saved.getName(), saved);
            }
        }

        Map<String, Group> out = new LinkedHashMap<>();
        byName.values().forEach(g -> out.put(g.getName(), g));
        return out;
    }

    private User ensureUser(String email, String rawPassword, boolean enabled) {
        return userRepository.findByEmail(email).orElseGet(() ->
                userRepository.save(User.builder()
                        .email(email)
                        .password(passwordEncoder.encode(rawPassword))
                        .enabled(enabled)
                        .build()));
    }

    private void wireRolePermissions(Map<String, Role> roles, Map<String, Permission> perms) {
        Role admin = roles.get("ADMIN");
        Role member = roles.get("MEMBER");

        admin.getPermissions().addAll(perms.values());
        roleRepository.save(admin);

        List<Permission> memberView = perms.values().stream()
                .filter(p -> p.getName().endsWith("_VIEW") || p.getName().equals("PERMISSION_VIEW"))
                .toList();
        member.getPermissions().addAll(memberView);
        roleRepository.save(member);
    }

    private void addUserRole(User user, Role role) {
        user.getRoles().add(role);
    }

    private void addUserGroup(User user, Group group) {
        user.getGroups().add(group);
    }

    private void addGroupRole(Group group, Role role) {
        role.getGroups().add(group);
    }
}
