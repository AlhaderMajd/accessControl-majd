package com.example.accesscontrol.service;

import com.example.accesscontrol.dto.*;
import com.example.accesscontrol.entity.Role;
import com.example.accesscontrol.entity.User;
import com.example.accesscontrol.exception.DuplicateEmailException;
import com.example.accesscontrol.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserRoleService userRoleService;
    private final RoleService roleService;

    @PersistenceContext
    private EntityManager em;

    public BulkCreateUsersResponse createUsers(BulkCreateUsersRequest request) {
        List<CreateUserRequest> users = request.getUsers();

        if (users == null || users.isEmpty()) {
            throw new IllegalArgumentException("User list cannot be empty");
        }

        for (CreateUserRequest user : users) {
            if (!isValidEmail(user.getEmail()) || !isValidPassword(user.getPassword())) {
                throw new IllegalArgumentException("Invalid user input");
            }
        }

        List<String> emails = users.stream().map(CreateUserRequest::getEmail).toList();
        List<String> existingEmails = userRepository.findAllByEmailIn(emails)
                .stream().map(User::getEmail).toList();

        if (!existingEmails.isEmpty()) {
            throw new DuplicateEmailException("Some emails already in use: " + existingEmails);
        }

        List<User> userEntities = users.stream().map(u -> {
            User user = new User();
            user.setEmail(u.getEmail());
            user.setPassword(passwordEncoder.encode(u.getPassword()));
            user.setEnabled(u.isEnabled());
            return user;
        }).toList();

        List<User> savedUsers = userRepository.saveAll(userEntities);

        Role memberRole = roleService.getOrCreateRole("MEMBER");

        for (User savedUser : savedUsers) {
            userRoleService.assignRoleToUser(savedUser.getId(), memberRole.getName());
        }

        List<Long> ids = savedUsers.stream().map(User::getId).toList();
        return new BulkCreateUsersResponse(ids, List.of(memberRole.getName()));
    }

    public GetUsersResponse getUsers(String search, int page, int size) {
        String queryStr = "SELECT u FROM User u WHERE u.email LIKE :search ORDER BY u.id DESC";
        TypedQuery<User> query = em.createQuery(queryStr, User.class);
        query.setParameter("search", "%" + search + "%");
        query.setFirstResult(page * size);
        query.setMaxResults(size);

        List<UserDto> users = query.getResultList().stream().map(user ->
                new UserDto(user.getId(), user.getEmail(), user.isEnabled())
        ).collect(Collectors.toList());

        long total = em.createQuery("SELECT COUNT(u) FROM User u WHERE u.email LIKE :search", Long.class)
                .setParameter("search", "%" + search + "%")
                .getSingleResult();

        return new GetUsersResponse(users, page, total);
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }

    private boolean isValidPassword(String password) {
        return password != null && password.length() >= 6;
    }
}