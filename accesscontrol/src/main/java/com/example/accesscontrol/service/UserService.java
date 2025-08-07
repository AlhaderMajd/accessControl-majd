package com.example.accesscontrol.service;

import com.example.accesscontrol.dto.*;
import com.example.accesscontrol.entity.Role;
import com.example.accesscontrol.entity.User;
import com.example.accesscontrol.exception.*;
import com.example.accesscontrol.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserRoleService userRoleService;
    private final UserGroupService userGroupService;
    private final GroupService groupService;
    private final RoleService  roleService;

    @PersistenceContext
    private EntityManager em;

    public BulkCreateUsersResponse createUsers(BulkCreateUsersRequest request) {
        List<CreateUserRequest> users = request.getUsers();
        if (users == null || users.isEmpty()) throw new IllegalArgumentException("User list cannot be empty");

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

        List<UserDto> users = query.getResultList().stream()
                .map(user -> new UserDto(user.getId(), user.getEmail(), user.isEnabled()))
                .toList();

        long total = em.createQuery("SELECT COUNT(u) FROM User u WHERE u.email LIKE :search", Long.class)
                .setParameter("search", "%" + search + "%")
                .getSingleResult();

        return new GetUsersResponse(users, page, total);
    }

    public UserDetailsResponse getUserDetails(Long id) {
        if (id == null || id <= 0) throw new IllegalArgumentException("Invalid user ID");

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<String> roles = userRoleService.getRoleNamesByUserId(user.getId());
        List<String> groups = userGroupService.getGroupNamesByUserId(user.getId());

        return UserDetailsResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .enabled(user.isEnabled())
                .roles(roles)
                .groups(groups)
                .build();
    }

    public void changePassword(ChangePasswordRequest request) {
        if (!isValidPassword(request.getNewPassword())) {
            throw new IllegalArgumentException("Password must meet security requirements");
        }

        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof User user)) {
            throw new RuntimeException("Unexpected principal type: " + principal.getClass().getName());
        }

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Old password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    public void changeEmail(UpdateEmailRequest request) {
        String newEmail = request.getNewEmail();
        if (!isValidEmail(newEmail)) throw new IllegalArgumentException("Invalid email format");
        if (emailExists(newEmail)) throw new EmailAlreadyUsedException("Email already taken");

        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof User user)) {
            throw new RuntimeException("Unexpected principal type");
        }

        user.setEmail(newEmail);
        userRepository.save(user);
    }

    public UpdateUserStatusResponse updateUserStatus(UpdateUserStatusRequest request) {
        List<Long> userIds = request.getUserIds();
        Boolean enabled = request.getEnabled();

        if (userIds == null || userIds.isEmpty() || enabled == null) {
            throw new IllegalArgumentException("User list or status flag is missing/invalid");
        }

        List<User> users = userRepository.findAllById(userIds);
        if (users.isEmpty()) throw new ResourceNotFoundException("No users found to update");

        users.forEach(user -> user.setEnabled(enabled));
        List<User> updatedUsers = userRepository.saveAll(users);

        return UpdateUserStatusResponse.builder()
                .message("User status updated successfully")
                .updatedCount(updatedUsers.size())
                .build();
    }

    public AssignRolesResponse assignRolesToUsers(AssignRolesRequest request) {
        List<Long> userIds = request.getUserIds();
        List<Long> roleIds = request.getRoleIds();

        if (userIds == null || userIds.isEmpty() || roleIds == null || roleIds.isEmpty()) {
            throw new IllegalArgumentException("User or role list is invalid or empty");
        }

        List<User> users = getByIdsOrThrow(userIds);
        List<Role> roles = roleService.getByIdsOrThrow(roleIds);

        int assignedCount = userRoleService.assignRolesToUsers(
                users.stream().map(User::getId).toList(),
                roles.stream().map(Role::getId).toList()
        );

        return AssignRolesResponse.builder()
                .message("Roles assigned successfully")
                .assignedCount(assignedCount)
                .build();
    }

    public DeassignRolesResponse deassignRolesFromUsers(DeassignRolesRequest request) {
        List<User> users = getByIdsOrThrow(request.getUserIds());
        List<Role> roles = roleService.getByIdsOrThrow(request.getRoleIds());
        return userRoleService.deassignRoles(users, roles);
    }

    public AssignUsersToGroupsResponse assignUsersToGroups(AssignUsersToGroupsRequest request) {
        return userGroupService.assignUsersToGroups(request);
    }

    public DeassignUsersFromGroupsResponse deassignUsersFromGroups(DeassignUsersFromGroupsRequest request) {
        List<Long> userIds = request.getUserIds();
        List<Long> groupIds = request.getGroupIds();
        if (userIds == null || userIds.isEmpty() || groupIds == null || groupIds.isEmpty()) {
            throw new IllegalArgumentException("User or group list is invalid");
        }

        getByIdsOrThrow(userIds);
        groupService.getByIdsOrThrow(groupIds);
        return userGroupService.deassignUsersFromGroups(request);
    }

    @Transactional
    public DeleteUsersResponse deleteUsers(DeleteUsersRequest request) {
        List<Long> userIds = request.getUserIds();
        if (userIds == null || userIds.isEmpty()) {
            throw new IllegalArgumentException("User ID list is invalid");
        }

        List<User> users = userRepository.findAllById(userIds);
        if (users.size() != userIds.size()) {
            throw new ResourceNotFoundException("Some users not found");
        }

        try {
            userRoleService.deleteByUserIds(userIds);
            userGroupService.deleteByUserIds(userIds);
            userRepository.deleteAllById(userIds);

            return DeleteUsersResponse.builder()
                    .message("Users deleted successfully")
                    .deletedCount(userIds.size())
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete users");
        }
    }

    public List<User> getByIdsOrThrow(List<Long> userIds) {
        List<User> users = userRepository.findAllById(userIds);
        if (users.size() != userIds.size()) throw new ResourceNotFoundException("Some users not found");
        return users;
    }

    public User getByEmailOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));
    }

    public boolean emailExists(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }

    private boolean isValidPassword(String password) {
        return password != null && password.length() >= 6;
    }

    @Transactional
    public void cleanupRolesFromUsers(List<Long> roleIds) {
        userRoleService.deleteByRoleIds(roleIds);
    }
}
