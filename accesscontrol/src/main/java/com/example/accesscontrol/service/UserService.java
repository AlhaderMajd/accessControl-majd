package com.example.accesscontrol.service;

import com.example.accesscontrol.dto.user.*;
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
    private final RoleService roleService;

    @PersistenceContext
    private EntityManager em;

    public BulkCreateUsersResponse createUsers(BulkCreateUsersRequest request) {
        List<CreateUserRequest> users = request.getUsers();
        if (users == null || users.isEmpty()) throw new IllegalArgumentException("User list cannot be empty");
        for (CreateUserRequest u : users) if (!isValidEmail(u.getEmail()) || !isValidPassword(u.getPassword())) throw new IllegalArgumentException("Invalid user input");

        List<String> emails = users.stream().map(CreateUserRequest::getEmail).toList();
        List<String> existingEmails = userRepository.findAllByEmailIn(emails).stream().map(User::getEmail).toList();
        if (!existingEmails.isEmpty()) throw new DuplicateEmailException("Some emails already in use: " + existingEmails);

        var entities = users.stream().map(u -> User.builder().email(u.getEmail()).password(passwordEncoder.encode(u.getPassword())).enabled(u.isEnabled()).build()).toList();
        var saved = userRepository.saveAll(entities);
        var userIds = saved.stream().map(User::getId).toList();

        Role memberRole = roleService.getOrCreateRole("MEMBER");
        userRoleService.assignRolesToUsers(userIds, List.of(memberRole.getId()));

        return new BulkCreateUsersResponse(userIds, List.of(memberRole.getName()));
    }

    public GetUsersResponse getUsers(String search, int page, int size) {
        String q = search == null ? "" : search;
        TypedQuery<User> query = em.createQuery("SELECT u FROM User u WHERE u.email LIKE :s ORDER BY u.id DESC", User.class);
        query.setParameter("s", "%" + q + "%").setFirstResult(page * size).setMaxResults(size);
        var users = query.getResultList().stream().map(u -> new UserSummaryResponse(u.getId(), u.getEmail(), u.isEnabled())).toList();
        long total = em.createQuery("SELECT COUNT(u) FROM User u WHERE u.email LIKE :s", Long.class).setParameter("s", "%" + q + "%").getSingleResult();
        return new GetUsersResponse(users, page, total);
    }

    public UserDetailsResponse getUserDetails(Long id) {
        if (id == null || id <= 0) throw new IllegalArgumentException("Invalid user ID");
        User user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        List<String> roles = userRoleService.getRoleNamesByUserId(user.getId());
        List<String> groups = userGroupService.getGroupNamesByUserId(user.getId());
        return UserDetailsResponse.builder().id(user.getId()).email(user.getEmail()).enabled(user.isEnabled()).roles(roles).groups(groups).build();
    }

    public void changePassword(ChangePasswordRequest request) {
        if (!isValidPassword(request.getNewPassword())) throw new IllegalArgumentException("Password must meet security requirements");
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof User u)) throw new RuntimeException("Unexpected principal type: " + principal.getClass().getName());
        if (!passwordEncoder.matches(request.getOldPassword(), u.getPassword())) throw new InvalidCredentialsException("Old password is incorrect");
        u.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(u);
    }

    public void changeEmail(UpdateEmailRequest request) {
        String newEmail = request.getNewEmail();
        if (!isValidEmail(newEmail)) throw new IllegalArgumentException("Invalid email format");
        if (emailExists(newEmail)) throw new EmailAlreadyUsedException("Email already taken");
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof User u)) throw new RuntimeException("Unexpected principal type");
        u.setEmail(newEmail);
        userRepository.save(u);
    }

    public UpdateUserStatusResponse updateUserStatus(UpdateUserStatusRequest request) {
        List<Long> userIds = request.getUserIds();
        Boolean enabled = request.getEnabled();
        if (userIds == null || userIds.isEmpty() || enabled == null) throw new IllegalArgumentException("User list or status flag is missing/invalid");
        var users = userRepository.findAllById(userIds);
        if (users.isEmpty()) throw new ResourceNotFoundException("No users found to update");
        users.forEach(u -> u.setEnabled(enabled));
        var updated = userRepository.saveAll(users);
        return UpdateUserStatusResponse.builder().message("User status updated successfully").updatedCount(updated.size()).build();
    }

    public AssignRolesResponse assignRolesToUsers(AssignRolesRequest request) {
        if (request.getUserIds() == null || request.getUserIds().isEmpty() || request.getRoleIds() == null || request.getRoleIds().isEmpty())
            throw new IllegalArgumentException("User or role list is invalid or empty");
        var users = getByIdsOrThrow(request.getUserIds());
        var roles = roleService.getByIdsOrThrow(request.getRoleIds());
        int n = userRoleService.assignRolesToUsers(users.stream().map(User::getId).toList(), roles.stream().map(Role::getId).toList());
        return AssignRolesResponse.builder().message("Roles assigned successfully").assignedCount(n).build();
    }

    public DeassignRolesResponse deassignRolesFromUsers(DeassignRolesRequest request) {
        var users = getByIdsOrThrow(request.getUserIds());
        var roles = roleService.getByIdsOrThrow(request.getRoleIds());
        return userRoleService.deassignRoles(users, roles);
    }

    public AssignUsersToGroupsResponse assignUsersToGroups(AssignUsersToGroupsRequest request) {
        return userGroupService.assignUsersToGroups(request);
    }

    public DeassignUsersFromGroupsResponse deassignUsersFromGroups(DeassignUsersFromGroupsRequest request) {
        if (request.getUserIds() == null || request.getUserIds().isEmpty() || request.getGroupIds() == null || request.getGroupIds().isEmpty())
            throw new IllegalArgumentException("User or group list is invalid");
        getByIdsOrThrow(request.getUserIds());
        return userGroupService.deassignUsersFromGroups(request);
    }

    @Transactional
    public DeleteUsersResponse deleteUsers(DeleteUsersRequest request) {
        List<Long> userIds = request.getUserIds();
        if (userIds == null || userIds.isEmpty()) throw new IllegalArgumentException("User ID list is invalid");
        var users = userRepository.findAllById(userIds);
        if (users.size() != userIds.size()) throw new ResourceNotFoundException("Some users not found");
        userRoleService.deleteByUserIds(userIds);
        userGroupService.deleteByUserIds(userIds);
        userRepository.deleteAllById(userIds);
        return DeleteUsersResponse.builder().message("Users deleted successfully").deletedCount(userIds.size()).build();
    }

    public List<User> getByIdsOrThrow(List<Long> userIds) {
        var users = userRepository.findAllById(userIds);
        if (users.size() != userIds.size()) throw new ResourceNotFoundException("Some users not found");
        return users;
    }

    public User getByEmailOrThrow(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new UserNotFoundException(email));
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

    public List<UserSummaryResponse> getUserSummariesByIds(List<Long> userIds) {
        return userRepository.findAllById(userIds).stream()
                .map(u -> UserSummaryResponse.builder().id(u.getId()).email(u.getEmail()).enabled(u.isEnabled()).build())
                .toList();
    }
}
