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
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private UserRoleService userRoleService;
    @Mock private UserGroupService userGroupService;
    @Mock private RoleService roleService;
    @Mock private EntityManager em;
    @Mock private TypedQuery<User> typedQuery;

    @InjectMocks private UserService userService;

    @BeforeEach
    void wireEntityManager() {
        userService.setEntityManager(em);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    // -------- createUsers --------

    @Test
    void createUsers_success_assignsMemberRole() {
        CreateUserRequest u1 = new CreateUserRequest();
        u1.setEmail("a@b.com");
        u1.setPassword("secret1");
        u1.setEnabled(true);
        CreateUsersRequest req = new CreateUsersRequest();
        req.setUsers(List.of(u1));

        when(userRepository.findAllByEmailIn(List.of("a@b.com"))).thenReturn(List.of());
        when(passwordEncoder.encode("secret1")).thenReturn("enc");
        when(userRepository.saveAll(anyList())).thenAnswer(inv -> {
            @SuppressWarnings("unchecked") List<User> list = (List<User>) inv.getArgument(0);
            list.forEach(u -> u.setId(10L));
            return list;
        });
        when(roleService.getOrCreateRole("MEMBER")).thenReturn(Role.builder().id(5L).name("MEMBER").build());
        when(userRoleService.assignRolesToUsers(List.of(10L), List.of(5L))).thenReturn(1);

        CreateUsersResponse resp = userService.createUsers(req);

        assertEquals(List.of(10L), resp.getCreatedUserIds());
        assertEquals(List.of("MEMBER"), resp.getAssignedRoles());
    }

    @Test
    void createUsers_nullUsers_throws() {
        CreateUsersRequest req = new CreateUsersRequest();
        req.setUsers(null);
        assertThrows(IllegalArgumentException.class, () -> userService.createUsers(req));
    }

    @Test
    void createUsers_emptyUsers_throws() {
        CreateUsersRequest req = new CreateUsersRequest();
        req.setUsers(List.of());
        assertThrows(IllegalArgumentException.class, () -> userService.createUsers(req));
    }

    @Test
    void createUsers_invalidEmailOrPassword_throws() {
        // invalid email
        CreateUserRequest bad1 = new CreateUserRequest();
        bad1.setEmail("not-email");
        bad1.setPassword("good123");
        CreateUsersRequest req1 = new CreateUsersRequest();
        req1.setUsers(List.of(bad1));
        assertThrows(IllegalArgumentException.class, () -> userService.createUsers(req1));

        // short password
        CreateUserRequest bad2 = new CreateUserRequest();
        bad2.setEmail("ok@x.com");
        bad2.setPassword("123");
        CreateUsersRequest req2 = new CreateUsersRequest();
        req2.setUsers(List.of(bad2));
        assertThrows(IllegalArgumentException.class, () -> userService.createUsers(req2));

        // null email
        CreateUserRequest bad3 = new CreateUserRequest();
        bad3.setEmail(null);
        bad3.setPassword("good123");
        CreateUsersRequest req3 = new CreateUsersRequest();
        req3.setUsers(List.of(bad3));
        assertThrows(IllegalArgumentException.class, () -> userService.createUsers(req3));

        // null password
        CreateUserRequest bad4 = new CreateUserRequest();
        bad4.setEmail("ok@x.com");
        bad4.setPassword(null);
        CreateUsersRequest req4 = new CreateUsersRequest();
        req4.setUsers(List.of(bad4));
        assertThrows(IllegalArgumentException.class, () -> userService.createUsers(req4));
    }

    @Test
    void createUsers_duplicateEmails_throws() {
        CreateUserRequest u1 = new CreateUserRequest();
        u1.setEmail("a@b.com");
        u1.setPassword("secret1");
        CreateUsersRequest req = new CreateUsersRequest();
        req.setUsers(List.of(u1));
        when(userRepository.findAllByEmailIn(List.of("a@b.com")))
                .thenReturn(List.of(User.builder().id(1L).email("a@b.com").build()));
        assertThrows(EmailAlreadyUsedException.class, () -> userService.createUsers(req));
    }

    // -------- getUsers --------

    @Test
    void getUsers_paged_success() {
        when(em.createQuery(anyString(), eq(User.class))).thenReturn(typedQuery);
        when(typedQuery.setParameter(eq("s"), any())).thenReturn(typedQuery);
        when(typedQuery.setFirstResult(anyInt())).thenReturn(typedQuery);
        when(typedQuery.setMaxResults(anyInt())).thenReturn(typedQuery);
        when(typedQuery.getResultList()).thenReturn(
                List.of(User.builder().id(2L).email("x@y.com").enabled(true).build()));
        @SuppressWarnings("unchecked")
        jakarta.persistence.TypedQuery<Long> countQ = mock(jakarta.persistence.TypedQuery.class);
        when(em.createQuery(startsWith("SELECT COUNT(u)"), eq(Long.class))).thenReturn(countQ);
        when(countQ.setParameter(eq("s"), any())).thenReturn(countQ);
        when(countQ.getSingleResult()).thenReturn(1L);

        GetUsersResponse resp = userService.getUsers("x", 0, 10);
        assertEquals(1, resp.getTotal());
        assertEquals(0, resp.getPage());
        assertEquals("x@y.com", resp.getUsers().get(0).getEmail());
    }

    @Test
    void getUsers_nullSearch_qBecomesEmptyString() {
        when(em.createQuery(anyString(), eq(User.class))).thenReturn(typedQuery);
        when(typedQuery.setParameter(eq("s"), any())).thenReturn(typedQuery);
        when(typedQuery.setFirstResult(anyInt())).thenReturn(typedQuery);
        when(typedQuery.setMaxResults(anyInt())).thenReturn(typedQuery);
        when(typedQuery.getResultList()).thenReturn(
                List.of(User.builder().id(5L).email("n@x.com").enabled(true).build()));
        @SuppressWarnings("unchecked")
        jakarta.persistence.TypedQuery<Long> countQ = mock(jakarta.persistence.TypedQuery.class);
        when(em.createQuery(startsWith("SELECT COUNT(u)"), eq(Long.class))).thenReturn(countQ);
        when(countQ.setParameter(eq("s"), any())).thenReturn(countQ);
        when(countQ.getSingleResult()).thenReturn(1L);

        GetUsersResponse resp = userService.getUsers(null, 0, 10);
        assertEquals(1, resp.getTotal());
        assertEquals("n@x.com", resp.getUsers().get(0).getEmail());
    }

    // -------- getUserDetails --------

    @Test
    void getUserDetails_success() {
        User u = User.builder().id(3L).email("a@b.com").enabled(true).build();
        when(userRepository.findById(3L)).thenReturn(Optional.of(u));
        when(userRoleService.getRoleNamesByUserId(3L)).thenReturn(List.of("ADMIN"));
        when(userGroupService.getGroupNamesByUserId(3L)).thenReturn(List.of("G1"));

        var resp = userService.getUserDetails(3L);
        assertEquals(3L, resp.getId());
        assertEquals("a@b.com", resp.getEmail());
        assertEquals(List.of("ADMIN"), resp.getRoles());
        assertEquals(List.of("G1"), resp.getGroups());
    }

    @Test
    void getUserDetails_invalidIdZeroOrNull_throws() {
        assertThrows(IllegalArgumentException.class, () -> userService.getUserDetails(0L));
        assertThrows(IllegalArgumentException.class, () -> userService.getUserDetails(null));
    }

    @Test
    void getUserDetails_notFound_throws() {
        when(userRepository.findById(9L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> userService.getUserDetails(9L));
    }

    // -------- changePassword --------

    private void mockAuthenticated(String email) {
        SecurityContext sc = mock(SecurityContext.class);
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(email);
        when(sc.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(sc);
    }

    @Test
    void changePassword_success() {
        mockAuthenticated("me@x.com");
        User u = User.builder().id(1L).email("me@x.com").password("old_hash").build();
        when(userRepository.findByEmail("me@x.com")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("old", "old_hash")).thenReturn(true);
        when(passwordEncoder.encode("newPass")).thenReturn("new_hash");

        userService.changePassword(new ChangePasswordRequest("old", "newPass"));
        verify(userRepository).save(argThat(saved -> "new_hash".equals(saved.getPassword())));
    }

    @Test
    void changePassword_invalidNewPassword_throws() {
        mockAuthenticated("me@x.com");
        assertThrows(IllegalArgumentException.class,
                () -> userService.changePassword(new ChangePasswordRequest("old", "123")));
    }

    @Test
    void changePassword_unauthenticated_authIsNull_throws() {
        SecurityContextHolder.clearContext();
        assertThrows(InvalidCredentialsException.class,
                () -> userService.changePassword(new ChangePasswordRequest("old", "123456")));
    }

    @Test
    void changePassword_unauthenticated_authNameNull_throws() {
        SecurityContext sc = mock(SecurityContext.class);
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(null);
        when(sc.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(sc);

        assertThrows(InvalidCredentialsException.class,
                () -> userService.changePassword(new ChangePasswordRequest("old", "newgood")));
    }

    @Test
    void changePassword_wrongOldPassword_throws() {
        mockAuthenticated("u@a.com");
        when(userRepository.findByEmail("u@a.com")).thenReturn(
                Optional.of(User.builder().id(1L).email("u@a.com").password("h").build()));
        when(passwordEncoder.matches("old", "h")).thenReturn(false);
        assertThrows(InvalidCredentialsException.class,
                () -> userService.changePassword(new ChangePasswordRequest("old", "123456")));
    }

    // -------- changeEmail --------

    @Test
    void changeEmail_success() {
        mockAuthenticated("me@x.com");
        User u = User.builder().id(1L).email("me@x.com").password("p").build();
        when(userRepository.findByEmail("me@x.com")).thenReturn(Optional.of(u));
        when(userRepository.findByEmail("you@x.com")).thenReturn(Optional.empty());

        userService.changeEmail(new UpdateEmailRequest("you@x.com"));
        verify(userRepository).save(argThat(saved -> "you@x.com".equals(saved.getEmail())));
    }

    @Test
    void changeEmail_invalidFormat_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> userService.changeEmail(new UpdateEmailRequest("bad")));
    }

    @Test
    void changeEmail_duplicate_throws() {
        when(userRepository.findByEmail("dup@x.com"))
                .thenReturn(Optional.of(User.builder().id(2L).email("dup@x.com").build()));
        assertThrows(EmailAlreadyUsedException.class,
                () -> userService.changeEmail(new UpdateEmailRequest("dup@x.com")));
    }

    @Test
    void changeEmail_unauthenticated_authIsNull_throws() {
        SecurityContextHolder.clearContext();
        assertThrows(InvalidCredentialsException.class,
                () -> userService.changeEmail(new UpdateEmailRequest("ok@x.com")));
    }

    @Test
    void changeEmail_unauthenticated_authNameNull_throws() {
        SecurityContext sc = mock(SecurityContext.class);
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(null);
        when(sc.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(sc);

        assertThrows(InvalidCredentialsException.class,
                () -> userService.changeEmail(new UpdateEmailRequest("ok@x.com")));
    }

    // -------- updateUserStatus --------

    @Test
    void updateUserStatus_success() {
        UpdateUserStatusRequest req = new UpdateUserStatusRequest();
        req.setUserIds(List.of(1L, 2L));
        req.setEnabled(true);

        when(userRepository.findAllById(List.of(1L, 2L))).thenReturn(
                List.of(User.builder().id(1L).enabled(false).build(),
                        User.builder().id(2L).enabled(false).build()));
        when(userRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        UpdateUserStatusResponse resp = userService.updateUserStatus(req);
        assertEquals("User status updated successfully", resp.getMessage());
        assertEquals(2, resp.getUpdatedCount());
    }

    @Test
    void updateUserStatus_enabledNull_throws() {
        UpdateUserStatusRequest req = new UpdateUserStatusRequest();
        req.setUserIds(List.of(1L));
        req.setEnabled(null);
        assertThrows(IllegalArgumentException.class, () -> userService.updateUserStatus(req));
    }

    @Test
    void updateUserStatus_emptyUsers_throws() {
        UpdateUserStatusRequest req = new UpdateUserStatusRequest();
        req.setUserIds(List.of());
        req.setEnabled(true);
        assertThrows(IllegalArgumentException.class, () -> userService.updateUserStatus(req));
    }

    @Test
    void updateUserStatus_noUsersFound_throws() {
        UpdateUserStatusRequest req = new UpdateUserStatusRequest();
        req.setUserIds(List.of(9L));
        req.setEnabled(true);
        when(userRepository.findAllById(List.of(9L))).thenReturn(List.of());
        assertThrows(ResourceNotFoundException.class, () -> userService.updateUserStatus(req));
    }

    // -------- assignRolesToUsers --------

    @Test
    void assignRolesToUsers_success() {
        AssignRolesRequest req = new AssignRolesRequest();
        req.setUserIds(List.of(1L));
        req.setRoleIds(List.of(2L));

        when(userRepository.findAllById(List.of(1L))).thenReturn(List.of(User.builder().id(1L).build()));
        when(roleService.getByIdsOrThrow(List.of(2L))).thenReturn(List.of(Role.builder().id(2L).build()));
        when(userRoleService.assignRolesToUsers(List.of(1L), List.of(2L))).thenReturn(1);

        AssignRolesResponse resp = userService.assignRolesToUsers(req);
        assertEquals(1, resp.getAssignedCount());
    }

    @Test
    void assignRolesToUsers_invalid_userIdsOrRoleIds_throws() {
        AssignRolesRequest emptyUsers = new AssignRolesRequest();
        emptyUsers.setUserIds(List.of());
        emptyUsers.setRoleIds(List.of(2L));
        assertThrows(IllegalArgumentException.class, () -> userService.assignRolesToUsers(emptyUsers));

        AssignRolesRequest nullUsers = new AssignRolesRequest();
        nullUsers.setUserIds(null);
        nullUsers.setRoleIds(List.of(2L));
        assertThrows(IllegalArgumentException.class, () -> userService.assignRolesToUsers(nullUsers));

        AssignRolesRequest emptyRoles = new AssignRolesRequest();
        emptyRoles.setUserIds(List.of(1L));
        emptyRoles.setRoleIds(List.of());
        assertThrows(IllegalArgumentException.class, () -> userService.assignRolesToUsers(emptyRoles));

        AssignRolesRequest nullRoles = new AssignRolesRequest();
        nullRoles.setUserIds(List.of(1L));
        nullRoles.setRoleIds(null);
        assertThrows(IllegalArgumentException.class, () -> userService.assignRolesToUsers(nullRoles));
    }

    // -------- deassignRolesFromUsers --------

    @Test
    void deassignRolesFromUsers_success() {
        DeassignRolesRequest req = new DeassignRolesRequest();
        req.setUserIds(List.of(1L));
        req.setRoleIds(List.of(2L));
        when(userRepository.findAllById(List.of(1L))).thenReturn(List.of(User.builder().id(1L).build()));
        when(roleService.getByIdsOrThrow(List.of(2L))).thenReturn(List.of(Role.builder().id(2L).build()));
        when(userRoleService.deassignRoles(anyList(), anyList()))
                .thenReturn(DeassignRolesResponse.builder().removedCount(1).message("ok").build());
        DeassignRolesResponse resp = userService.deassignRolesFromUsers(req);
        assertEquals(1, resp.getRemovedCount());
    }

    // -------- assign/deassign users to/from groups --------

    @Test
    void assignUsersToGroups_delegates() {
        AssignUsersToGroupsRequest req = new AssignUsersToGroupsRequest();
        when(userGroupService.assignUsersToGroups(req))
                .thenReturn(AssignUsersToGroupsResponse.builder().assignedCount(2).build());
        AssignUsersToGroupsResponse resp = userService.assignUsersToGroups(req);
        assertEquals(2, resp.getAssignedCount());
    }

    @Test
    void deassignUsersFromGroups_invalid_inputs_throws() {
        DeassignUsersFromGroupsRequest usersNull = new DeassignUsersFromGroupsRequest();
        usersNull.setUserIds(null);
        usersNull.setGroupIds(List.of(1L));
        assertThrows(IllegalArgumentException.class, () -> userService.deassignUsersFromGroups(usersNull));

        DeassignUsersFromGroupsRequest groupsEmpty = new DeassignUsersFromGroupsRequest();
        groupsEmpty.setUserIds(List.of(1L));
        groupsEmpty.setGroupIds(List.of());
        assertThrows(IllegalArgumentException.class, () -> userService.deassignUsersFromGroups(groupsEmpty));

        DeassignUsersFromGroupsRequest groupsNull = new DeassignUsersFromGroupsRequest();
        groupsNull.setUserIds(List.of(1L));
        groupsNull.setGroupIds(null);
        assertThrows(IllegalArgumentException.class, () -> userService.deassignUsersFromGroups(groupsNull));
    }

    @Test
    void deassignUsersFromGroups_success() {
        DeassignUsersFromGroupsRequest ok = new DeassignUsersFromGroupsRequest();
        ok.setUserIds(List.of(1L));
        ok.setGroupIds(List.of(2L));
        when(userRepository.findAllById(List.of(1L))).thenReturn(List.of(User.builder().id(1L).build()));
        when(userGroupService.deassignUsersFromGroups(ok))
                .thenReturn(DeassignUsersFromGroupsResponse.builder().removedCount(1).build());
        DeassignUsersFromGroupsResponse resp = userService.deassignUsersFromGroups(ok);
        assertEquals(1, resp.getRemovedCount());
    }

    // -------- deleteUsers --------

    @Test
    void deleteUsers_success() {
        DeleteUsersRequest req = new DeleteUsersRequest();
        req.setUserIds(List.of(1L, 2L));
        when(userRepository.findAllById(List.of(1L, 2L))).thenReturn(
                List.of(User.builder().id(1L).build(), User.builder().id(2L).build()));

        DeleteUsersResponse resp = userService.deleteUsers(req);
        assertEquals(2, resp.getDeletedCount());
        verify(userRoleService).deleteByUserIds(List.of(1L, 2L));
        verify(userGroupService).deleteByUserIds(List.of(1L, 2L));
        verify(userRepository).deleteAllById(List.of(1L, 2L));
    }

    @Test
    void deleteUsers_nullOrEmpty_throws_and_missingUsers_throws() {
        DeleteUsersRequest nullIds = new DeleteUsersRequest();
        nullIds.setUserIds(null);
        assertThrows(IllegalArgumentException.class, () -> userService.deleteUsers(nullIds));

        DeleteUsersRequest emptyIds = new DeleteUsersRequest();
        emptyIds.setUserIds(List.of());
        assertThrows(IllegalArgumentException.class, () -> userService.deleteUsers(emptyIds));

        DeleteUsersRequest notFound = new DeleteUsersRequest();
        notFound.setUserIds(List.of(9L));
        when(userRepository.findAllById(List.of(9L))).thenReturn(List.of());
        assertThrows(ResourceNotFoundException.class, () -> userService.deleteUsers(notFound));
    }

    // -------- helpers & misc --------

    @Test
    void helpers_getByIdsOrThrow_emailExists_save_getExistingIds_getSummaries() {
        when(userRepository.findAllById(List.of(1L))).thenReturn(List.of(User.builder().id(1L).build()));
        assertEquals(1, userService.getByIdsOrThrow(List.of(1L)).size());

        when(userRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(User.builder().id(1L).build()));
        assertThrows(ResourceNotFoundException.class, () -> userService.getByIdsOrThrow(List.of(1L, 2L)));

        when(userRepository.findByEmail("a@b.com"))
                .thenReturn(Optional.of(User.builder().id(7L).email("a@b.com").build()));
        assertTrue(userService.emailExists("a@b.com"));

        User toSave = User.builder().id(20L).email("x@y.com").build();
        when(userRepository.save(toSave)).thenReturn(toSave);
        assertEquals(toSave, userService.save(toSave));

        when(userRepository.findAllById(List.of(1L, 3L))).thenReturn(
                List.of(User.builder().id(1L).build(), User.builder().id(3L).build()));
        assertEquals(List.of(1L, 3L), userService.getExistingIds(List.of(1L, 3L)));

        when(userRepository.findAllById(List.of(5L))).thenReturn(
                List.of(User.builder().id(5L).email("e@x.com").enabled(true).build()));
        List<UserSummaryResponse> summaries = userService.getUserSummariesByIds(List.of(5L));
        assertEquals(1, summaries.size());
        assertEquals("e@x.com", summaries.get(0).getEmail());
    }

    @Test
    void getByEmailOrThrow_throws() {
        when(userRepository.findByEmail("missing@x.com")).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, () -> userService.getByEmailOrThrow("missing@x.com"));
    }

    // -------- updateCredentialsByAdmin --------

    @Test
    void adminUpdateCredentials_requestNull_throws() {
        assertThrows(IllegalArgumentException.class, () -> userService.updateCredentialsByAdmin(1L, null));
    }

    @Test
    void adminUpdateCredentials_success_bothEmailAndPasswordUpdated() {
        User db = User.builder().id(1L).email("old@x.com").password("old").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(db));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(passwordEncoder.encode("newpass")).thenReturn("enc");

        AdminUpdateCredentialsRequest both = new AdminUpdateCredentialsRequest();
        both.setEmail("new@x.com");
        both.setPassword("newpass");

        AdminUpdateCredentialsResponse resp = userService.updateCredentialsByAdmin(1L, both);
        assertEquals(1L, resp.getId());
        assertTrue(resp.isEmailUpdated());
        assertTrue(resp.isPasswordUpdated());
    }

    @Test
    void adminUpdateCredentials_emailEqualsExisting_ignoreCase_updates_noDuplicateCheck() {
        User db = User.builder().id(1L).email("old@x.com").password("p").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(db));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminUpdateCredentialsRequest same = new AdminUpdateCredentialsRequest();
        same.setEmail("OLD@x.com"); // equalsIgnoreCase

        AdminUpdateCredentialsResponse resp = userService.updateCredentialsByAdmin(1L, same);
        assertTrue(resp.isEmailUpdated());
        assertFalse(resp.isPasswordUpdated());
        verify(userRepository, never()).findByEmail(eq("OLD@x.com"));
    }

    @Test
    void adminUpdateCredentials_invalidEmailFormat_throws() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(User.builder().id(2L).email("a@b.com").build()));
        AdminUpdateCredentialsRequest bad = new AdminUpdateCredentialsRequest();
        bad.setEmail("bad");
        assertThrows(IllegalArgumentException.class, () -> userService.updateCredentialsByAdmin(2L, bad));
    }

    @Test
    void adminUpdateCredentials_duplicateEmail_throws() {
        when(userRepository.findById(3L)).thenReturn(Optional.of(User.builder().id(3L).email("self@x.com").build()));
        when(userRepository.findByEmail("dup@x.com"))
                .thenReturn(Optional.of(User.builder().id(9L).email("dup@x.com").build()));

        AdminUpdateCredentialsRequest dup = new AdminUpdateCredentialsRequest();
        dup.setEmail("dup@x.com");
        assertThrows(EmailAlreadyUsedException.class, () -> userService.updateCredentialsByAdmin(3L, dup));
    }

    @Test
    void adminUpdateCredentials_invalidPassword_throws() {
        when(userRepository.findById(4L)).thenReturn(Optional.of(User.builder().id(4L).email("e@x.com").build()));
        AdminUpdateCredentialsRequest badPwd = new AdminUpdateCredentialsRequest();
        badPwd.setPassword("123");
        assertThrows(IllegalArgumentException.class, () -> userService.updateCredentialsByAdmin(4L, badPwd));
    }

    @Test
    void adminUpdateCredentials_passwordOnly_updates() {
        User db = User.builder().id(7L).email("keep@x.com").password("old").build();
        when(userRepository.findById(7L)).thenReturn(Optional.of(db));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(passwordEncoder.encode("stronger")).thenReturn("enc2");

        AdminUpdateCredentialsRequest pwdOnly = new AdminUpdateCredentialsRequest();
        pwdOnly.setPassword("stronger");

        AdminUpdateCredentialsResponse resp = userService.updateCredentialsByAdmin(7L, pwdOnly);
        assertFalse(resp.isEmailUpdated());
        assertTrue(resp.isPasswordUpdated());
    }

    @Test
    void adminUpdateCredentials_spacesOnly_throwsAtLeastOneProvidedGuard() {
        when(userRepository.findById(5L)).thenReturn(Optional.of(User.builder().id(5L).email("e@x.com").build()));
        AdminUpdateCredentialsRequest spaces = new AdminUpdateCredentialsRequest();
        spaces.setEmail("   ");
        spaces.setPassword("   ");
        assertThrows(IllegalArgumentException.class, () -> userService.updateCredentialsByAdmin(5L, spaces));
    }

    @Test
    void adminUpdateCredentials_userNotFound_throws() {
        when(userRepository.findById(6L)).thenReturn(Optional.empty());
        AdminUpdateCredentialsRequest req = new AdminUpdateCredentialsRequest();
        req.setEmail("a@b.com");
        assertThrows(ResourceNotFoundException.class, () -> userService.updateCredentialsByAdmin(6L, req));
    }
}
