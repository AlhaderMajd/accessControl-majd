package com.example.accesscontrol.config;

import com.example.accesscontrol.entity.*;
import com.example.accesscontrol.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Insert roles if missing
        Role adminRole = roleRepository.findByName("ADMIN").orElse(null);
        if (adminRole == null) {
            adminRole = new Role();
            adminRole.setName("ADMIN");
            adminRole = roleRepository.save(adminRole);
        }

        Role memberRole = roleRepository.findByName("MEMBER").orElse(null);
        if (memberRole == null) {
            memberRole = new Role();
            memberRole.setName("MEMBER");
            memberRole = roleRepository.save(memberRole);
        }

        // Insert users if none exist
        if (userRepository.count() == 0) {
            User admin = User.builder()
                    .email("admin@example.com")
                    .password(passwordEncoder.encode("123456"))
                    .enabled(true)
                    .build();

            User adminAndMember = User.builder()
                    .email("adminandmember@example.com")
                    .password(passwordEncoder.encode("123456"))
                    .enabled(true)
                    .build();

            User user = User.builder()
                    .email("user@example.com")
                    .password(passwordEncoder.encode("123456"))
                    .enabled(true)
                    .build();

            admin = userRepository.save(admin);
            adminAndMember = userRepository.save(adminAndMember);
            user = userRepository.save(user);

            // Assign roles
            userRoleRepository.save(new UserRole(admin.getId(), adminRole.getId()));
            userRoleRepository.save(new UserRole(adminAndMember.getId(), adminRole.getId()));
            userRoleRepository.save(new UserRole(adminAndMember.getId(), memberRole.getId()));
            userRoleRepository.save(new UserRole(user.getId(), memberRole.getId()));

            System.out.println("✅ Inserted test users and roles:");
            System.out.println("   admin@example.com [ADMIN]");
            System.out.println("   adminandmember@example.com [ADMIN, MEMBER]");
            System.out.println("   user@example.com  [MEMBER]");
        }
    }
}
