package com.example.accesscontrol.service;

import com.example.accesscontrol.dto.user.assignRolesToUser.AssignRolesRequest;
import com.example.accesscontrol.dto.user.assignRolesToUser.AssignRolesResponse;
import com.example.accesscontrol.dto.user.assignUsersToGroup.AssignUsersToGroupsRequest;
import com.example.accesscontrol.dto.user.assignUsersToGroup.AssignUsersToGroupsResponse;
import com.example.accesscontrol.dto.user.createUsers.CreateUserRequest;
import com.example.accesscontrol.dto.user.createUsers.CreateUsersRequest;
import com.example.accesscontrol.dto.user.createUsers.CreateUsersResponse;
import com.example.accesscontrol.dto.user.deassignUsersFromGroups.DeassignUsersFromGroupsRequest;
import com.example.accesscontrol.dto.user.deassignUsersFromGroups.DeassignUsersFromGroupsResponse;
import com.example.accesscontrol.dto.user.deassignUsersFromUsers.DeassignRolesRequest;
import com.example.accesscontrol.dto.user.deassignUsersFromUsers.DeassignRolesResponse;
import com.example.accesscontrol.dto.user.deleteUsers.DeleteUsersRequest;
import com.example.accesscontrol.dto.user.deleteUsers.DeleteUsersResponse;
import com.example.accesscontrol.dto.user.getUsers.GetUsersResponse;
import com.example.accesscontrol.dto.user.getUsers.UserResponse;
import com.example.accesscontrol.dto.user.getUsers.UserSummaryResponse;
import com.example.accesscontrol.dto.user.updateCredentials.AdminUpdateCredentialsRequest;
import com.example.accesscontrol.dto.user.updateCredentials.AdminUpdateCredentialsResponse;
import com.example.accesscontrol.dto.user.updateUserInfo.ChangePasswordRequest;
import com.example.accesscontrol.dto.user.updateUserInfo.UpdateEmailRequest;
import com.example.accesscontrol.dto.user.updateUserStatus.UpdateUserStatusRequest;
import com.example.accesscontrol.dto.user.updateUserStatus.UpdateUserStatusResponse;
import com.example.accesscontrol.entity.Role;
import com.example.accesscontrol.entity.User;
import com.example.accesscontrol.exception.*;
import com.example.accesscontrol.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
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

    void setEntityManager(EntityManager em) {
        this.em = em;
    }


    @Transactional
    public CreateUsersResponse createUsers(CreateUsersRequest request) {
        var users = request.getUsers();
        if (users == null || users.isEmpty()) throw new IllegalArgumentException("User list cannot be empty");
        for (var u : users)
            if (!isValidEmail(u.getEmail()) || !isValidPassword(u.getPassword()))
                throw new IllegalArgumentException("Invalid user input");

        var emails = users.stream().map(CreateUserRequest::getEmail).toList();
        var existingEmails = userRepository.findAllByEmailIn(emails).stream().map(User::getEmail).toList();
        if (!existingEmails.isEmpty())
            throw new EmailAlreadyUsedException("Some emails already in use: " + existingEmails);

        var entities = users.stream()
                .map(u -> User.builder()
                        .email(u.getEmail())
                        .password(passwordEncoder.encode(u.getPassword()))
                        .enabled(u.isEnabled())
                        .build())
                .toList();

        var saved = userRepository.saveAll(entities);
        var userIds = saved.stream().map(User::getId).toList();

        Role memberRole = roleService.getOrCreateRole("MEMBER");
        userRoleService.assignRolesToUsers(userIds, List.of(memberRole.getId()));

        return new CreateUsersResponse(userIds, List.of(memberRole.getName()));
    }

    @Transactional(readOnly = true)
    public GetUsersResponse getUsers(String search, int page, int size) {
        String q = (search == null) ? "" : search;
        TypedQuery<User> query = em.createQuery(
                "SELECT u FROM User u WHERE u.email LIKE :s ORDER BY u.id DESC", User.class);
        query.setParameter("s", "%" + q + "%")
                .setFirstResult(page * size)
                .setMaxResults(size);

        var users = query.getResultList().stream()
                .map(u -> new UserSummaryResponse(u.getId(), u.getEmail(), u.isEnabled()))
                .toList();

        long total = em.createQuery("SELECT COUNT(u) FROM User u WHERE u.email LIKE :s", Long.class)
                .setParameter("s", "%" + q + "%")
                .getSingleResult();

        return new GetUsersResponse(users, page, total);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserDetails(Long id) {
        if (id == null || id <= 0) throw new IllegalArgumentException("Invalid user ID");
        User user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        var roles = userRoleService.getRoleNamesByUserId(user.getId());
        var groups = userGroupService.getGroupNamesByUserId(user.getId());
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .enabled(user.isEnabled())
                .roles(roles)
                .groups(groups)
                .build();
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        if (!isValidPassword(request.getNewPassword()))
            throw new IllegalArgumentException("Password must meet security requirements");

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new InvalidCredentialsException("Unauthenticated");
        }
        User u = getByEmailOrThrow(auth.getName());
        if (!passwordEncoder.matches(request.getOldPassword(), u.getPassword()))
            throw new InvalidCredentialsException("Old password is incorrect");

        u.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(u);
    }

    @Transactional
    public void changeEmail(UpdateEmailRequest request) {
        String newEmail = request.getNewEmail();
        if (!isValidEmail(newEmail)) throw new IllegalArgumentException("Invalid email format");
        if (emailExists(newEmail)) throw new EmailAlreadyUsedException("Email already taken");

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new InvalidCredentialsException("Unauthenticated");
        }
        User u = getByEmailOrThrow(auth.getName());
        u.setEmail(newEmail);
        userRepository.save(u);
    }

    @Transactional
    public UpdateUserStatusResponse updateUserStatus(UpdateUserStatusRequest request) {
        var userIds = request.getUserIds();
        Boolean enabled = request.getEnabled();
        if (userIds == null || userIds.isEmpty() || enabled == null)
            throw new IllegalArgumentException("User list or status flag is missing/invalid");

        var users = userRepository.findAllById(userIds);
        if (users.isEmpty()) throw new ResourceNotFoundException("No users found to update");

        users.forEach(u -> u.setEnabled(enabled));
        var updated = userRepository.saveAll(users);
        return UpdateUserStatusResponse.builder()
                .message("User status updated successfully")
                .updatedCount(updated.size())
                .build();
    }

    @Transactional
    public AssignRolesResponse assignRolesToUsers(AssignRolesRequest request) {
        if (request.getUserIds() == null || request.getUserIds().isEmpty()
                || request.getRoleIds() == null || request.getRoleIds().isEmpty())
            throw new IllegalArgumentException("User or role list is invalid or empty");

        var users = getByIdsOrThrow(request.getUserIds());
        var roles = roleService.getByIdsOrThrow(request.getRoleIds());

        int n = userRoleService.assignRolesToUsers(
                users.stream().map(User::getId).toList(),
                roles.stream().map(Role::getId).toList()
        );
        return AssignRolesResponse.builder().message("Roles assigned successfully").assignedCount(n).build();
    }

    @Transactional
    public DeassignRolesResponse deassignRolesFromUsers(DeassignRolesRequest request) {
        var users = getByIdsOrThrow(request.getUserIds());
        var roles = roleService.getByIdsOrThrow(request.getRoleIds());
        return userRoleService.deassignRoles(users, roles);
    }

    @Transactional
    public AssignUsersToGroupsResponse assignUsersToGroups(AssignUsersToGroupsRequest request) {
        return userGroupService.assignUsersToGroups(request);
    }

    @Transactional
    public DeassignUsersFromGroupsResponse deassignUsersFromGroups(DeassignUsersFromGroupsRequest request) {
        if (request.getUserIds() == null || request.getUserIds().isEmpty()
                || request.getGroupIds() == null || request.getGroupIds().isEmpty())
            throw new IllegalArgumentException("User or group list is invalid");
        getByIdsOrThrow(request.getUserIds());
        return userGroupService.deassignUsersFromGroups(request);
    }

    @Transactional
    public DeleteUsersResponse deleteUsers(DeleteUsersRequest request) {
        var userIds = request.getUserIds();
        if (userIds == null || userIds.isEmpty()) throw new IllegalArgumentException("User ID list is invalid");
        var users = userRepository.findAllById(userIds);
        if (users.size() != userIds.size()) throw new ResourceNotFoundException("Some users not found");

        userRoleService.deleteByUserIds(userIds);
        userGroupService.deleteByUserIds(userIds);
        userRepository.deleteAllById(userIds);

        return DeleteUsersResponse.builder()
                .message("Users deleted successfully")
                .deletedCount(userIds.size())
                .build();
    }

    @Transactional(readOnly = true)
    public List<User> getByIdsOrThrow(List<Long> userIds) {
        var users = userRepository.findAllById(userIds);
        if (users.size() != userIds.size()) throw new ResourceNotFoundException("Some users not found");
        return users;
    }

    @Transactional(readOnly = true)
    public User getByEmailOrThrow(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new UserNotFoundException(email));
    }

    @Transactional(readOnly = true)
    public boolean emailExists(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    @Transactional
    public User save(User user) {
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<Long> getExistingIds(List<Long> ids) {
        return userRepository.findAllById(ids).stream().map(User::getId).toList();
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }

    private boolean isValidPassword(String password) {
        return password != null && password.length() >= 6;
    }

    @Transactional(readOnly = true)
    public List<UserSummaryResponse> getUserSummariesByIds(List<Long> userIds) {
        return userRepository.findAllById(userIds).stream()
                .map(u -> UserSummaryResponse.builder()
                        .id(u.getId())
                        .email(u.getEmail())
                        .enabled(u.isEnabled())
                        .build())
                .toList();
    }

    @Transactional
    public AdminUpdateCredentialsResponse updateCredentialsByAdmin(Long userId, AdminUpdateCredentialsRequest request) {
        if (request == null ||
                (!org.springframework.util.StringUtils.hasText(request.getEmail())
                        && !org.springframework.util.StringUtils.hasText(request.getPassword()))) {
            throw new IllegalArgumentException("At least one of email or password must be provided");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean emailUpdated = false;
        boolean passwordUpdated = false;

        if (org.springframework.util.StringUtils.hasText(request.getEmail())) {
            String newEmail = request.getEmail().trim();
            if (!isValidEmail(newEmail)) {
                throw new IllegalArgumentException("Invalid email format");
            }
            if (!newEmail.equalsIgnoreCase(user.getEmail()) && emailExists(newEmail)) {
                throw new EmailAlreadyUsedException("Email already in use");
            }
            user.setEmail(newEmail);
            emailUpdated = true;
        }

        if (org.springframework.util.StringUtils.hasText(request.getPassword())) {
            String newPwd = request.getPassword();
            if (!isValidPassword(newPwd)) {
                throw new IllegalArgumentException("Password must meet security requirements");
            }
            user.setPassword(passwordEncoder.encode(newPwd));
            passwordUpdated = true;
        }

        if (!emailUpdated && !passwordUpdated) {
            throw new IllegalArgumentException("Nothing to update");
        }

        userRepository.save(user);

        return AdminUpdateCredentialsResponse.builder()
                .message("Credentials updated successfully")
                .id(user.getId())
                .emailUpdated(emailUpdated)
                .passwordUpdated(passwordUpdated)
                .build();
    }
}
