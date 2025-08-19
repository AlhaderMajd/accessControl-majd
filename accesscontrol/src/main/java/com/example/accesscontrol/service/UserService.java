package com.example.accesscontrol.service;

import com.example.accesscontrol.config.logs;
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
import com.example.accesscontrol.exception.EmailAlreadyUsedException;
import com.example.accesscontrol.exception.InvalidCredentialsException;
import com.example.accesscontrol.exception.ResourceNotFoundException;
import com.example.accesscontrol.exception.UserNotFoundException;
import com.example.accesscontrol.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserRoleService userRoleService;
    private final UserGroupService userGroupService;
    private final RoleService roleService;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@(?:[A-Za-z0-9-]+\\.)+[A-Za-z]{2,24}$"
    );
    private final logs logs;


    @PersistenceContext
    private EntityManager em;

    void setEntityManager(EntityManager em) {
        this.em = em;
    }

    @Transactional
    public CreateUsersResponse createUsers(CreateUsersRequest request) {
        var users = request == null ? null : request.getUsers();
        if (users == null || users.isEmpty())
            throw new IllegalArgumentException("User list cannot be empty"); //400

        var normalized = users.stream().map(u -> {
            String email = (u.getEmail() == null) ? null : u.getEmail().trim();
            if (isInvalidEmail(email) || isInvalidPassword(u.getPassword())) {
                throw new IllegalArgumentException("Invalid user input"); //400
            }
            return CreateUserRequest.builder()
                    .email(email)
                    .password(u.getPassword())
                    .enabled(u.isEnabled())
                    .build();
        }).toList();

        var lowerEmails = normalized.stream().map(x -> x.getEmail().toLowerCase()).toList();
        var dupInPayload = lowerEmails.stream()
                .collect(java.util.stream.Collectors.groupingBy(e -> e, java.util.stream.Collectors.counting()))
                .entrySet().stream().filter(e -> e.getValue() > 1).map(java.util.Map.Entry::getKey).toList();
        if (!dupInPayload.isEmpty()) {
            throw new IllegalArgumentException("Duplicate emails in request"); //400
        }

        var existingEmails = userRepository.findAllByEmailIn(
                normalized.stream().map(CreateUserRequest::getEmail).toList()
        ).stream().map(User::getEmail).toList();
        if (!existingEmails.isEmpty()) {
            throw new EmailAlreadyUsedException("Some emails already in use"); //409
        }

        var entities = normalized.stream()
                .map(u -> User.builder()
                        .email(u.getEmail())
                        .password(passwordEncoder.encode(u.getPassword()))
                        .enabled(u.isEnabled())
                        .build())
                .toList();

        List<User> saved;
        try {
            saved = userRepository.saveAll(entities);
        } catch (DataIntegrityViolationException ex) {
            throw new EmailAlreadyUsedException("Some emails already in use"); //409
        }

        var userIds = saved.stream().map(User::getId).toList();

        Role memberRole = roleService.getOrCreateRole("MEMBER");
        int assigned = userRoleService.assignRolesToUsers(userIds, List.of(memberRole.getId()));

        var principal = SecurityContextHolder.getContext().getAuthentication();
        String actor = (principal == null) ? "unknown" : principal.getName();

        if (assigned != userIds.size()) {
            log.warn("users.create.partial_role_assignment created={} roles_assigned={} actor={}",
                    userIds.size(), assigned, logs.mask(actor));
        } else {
            log.info("users.create.success created={} roles_assigned={} actor={}",
                    userIds.size(), assigned, logs.mask(actor));
        }

        return new CreateUsersResponse(userIds, List.of(memberRole.getName()));
    }

    @Transactional(readOnly = true)
    public GetUsersResponse getUsers(String search, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Invalid pagination params"); //400
        }

        final String q = (search == null ? "" : search.trim().toLowerCase());
        PageRequest pr = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));

        Page<UserSummaryResponse> p = userRepository.searchUserSummaries(q, pr);

        return new GetUsersResponse(p.getContent(), page, p.getTotalElements()); //200
    }


    @Transactional(readOnly = true)
    public UserResponse getUserDetails(Long id) {
        if (id == null || id <= 0) throw new IllegalArgumentException("Invalid user ID"); //400
        User user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found")); //404
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
    public AdminUpdateCredentialsResponse updateCredentialsByAdmin(Long userId, AdminUpdateCredentialsRequest request) {
        if (request == null ||
                (!StringUtils.hasText(request.getEmail()) && !StringUtils.hasText(request.getPassword()))) {
            throw new IllegalArgumentException("At least one of email or password must be provided"); //400
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found")); //404

        boolean emailUpdated = false;
        boolean passwordUpdated = false;

        if (StringUtils.hasText(request.getEmail())) {
            String newEmail = request.getEmail().trim();
            if (isInvalidEmail(newEmail)) {
                throw new IllegalArgumentException("Invalid email format"); //400
            }
            if (!newEmail.equalsIgnoreCase(user.getEmail())) {
                if (emailExists(newEmail)) {
                    throw new EmailAlreadyUsedException("Email already in use"); //409
                }
                user.setEmail(newEmail);
                emailUpdated = true;
            }
        }

        if (StringUtils.hasText(request.getPassword())) {
            String newPwd = request.getPassword();
            if (isInvalidPassword(newPwd)) {
                throw new IllegalArgumentException("Password must meet security requirements"); //400
            }
            user.setPassword(passwordEncoder.encode(newPwd));
            passwordUpdated = true;
        }

        if (!emailUpdated && !passwordUpdated) {
            throw new IllegalArgumentException("Nothing to update"); //400
        }

        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            throw new EmailAlreadyUsedException("Email already in use"); //409
        }

        var auth = SecurityContextHolder.getContext().getAuthentication();
        String actor = (auth == null) ? "unknown" : auth.getName();
        log.info("users.admin.update_credentials success actor={} userId={} emailUpdated={} passwordUpdated={}",
                logs.mask(actor), user.getId(), emailUpdated, passwordUpdated);

        return AdminUpdateCredentialsResponse.builder()
                .message("Credentials updated successfully")
                .id(user.getId())
                .emailUpdated(emailUpdated)
                .passwordUpdated(passwordUpdated)
                .build();
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        if (request == null || isInvalidPassword(request.getNewPassword()))
            throw new IllegalArgumentException("Password must meet security requirements"); //400

        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            log.info("users.change_password failed reason=unauthenticated");
            throw new InvalidCredentialsException("Unauthenticated"); //401
        }

        User u = getByEmailOrThrow(auth.getName()); //404 via UserNotFoundException mapping if applicable

        if (request.getOldPassword().equals(request.getNewPassword())) {
            log.info("users.change_password failed reason=new_equals_old actor={}", logs.mask(u.getEmail()));
            throw new IllegalArgumentException("New password must be different from old password"); //400
        }

        if (!passwordEncoder.matches(request.getOldPassword(), u.getPassword())) {
            log.info("users.change_password failed reason=bad_old_password actor={}", logs.mask(u.getEmail()));
            throw new InvalidCredentialsException("Old password is incorrect"); //401
        }

        if (passwordEncoder.matches(request.getNewPassword(), u.getPassword())) {
            log.info("users.change_password failed reason=new_equals_current_hash actor={}", logs.mask(u.getEmail()));
            throw new IllegalArgumentException("New password must be different from old password"); //400
        }

        u.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(u);

        log.info("users.change_password success actor={}", logs.mask(u.getEmail()));
    }

    @Transactional
    public void changeEmail(UpdateEmailRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            log.info("users.change_email failed reason=unauthenticated");
            throw new InvalidCredentialsException("Unauthenticated"); //401
        }

        User u = getByEmailOrThrow(auth.getName()); //404 if not found

        String newEmail = request == null ? null : request.getNewEmail();
        if (isInvalidEmail(newEmail)) {
            log.info("users.change_email failed reason=invalid_format actor={}", logs.mask(u.getEmail()));
            throw new IllegalArgumentException("Invalid email format"); //400
        }

        newEmail = newEmail.trim();

        if (newEmail.equalsIgnoreCase(u.getEmail())) {
            log.info("users.change_email failed reason=same_email actor={}", logs.mask(u.getEmail()));
            throw new IllegalArgumentException("New email must be different from current email"); //400
        }

        if (userRepository.existsByEmailIgnoreCase(newEmail)) {
            log.info("users.change_email failed reason=conflict actor={} new={}", logs.mask(u.getEmail()), logs.mask(newEmail));
            throw new EmailAlreadyUsedException("Email already taken"); //409
        }

        try {
            u.setEmail(newEmail);
            userRepository.save(u);
        } catch (DataIntegrityViolationException ex) {
            log.info("users.change_email failed reason=unique_violation actor={} new={}", logs.mask(u.getEmail()), logs.mask(newEmail));
            throw new EmailAlreadyUsedException("Email already taken"); //409
        }

        log.info("users.change_email success old={} new={}", logs.mask(auth.getName()), logs.mask(newEmail));
    }

    @Transactional
    public UpdateUserStatusResponse updateUserStatus(UpdateUserStatusRequest request) {
        var userIdsRaw = request == null ? null : request.getUserIds();
        Boolean enabled = request == null ? null : request.getEnabled();

        if (userIdsRaw == null || userIdsRaw.isEmpty() || enabled == null)
            throw new IllegalArgumentException("User list or status flag is missing/invalid"); //400

        var userIds = userIdsRaw.stream()
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .distinct()
                .toList();
        if (userIds.isEmpty())
            throw new IllegalArgumentException("No valid user IDs provided"); //400

        var users = userRepository.findAllById(userIds);

        if (users.size() != userIds.size()) {
            var foundIds = users.stream().map(User::getId).collect(java.util.stream.Collectors.toSet());
            var missing = userIds.stream().filter(id -> !foundIds.contains(id)).toList();
            throw new ResourceNotFoundException("Some users not found"); //404
        }

        users.forEach(u -> u.setEnabled(enabled));
        var updated = userRepository.saveAll(users);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        String actor = (auth == null) ? "unknown" : auth.getName();
        log.info("users.status success actor={} updated={} enable={}", logs.mask(actor), updated.size(), enabled);

        return UpdateUserStatusResponse.builder()
                .message("User status updated successfully")
                .updatedCount(updated.size())
                .build();
    }

    @Transactional
    public AssignRolesResponse assignRolesToUsers(AssignRolesRequest request) {
        if (request == null || request.getUserIds() == null || request.getRoleIds() == null)
            throw new IllegalArgumentException("User or role list is invalid or empty"); //400

        var userIds = request.getUserIds().stream()
                .filter(Objects::nonNull).filter(id -> id > 0).distinct().toList();
        var roleIds = request.getRoleIds().stream()
                .filter(Objects::nonNull).filter(id -> id > 0).distinct().toList();

        if (userIds.isEmpty() || roleIds.isEmpty())
            throw new IllegalArgumentException("User or role list is invalid or empty"); //400

        var users = getByIdsOrThrow(userIds); //404
        var roles = roleService.getByIdsOrThrow(roleIds); //404

        int assigned = userRoleService.assignRolesToUsers(
                users.stream().map(User::getId).toList(),
                roles.stream().map(Role::getId).toList()
        );

        var auth = SecurityContextHolder.getContext().getAuthentication();
        String actor = (auth == null) ? "unknown" : auth.getName();
        log.info("users.roles.assign success actor={} users={} roles={} assigned={}",
                logs.mask(actor), users.size(), roles.size(), assigned);

        return AssignRolesResponse.builder()
                .message("Roles assigned successfully")
                .assignedCount(assigned)
                .build();
    }

    @Transactional
    public DeassignRolesResponse deassignRolesFromUsers(DeassignRolesRequest request) {
        if (request == null || request.getUserIds() == null || request.getRoleIds() == null)
            throw new IllegalArgumentException("User or role list is invalid or empty"); //400

        var userIds = request.getUserIds().stream()
                .filter(Objects::nonNull).filter(id -> id > 0).distinct().toList();
        var roleIds = request.getRoleIds().stream()
                .filter(Objects::nonNull).filter(id -> id > 0).distinct().toList();
        if (userIds.isEmpty() || roleIds.isEmpty())
            throw new IllegalArgumentException("User or role list is invalid or empty"); //400

        var users = getByIdsOrThrow(userIds); //404
        var roles = roleService.getByIdsOrThrow(roleIds); //404

        var resp = userRoleService.deassignRoles(users, roles);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        String actor = (auth == null) ? "unknown" : auth.getName();
        log.info("users.roles.deassign success actor={} users={} roles={} removed={}",
                logs.mask(actor), users.size(), roles.size(), resp.getRemovedCount());

        return resp;
    }

    @Transactional
    public AssignUsersToGroupsResponse assignUsersToGroups(AssignUsersToGroupsRequest request) {
        if (request == null || request.getUserIds() == null || request.getGroupIds() == null)
            throw new IllegalArgumentException("User or group list is invalid"); //400

        var userIds = request.getUserIds().stream()
                .filter(Objects::nonNull).filter(id -> id > 0).distinct().toList();
        var groupIds = request.getGroupIds().stream()
                .filter(Objects::nonNull).filter(id -> id > 0).distinct().toList();

        if (userIds.isEmpty() || groupIds.isEmpty())
            throw new IllegalArgumentException("User or group list is invalid"); //400

        getByIdsOrThrow(userIds); //404

        var clean = new AssignUsersToGroupsRequest();
        clean.setUserIds(userIds);
        clean.setGroupIds(groupIds);

        var resp = userGroupService.assignUsersToGroups(clean);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        String actor = (auth == null) ? "unknown" : auth.getName();
        log.info("users.groups.assign success actor={} users={} groups={} assigned={}",
                logs.mask(actor), userIds.size(), groupIds.size(), resp.getAssignedCount());

        return resp;
    }

    @Transactional
    public DeassignUsersFromGroupsResponse deassignUsersFromGroups(DeassignUsersFromGroupsRequest request) {
        if (request == null || request.getUserIds() == null || request.getGroupIds() == null)
            throw new IllegalArgumentException("User or group list is invalid"); //400

        var userIds = request.getUserIds().stream()
                .filter(Objects::nonNull).filter(id -> id > 0).distinct().toList();
        var groupIds = request.getGroupIds().stream()
                .filter(Objects::nonNull).filter(id -> id > 0).distinct().toList();

        if (userIds.isEmpty() || groupIds.isEmpty())
            throw new IllegalArgumentException("User or group list is invalid"); //400

        getByIdsOrThrow(userIds); //404

        var cleanReq = new DeassignUsersFromGroupsRequest();
        cleanReq.setUserIds(userIds);
        cleanReq.setGroupIds(groupIds);

        var resp = userGroupService.deassignUsersFromGroups(cleanReq);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        String actor = (auth == null) ? "unknown" : auth.getName();
        log.info("users.groups.deassign success actor={} users={} groups={} removed={}",
                logs.mask(actor), userIds.size(), groupIds.size(), resp.getRemovedCount());

        return resp;
    }

    @Transactional
    public DeleteUsersResponse deleteUsers(DeleteUsersRequest request) {
        var idsRaw = request == null ? null : request.getUserIds();
        if (idsRaw == null || idsRaw.isEmpty()) {
            throw new IllegalArgumentException("User ID list is invalid"); //400
        }

        var userIds = idsRaw.stream()
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .distinct()
                .toList();
        if (userIds.isEmpty()) {
            throw new IllegalArgumentException("User ID list is invalid"); //400
        }

        var users = userRepository.findAllById(userIds);
        if (users.size() != userIds.size()) {
            var found = users.stream().map(User::getId).collect(java.util.stream.Collectors.toSet());
            var missing = userIds.stream().filter(id -> !found.contains(id)).toList();
            throw new ResourceNotFoundException("Some users not found"); //404
        }

        try {
            userRoleService.deleteByUserIds(userIds);
            userGroupService.deleteByUserIds(userIds);
            userRepository.deleteAllByIdInBatch(userIds);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("Cannot delete users due to existing references"); //409
        }

        var auth = SecurityContextHolder.getContext().getAuthentication();
        String actor = (auth == null) ? "unknown" : auth.getName();
        log.info("users.delete success actor={} deleted={}", logs.mask(actor), userIds.size());

        return DeleteUsersResponse.builder()
                .message("Users deleted successfully")
                .deletedCount(userIds.size())
                .build();
    }

    private boolean isInvalidEmail(String email) {
        return email == null || email.isBlank() || !EMAIL_PATTERN.matcher(email).matches();
    }

    private boolean isInvalidPassword(String password) {
        return password == null || password.length() < 6;
    }

    @Transactional(readOnly = true)
    public List<User> getByIdsOrThrow(List<Long> userIds) {
        var users = userRepository.findAllById(userIds);
        if (users.size() != userIds.size()) throw new ResourceNotFoundException("Some users not found"); //404
        return users;
    }

    @Transactional(readOnly = true)
    public User getByEmailOrThrow(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new UserNotFoundException(email)); //404
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
}
