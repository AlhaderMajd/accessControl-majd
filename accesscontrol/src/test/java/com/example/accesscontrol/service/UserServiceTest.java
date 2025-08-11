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

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserRoleService userRoleService;
    @Mock
    private UserGroupService userGroupService;
    @Mock
    private RoleService roleService;
    @Mock
    private EntityManager em;
    @Mock
    private TypedQuery<User> typedQuery;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void wireEntityManager() {
        userService.setEntityManager(em);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

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
    void createUsers_invalidInputs_throw() {
        CreateUsersRequest req = new CreateUsersRequest();
        req.setUsers(List.of());
        assertThrows(IllegalArgumentException.class, () -> userService.createUsers(req));

        CreateUserRequest bad = new CreateUserRequest();
        bad.setEmail("not-email");
        bad.setPassword("123");
        CreateUsersRequest req2 = new CreateUsersRequest();
        req2.setUsers(List.of(bad));
        assertThrows(IllegalArgumentException.class, () -> userService.createUsers(req2));
    }

    @Test
    void createUsers_duplicateEmails_throw() {
        CreateUserRequest u1 = new CreateUserRequest();
        u1.setEmail("a@b.com");
        u1.setPassword("secret1");
        CreateUsersRequest req = new CreateUsersRequest();
        req.setUsers(List.of(u1));
        when(userRepository.findAllByEmailIn(List.of("a@b.com"))).thenReturn(List.of(User.builder().id(1L).email("a@b.com").build()));
        assertThrows(EmailAlreadyUsedException.class, () -> userService.createUsers(req));
    }

    @Test
    void getUsers_paged_success() {
        when(em.createQuery(anyString(), eq(User.class))).thenReturn(typedQuery);
        when(typedQuery.setParameter(eq("s"), any())).thenReturn(typedQuery);
        when(typedQuery.setFirstResult(anyInt())).thenReturn(typedQuery);
        when(typedQuery.setMaxResults(anyInt())).thenReturn(typedQuery);
        when(typedQuery.getResultList()).thenReturn(List.of(User.builder().id(2L).email("x@y.com").enabled(true).build()));
        // count query
        @SuppressWarnings("unchecked") jakarta.persistence.TypedQuery<Long> countQ = mock(jakarta.persistence.TypedQuery.class);
        when(em.createQuery(startsWith("SELECT COUNT(u)"), eq(Long.class))).thenReturn(countQ);
        when(countQ.setParameter(eq("s"), any())).thenReturn(countQ);
        when(countQ.getSingleResult()).thenReturn(1L);

        GetUsersResponse resp = userService.getUsers("x", 0, 10);
        assertEquals(1, resp.getTotal());
        assertEquals(0, resp.getPage());
        assertEquals("x@y.com", resp.getUsers().get(0).getEmail());
    }

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
    void getUserDetails_invalidOrMissing_throws() {
        assertThrows(IllegalArgumentException.class, () -> userService.getUserDetails(0L));
        when(userRepository.findById(9L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> userService.getUserDetails(9L));
    }

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
    void changePassword_invalidOrUnauthenticated_throws() {
        assertThrows(IllegalArgumentException.class, () -> userService.changePassword(new ChangePasswordRequest("a", "123")));
        // unauthenticated
        SecurityContextHolder.clearContext();
        assertThrows(InvalidCredentialsException.class, () -> userService.changePassword(new ChangePasswordRequest("a", "123456")));

        // wrong old password
        mockAuthenticated("u@a.com");
        when(userRepository.findByEmail("u@a.com")).thenReturn(Optional.of(User.builder().id(1L).email("u@a.com").password("h").build()));
        when(passwordEncoder.matches("old", "h")).thenReturn(false);
        assertThrows(InvalidCredentialsException.class, () -> userService.changePassword(new ChangePasswordRequest("old", "123456")));
    }

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
    void changeEmail_invalidOrTakenOrUnauthenticated_throws() {
        assertThrows(IllegalArgumentException.class, () -> userService.changeEmail(new UpdateEmailRequest("bad")));

        when(userRepository.findByEmail("dup@x.com")).thenReturn(Optional.of(User.builder().id(2L).email("dup@x.com").build()));
        assertThrows(EmailAlreadyUsedException.class, () -> userService.changeEmail(new UpdateEmailRequest("dup@x.com")));

        SecurityContextHolder.clearContext();
        assertThrows(InvalidCredentialsException.class, () -> userService.changeEmail(new UpdateEmailRequest("ok@x.com")));
    }

    @Test
    void updateUserStatus_success_and_invalid() {
        UpdateUserStatusRequest req = new UpdateUserStatusRequest();
        req.setUserIds(List.of(1L, 2L));
        req.setEnabled(true);

        when(userRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(User.builder().id(1L).enabled(false).build(), User.builder().id(2L).enabled(false).build()));
        when(userRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        UpdateUserStatusResponse resp = userService.updateUserStatus(req);
        assertEquals("User status updated successfully", resp.getMessage());
        assertEquals(2, resp.getUpdatedCount());

        UpdateUserStatusRequest bad = new UpdateUserStatusRequest();
        bad.setUserIds(List.of());
        assertThrows(IllegalArgumentException.class, () -> userService.updateUserStatus(bad));

        UpdateUserStatusRequest noUsers = new UpdateUserStatusRequest();
        noUsers.setUserIds(List.of(9L));
        noUsers.setEnabled(true);
        when(userRepository.findAllById(List.of(9L))).thenReturn(List.of());
        assertThrows(ResourceNotFoundException.class, () -> userService.updateUserStatus(noUsers));
    }

    @Test
    void assignRolesToUsers_success_and_invalid() {
        AssignRolesRequest req = new AssignRolesRequest();
        req.setUserIds(List.of(1L));
        req.setRoleIds(List.of(2L));

        when(userRepository.findAllById(List.of(1L))).thenReturn(List.of(User.builder().id(1L).build()));
        when(roleService.getByIdsOrThrow(List.of(2L))).thenReturn(List.of(Role.builder().id(2L).build()));
        when(userRoleService.assignRolesToUsers(List.of(1L), List.of(2L))).thenReturn(1);

        AssignRolesResponse resp = userService.assignRolesToUsers(req);
        assertEquals(1, resp.getAssignedCount());

        AssignRolesRequest bad = new AssignRolesRequest();
        bad.setUserIds(List.of());
        bad.setRoleIds(List.of(2L));
        assertThrows(IllegalArgumentException.class, () -> userService.assignRolesToUsers(bad));
    }

    @Test
    void deassignRolesFromUsers_success() {
        DeassignRolesRequest req = new DeassignRolesRequest();
        req.setUserIds(List.of(1L));
        req.setRoleIds(List.of(2L));
        when(userRepository.findAllById(List.of(1L))).thenReturn(List.of(User.builder().id(1L).build()));
        when(roleService.getByIdsOrThrow(List.of(2L))).thenReturn(List.of(Role.builder().id(2L).build()));
        when(userRoleService.deassignRoles(anyList(), anyList())).thenReturn(DeassignRolesResponse.builder().removedCount(1).message("ok").build());
        DeassignRolesResponse resp = userService.deassignRolesFromUsers(req);
        assertEquals(1, resp.getRemovedCount());
    }

    @Test
    void assignUsersToGroups_delegates() {
        AssignUsersToGroupsRequest req = new AssignUsersToGroupsRequest();
        when(userGroupService.assignUsersToGroups(req)).thenReturn(AssignUsersToGroupsResponse.builder().assignedCount(2).build());
        AssignUsersToGroupsResponse resp = userService.assignUsersToGroups(req);
        assertEquals(2, resp.getAssignedCount());
    }

    @Test
    void deassignUsersFromGroups_invalidOrSuccess() {
        DeassignUsersFromGroupsRequest bad = new DeassignUsersFromGroupsRequest();
        bad.setUserIds(List.of());
        bad.setGroupIds(List.of(1L));
        assertThrows(IllegalArgumentException.class, () -> userService.deassignUsersFromGroups(bad));

        DeassignUsersFromGroupsRequest ok = new DeassignUsersFromGroupsRequest();
        ok.setUserIds(List.of(1L));
        ok.setGroupIds(List.of(2L));
        when(userRepository.findAllById(List.of(1L))).thenReturn(List.of(User.builder().id(1L).build()));
        when(userGroupService.deassignUsersFromGroups(ok)).thenReturn(DeassignUsersFromGroupsResponse.builder().removedCount(1).build());
        DeassignUsersFromGroupsResponse resp = userService.deassignUsersFromGroups(ok);
        assertEquals(1, resp.getRemovedCount());
    }

    @Test
    void deleteUsers_success_and_errors() {
        DeleteUsersRequest req = new DeleteUsersRequest();
        req.setUserIds(List.of(1L, 2L));
        when(userRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(User.builder().id(1L).build(), User.builder().id(2L).build()));

        DeleteUsersResponse resp = userService.deleteUsers(req);
        assertEquals(2, resp.getDeletedCount());
        verify(userRoleService).deleteByUserIds(List.of(1L, 2L));
        verify(userGroupService).deleteByUserIds(List.of(1L, 2L));
        verify(userRepository).deleteAllById(List.of(1L, 2L));

        DeleteUsersRequest bad = new DeleteUsersRequest();
        bad.setUserIds(List.of());
        assertThrows(IllegalArgumentException.class, () -> userService.deleteUsers(bad));

        DeleteUsersRequest notFound = new DeleteUsersRequest();
        notFound.setUserIds(List.of(9L));
        when(userRepository.findAllById(List.of(9L))).thenReturn(List.of());
        assertThrows(ResourceNotFoundException.class, () -> userService.deleteUsers(notFound));
    }

    @Test
    void helpers_getByIdsOrThrow_and_emailExists_and_save_and_getExistingIds_and_getSummaries() {
        when(userRepository.findAllById(List.of(1L))).thenReturn(List.of(User.builder().id(1L).build()));
        assertEquals(1, userService.getByIdsOrThrow(List.of(1L)).size());

        when(userRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(User.builder().id(1L).build()));
        assertThrows(ResourceNotFoundException.class, () -> userService.getByIdsOrThrow(List.of(1L, 2L)));

        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(User.builder().id(7L).email("a@b.com").build()));
        assertTrue(userService.emailExists("a@b.com"));

        User toSave = User.builder().id(20L).email("x@y.com").build();
        when(userRepository.save(toSave)).thenReturn(toSave);
        assertEquals(toSave, userService.save(toSave));

        when(userRepository.findAllById(List.of(1L, 3L))).thenReturn(List.of(User.builder().id(1L).build(), User.builder().id(3L).build()));
        assertEquals(List.of(1L, 3L), userService.getExistingIds(List.of(1L, 3L)));

        when(userRepository.findAllById(List.of(5L))).thenReturn(List.of(User.builder().id(5L).email("e@x.com").enabled(true).build()));
        List<UserSummaryResponse> summaries = userService.getUserSummariesByIds(List.of(5L));
        assertEquals(1, summaries.size());
        assertEquals("e@x.com", summaries.get(0).getEmail());
    }

    @Test
    void getByEmailOrThrow_throws() {
        when(userRepository.findByEmail("missing@x.com")).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, () -> userService.getByEmailOrThrow("missing@x.com"));
    }

    @Test
    void adminUpdateCredentials_success_emailAndPassword_and_errors() {
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

        // invalid request (nothing provided)
        AdminUpdateCredentialsRequest empty = new AdminUpdateCredentialsRequest();
        assertThrows(IllegalArgumentException.class, () -> userService.updateCredentialsByAdmin(1L, empty));

        // invalid email format
        when(userRepository.findById(2L)).thenReturn(Optional.of(User.builder().id(2L).email("a@b.com").build()));
        AdminUpdateCredentialsRequest badEmail = new AdminUpdateCredentialsRequest();
        badEmail.setEmail("bad");
        assertThrows(IllegalArgumentException.class, () -> userService.updateCredentialsByAdmin(2L, badEmail));

        // duplicate email
        when(userRepository.findById(3L)).thenReturn(Optional.of(User.builder().id(3L).email("self@x.com").build()));
        when(userRepository.findByEmail("dup@x.com")).thenReturn(Optional.of(User.builder().id(9L).email("dup@x.com").build()));
        AdminUpdateCredentialsRequest dup = new AdminUpdateCredentialsRequest();
        dup.setEmail("dup@x.com");
        assertThrows(EmailAlreadyUsedException.class, () -> userService.updateCredentialsByAdmin(3L, dup));

        // invalid password
        when(userRepository.findById(4L)).thenReturn(Optional.of(User.builder().id(4L).email("e@x.com").build()));
        AdminUpdateCredentialsRequest badPwd = new AdminUpdateCredentialsRequest();
        badPwd.setPassword("123");
        assertThrows(IllegalArgumentException.class, () -> userService.updateCredentialsByAdmin(4L, badPwd));

        // nothing to update (after trimming/validation both not set) -> by code, already guarded, but we hit the late guard too
        when(userRepository.findById(5L)).thenReturn(Optional.of(User.builder().id(5L).email("e@x.com").build()));
        AdminUpdateCredentialsRequest spaces = new AdminUpdateCredentialsRequest();
        spaces.setEmail("   ");
        spaces.setPassword("   ");
        assertThrows(IllegalArgumentException.class, () -> userService.updateCredentialsByAdmin(5L, spaces));

        // user not found
        when(userRepository.findById(6L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> userService.updateCredentialsByAdmin(6L, both));
    }
}
