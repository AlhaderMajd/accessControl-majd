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

import java.util.List;
import java.util.Optional;

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
    @Mock private TypedQuery<UserSummaryResponse> dataQuery;
    @Mock private TypedQuery<Long> countQuery;

    @InjectMocks private UserService userService;

    @BeforeEach
    void setup() {
        userService.setEntityManager(em);
    }

    @AfterEach
    void clearCtx() {
        SecurityContextHolder.clearContext();
    }

    private void mockAuthenticated(String email) {
        SecurityContext sc = mock(SecurityContext.class);
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(email);
        when(sc.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(sc);
    }

    private void mockAuthenticatedNameNull() {
        SecurityContext sc = mock(SecurityContext.class);
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(null);
        when(sc.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(sc);
    }

    // ---------------- createUsers ----------------

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

        Role member = Role.builder().id(5L).name("MEMBER").build();
        when(roleService.getOrCreateRole("MEMBER")).thenReturn(member);
        when(userRoleService.assignRolesToUsers(List.of(10L), List.of(5L))).thenReturn(1);

        CreateUsersResponse resp = userService.createUsers(req);

        assertEquals(List.of(10L), resp.getCreatedUserIds());
        assertEquals(List.of("MEMBER"), resp.getAssignedRoles());
    }

    @Test
    void createUsers_invalidInputs_orDuplicates_throw() {
        CreateUsersRequest r1 = new CreateUsersRequest(); // null list
        r1.setUsers(null);
        assertThrows(IllegalArgumentException.class, () -> userService.createUsers(r1));

        CreateUsersRequest r2 = new CreateUsersRequest(); // empty
        r2.setUsers(List.of());
        assertThrows(IllegalArgumentException.class, () -> userService.createUsers(r2));

        CreateUserRequest badEmail = new CreateUserRequest();
        badEmail.setEmail("not-email");
        badEmail.setPassword("123456");
        CreateUsersRequest r3 = new CreateUsersRequest();
        r3.setUsers(List.of(badEmail));
        assertThrows(IllegalArgumentException.class, () -> userService.createUsers(r3));

        CreateUserRequest badPwd = new CreateUserRequest();
        badPwd.setEmail("ok@x.com");
        badPwd.setPassword("123");
        CreateUsersRequest r4 = new CreateUsersRequest();
        r4.setUsers(List.of(badPwd));
        assertThrows(IllegalArgumentException.class, () -> userService.createUsers(r4));

        CreateUserRequest ok = new CreateUserRequest();
        ok.setEmail("a@b.com");
        ok.setPassword("123456");
        CreateUsersRequest r5 = new CreateUsersRequest();
        r5.setUsers(List.of(ok));
        when(userRepository.findAllByEmailIn(List.of("a@b.com")))
                .thenReturn(List.of(User.builder().id(1L).email("a@b.com").build()));
        assertThrows(EmailAlreadyUsedException.class, () -> userService.createUsers(r5));
    }

    // ---------------- getUsers (JPQL -> DTO) ----------------

    @Test
    void getUsers_paged_success() {
        when(em.createQuery(anyString(), eq(UserSummaryResponse.class))).thenReturn(dataQuery);
        when(dataQuery.setParameter(eq("s"), any())).thenReturn(dataQuery);
        when(dataQuery.setFirstResult(anyInt())).thenReturn(dataQuery);
        when(dataQuery.setMaxResults(anyInt())).thenReturn(dataQuery);
        when(dataQuery.getResultList()).thenReturn(
                List.of(UserSummaryResponse.builder().id(2L).email("x@y.com").enabled(true).build())
        );

        when(em.createQuery(startsWith("SELECT COUNT(u)"), eq(Long.class))).thenReturn(countQuery);
        when(countQuery.setParameter(eq("s"), any())).thenReturn(countQuery);
        when(countQuery.getSingleResult()).thenReturn(1L);

        GetUsersResponse resp = userService.getUsers("x", 0, 10);
        assertEquals(1, resp.getTotal());
        assertEquals(0, resp.getPage());
        assertEquals("x@y.com", resp.getUsers().get(0).getEmail());
    }

    @Test
    void getUsers_nullSearch_ok() {
        when(em.createQuery(anyString(), eq(UserSummaryResponse.class))).thenReturn(dataQuery);
        when(dataQuery.setParameter(eq("s"), any())).thenReturn(dataQuery);
        when(dataQuery.setFirstResult(anyInt())).thenReturn(dataQuery);
        when(dataQuery.setMaxResults(anyInt())).thenReturn(dataQuery);
        when(dataQuery.getResultList()).thenReturn(
                List.of(UserSummaryResponse.builder().id(3L).email("z@z.com").enabled(true).build())
        );

        when(em.createQuery(startsWith("SELECT COUNT(u)"), eq(Long.class))).thenReturn(countQuery);
        when(countQuery.setParameter(eq("s"), any())).thenReturn(countQuery);
        when(countQuery.getSingleResult()).thenReturn(1L);

        GetUsersResponse resp = userService.getUsers(null, 1, 5);
        assertEquals(1, resp.getTotal());
        assertEquals(1, resp.getPage());
        assertEquals("z@z.com", resp.getUsers().get(0).getEmail());
    }

    // ---------------- getUserDetails ----------------

    @Test
    void getUserDetails_validates_and_returns() {
        assertThrows(IllegalArgumentException.class, () -> userService.getUserDetails(0L));
        assertThrows(IllegalArgumentException.class, () -> userService.getUserDetails(null));

        when(userRepository.findById(9L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> userService.getUserDetails(9L));

        User u = User.builder().id(3L).email("a@b.com").enabled(true).build();
        when(userRepository.findById(3L)).thenReturn(Optional.of(u));
        when(userRoleService.getRoleNamesByUserId(3L)).thenReturn(List.of("ADMIN"));
        when(userGroupService.getGroupNamesByUserId(3L)).thenReturn(List.of("G1"));

        UserResponse resp = userService.getUserDetails(3L);
        assertEquals(3L, resp.getId());
        assertEquals("a@b.com", resp.getEmail());
        assertEquals(List.of("ADMIN"), resp.getRoles());
        assertEquals(List.of("G1"), resp.getGroups());
    }

    // ---------------- changePassword ----------------

    @Test
    void changePassword_success() {
        mockAuthenticated("me@x.com");
        User db = User.builder().id(1L).email("me@x.com").password("old_hash").build();
        when(userRepository.findByEmail("me@x.com")).thenReturn(Optional.of(db));
        when(passwordEncoder.matches("old", "old_hash")).thenReturn(true);
        when(passwordEncoder.encode("newPass")).thenReturn("new_hash");
        when(passwordEncoder.matches("newPass", "old_hash")).thenReturn(false);

        userService.changePassword(new ChangePasswordRequest("old", "newPass"));

        verify(userRepository).save(argThat(saved -> "new_hash".equals(saved.getPassword())));
    }

    @Test
    void changePassword_invalidCases() {
        mockAuthenticated("me@x.com");
        assertThrows(IllegalArgumentException.class,
                () -> userService.changePassword(new ChangePasswordRequest("old", "123")));

        SecurityContextHolder.clearContext();
        assertThrows(InvalidCredentialsException.class,
                () -> userService.changePassword(new ChangePasswordRequest("old", "123456")));

        mockAuthenticatedNameNull();
        assertThrows(InvalidCredentialsException.class,
                () -> userService.changePassword(new ChangePasswordRequest("old", "123456")));

        mockAuthenticated("u@a.com");
        when(userRepository.findByEmail("u@a.com"))
                .thenReturn(Optional.of(User.builder().id(1L).email("u@a.com").password("h").build()));
        when(passwordEncoder.matches("old", "h")).thenReturn(false);
        assertThrows(InvalidCredentialsException.class,
                () -> userService.changePassword(new ChangePasswordRequest("old", "123456")));

        mockAuthenticated("same@x.com");
        when(userRepository.findByEmail("same@x.com"))
                .thenReturn(Optional.of(User.builder().id(2L).email("same@x.com").password("hash").build()));
        when(passwordEncoder.matches("old", "hash")).thenReturn(true);
        when(passwordEncoder.matches("newPass", "hash")).thenReturn(true); // equals current hash
        assertThrows(IllegalArgumentException.class,
                () -> userService.changePassword(new ChangePasswordRequest("old", "newPass")));
    }

    // ---------------- changeEmail ----------------

    @Test
    void changeEmail_success() {
        mockAuthenticated("me@x.com");
        User db = User.builder().id(1L).email("me@x.com").password("p").build();
        when(userRepository.findByEmail("me@x.com")).thenReturn(Optional.of(db));
        when(userRepository.existsByEmailIgnoreCase("you@x.com")).thenReturn(false);

        userService.changeEmail(new UpdateEmailRequest("you@x.com"));

        verify(userRepository).save(argThat(saved -> "you@x.com".equals(saved.getEmail())));
    }

    @Test
    void changeEmail_invalid_taken_sameIgnoringCase_or_unauthenticated() {
        mockAuthenticated("me@x.com");
        User me = User.builder().id(1L).email("me@x.com").build();
        when(userRepository.findByEmail("me@x.com")).thenReturn(Optional.of(me));

        assertThrows(IllegalArgumentException.class,
                () -> userService.changeEmail(new UpdateEmailRequest("bad")));

        // same email ignoring case -> service throws IllegalArgumentException
        assertThrows(IllegalArgumentException.class,
                () -> userService.changeEmail(new UpdateEmailRequest("ME@X.COM")));

        when(userRepository.existsByEmailIgnoreCase("dup@x.com")).thenReturn(true);
        assertThrows(EmailAlreadyUsedException.class,
                () -> userService.changeEmail(new UpdateEmailRequest("dup@x.com")));

        SecurityContextHolder.clearContext();
        assertThrows(InvalidCredentialsException.class,
                () -> userService.changeEmail(new UpdateEmailRequest("ok@x.com")));

        mockAuthenticatedNameNull();
        assertThrows(InvalidCredentialsException.class,
                () -> userService.changeEmail(new UpdateEmailRequest("ok2@x.com")));
    }

    // ---------------- updateUserStatus ----------------

    @Test
    void updateUserStatus_success_and_invalids() {
        UpdateUserStatusRequest req = new UpdateUserStatusRequest();
        req.setUserIds(List.of(1L, 2L));
        req.setEnabled(true);

        when(userRepository.findAllById(List.of(1L, 2L)))
                .thenReturn(List.of(
                        User.builder().id(1L).enabled(false).build(),
                        User.builder().id(2L).enabled(false).build()
                ));
        when(userRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        UpdateUserStatusResponse resp = userService.updateUserStatus(req);
        assertEquals("User status updated successfully", resp.getMessage());
        assertEquals(2, resp.getUpdatedCount());

        UpdateUserStatusRequest emptyIds = new UpdateUserStatusRequest();
        emptyIds.setUserIds(List.of());
        emptyIds.setEnabled(true);
        assertThrows(IllegalArgumentException.class, () -> userService.updateUserStatus(emptyIds));

        UpdateUserStatusRequest nullIds = new UpdateUserStatusRequest();
        nullIds.setUserIds(null);
        nullIds.setEnabled(true);
        assertThrows(IllegalArgumentException.class, () -> userService.updateUserStatus(nullIds));

        UpdateUserStatusRequest enabledNull = new UpdateUserStatusRequest();
        enabledNull.setUserIds(List.of(1L));
        enabledNull.setEnabled(null);
        assertThrows(IllegalArgumentException.class, () -> userService.updateUserStatus(enabledNull));

        UpdateUserStatusRequest notFound = new UpdateUserStatusRequest();
        notFound.setUserIds(List.of(9L));
        notFound.setEnabled(true);
        when(userRepository.findAllById(List.of(9L))).thenReturn(List.of());
        assertThrows(ResourceNotFoundException.class, () -> userService.updateUserStatus(notFound));
    }

    // ---------------- assign/deassign roles ----------------

    @Test
    void assignRolesToUsers_success_and_invalid() {
        AssignRolesRequest req = new AssignRolesRequest();
        req.setUserIds(List.of(1L));
        req.setRoleIds(List.of(2L));

        when(userRepository.findAllById(List.of(1L)))
                .thenReturn(List.of(User.builder().id(1L).build()));
        when(roleService.getByIdsOrThrow(List.of(2L)))
                .thenReturn(List.of(Role.builder().id(2L).build()));
        when(userRoleService.assignRolesToUsers(List.of(1L), List.of(2L))).thenReturn(1);

        AssignRolesResponse resp = userService.assignRolesToUsers(req);
        assertEquals(1, resp.getAssignedCount());

        AssignRolesRequest emptyUsers = new AssignRolesRequest();
        emptyUsers.setUserIds(List.of());
        emptyUsers.setRoleIds(List.of(2L));
        assertThrows(IllegalArgumentException.class, () -> userService.assignRolesToUsers(emptyUsers));

        AssignRolesRequest nullUsers = new AssignRolesRequest();
        nullUsers.setUserIds(null);
        nullUsers.setRoleIds(List.of(2L));
        assertThrows(IllegalArgumentException.class, () -> userService.assignRolesToUsers(nullUsers));

        AssignRolesRequest nullRoles = new AssignRolesRequest();
        nullRoles.setUserIds(List.of(1L));
        nullRoles.setRoleIds(null);
        assertThrows(IllegalArgumentException.class, () -> userService.assignRolesToUsers(nullRoles));

        AssignRolesRequest emptyRoles = new AssignRolesRequest();
        emptyRoles.setUserIds(List.of(1L));
        emptyRoles.setRoleIds(List.of());
        assertThrows(IllegalArgumentException.class, () -> userService.assignRolesToUsers(emptyRoles));
    }

    @Test
    void deassignRolesFromUsers_success() {
        DeassignRolesRequest req = new DeassignRolesRequest();
        req.setUserIds(List.of(1L));
        req.setRoleIds(List.of(2L));

        when(userRepository.findAllById(List.of(1L)))
                .thenReturn(List.of(User.builder().id(1L).build()));
        when(roleService.getByIdsOrThrow(List.of(2L)))
                .thenReturn(List.of(Role.builder().id(2L).build()));
        when(userRoleService.deassignRoles(anyList(), anyList()))
                .thenReturn(DeassignRolesResponse.builder().removedCount(1).message("ok").build());

        DeassignRolesResponse resp = userService.deassignRolesFromUsers(req);
        assertEquals(1, resp.getRemovedCount());
    }

    // ---------------- groups delegation ----------------

    @Test
    void assignUsersToGroups_delegates() {
        AssignUsersToGroupsRequest req = new AssignUsersToGroupsRequest();
        when(userGroupService.assignUsersToGroups(req))
                .thenReturn(AssignUsersToGroupsResponse.builder().assignedCount(2).build());
        AssignUsersToGroupsResponse resp = userService.assignUsersToGroups(req);
        assertEquals(2, resp.getAssignedCount());
    }

    @Test
    void deassignUsersFromGroups_invalid_or_success() {
        DeassignUsersFromGroupsRequest emptyUsers = new DeassignUsersFromGroupsRequest();
        emptyUsers.setUserIds(List.of());
        emptyUsers.setGroupIds(List.of(1L));
        assertThrows(IllegalArgumentException.class, () -> userService.deassignUsersFromGroups(emptyUsers));

        DeassignUsersFromGroupsRequest nullUsers = new DeassignUsersFromGroupsRequest();
        nullUsers.setUserIds(null);
        nullUsers.setGroupIds(List.of(1L));
        assertThrows(IllegalArgumentException.class, () -> userService.deassignUsersFromGroups(nullUsers));

        DeassignUsersFromGroupsRequest nullGroups = new DeassignUsersFromGroupsRequest();
        nullGroups.setUserIds(List.of(1L));
        nullGroups.setGroupIds(null);
        assertThrows(IllegalArgumentException.class, () -> userService.deassignUsersFromGroups(nullGroups));

        DeassignUsersFromGroupsRequest emptyGroups = new DeassignUsersFromGroupsRequest();
        emptyGroups.setUserIds(List.of(1L));
        emptyGroups.setGroupIds(List.of());
        assertThrows(IllegalArgumentException.class, () -> userService.deassignUsersFromGroups(emptyGroups));

        DeassignUsersFromGroupsRequest ok = new DeassignUsersFromGroupsRequest();
        ok.setUserIds(List.of(1L));
        ok.setGroupIds(List.of(2L));
        when(userRepository.findAllById(List.of(1L)))
                .thenReturn(List.of(User.builder().id(1L).build()));
        when(userGroupService.deassignUsersFromGroups(any()))
                .thenReturn(DeassignUsersFromGroupsResponse.builder().removedCount(1).build());

        DeassignUsersFromGroupsResponse resp = userService.deassignUsersFromGroups(ok);
        assertEquals(1, resp.getRemovedCount());
    }

    // ---------------- deleteUsers ----------------

    @Test
    void deleteUsers_success_and_errors() {
        DeleteUsersRequest req = new DeleteUsersRequest();
        req.setUserIds(List.of(1L, 2L));
        when(userRepository.findAllById(List.of(1L, 2L)))
                .thenReturn(List.of(User.builder().id(1L).build(), User.builder().id(2L).build()));

        DeleteUsersResponse resp = userService.deleteUsers(req);
        assertEquals(2, resp.getDeletedCount());
        verify(userRoleService).deleteByUserIds(List.of(1L, 2L));
        verify(userGroupService).deleteByUserIds(List.of(1L, 2L));
        verify(userRepository).deleteAllByIdInBatch(List.of(1L, 2L));

        DeleteUsersRequest bad = new DeleteUsersRequest();
        bad.setUserIds(List.of());
        assertThrows(IllegalArgumentException.class, () -> userService.deleteUsers(bad));

        DeleteUsersRequest notFound = new DeleteUsersRequest();
        notFound.setUserIds(List.of(9L));
        when(userRepository.findAllById(List.of(9L))).thenReturn(List.of());
        assertThrows(ResourceNotFoundException.class, () -> userService.deleteUsers(notFound));
    }

    // ---------------- helpers ----------------

    @Test
    void helpers_getByIdsOrThrow_emailExists_save_getExistingIds_getUserSummariesByIds_getByEmailOrThrow() {
        when(userRepository.findAllById(List.of(1L)))
                .thenReturn(List.of(User.builder().id(1L).build()));
        assertEquals(1, userService.getByIdsOrThrow(List.of(1L)).size());

        when(userRepository.findAllById(List.of(1L, 2L)))
                .thenReturn(List.of(User.builder().id(1L).build()));
        assertThrows(ResourceNotFoundException.class, () -> userService.getByIdsOrThrow(List.of(1L, 2L)));

        when(userRepository.findByEmail("a@b.com"))
                .thenReturn(Optional.of(User.builder().id(7L).email("a@b.com").build()));
        assertTrue(userService.emailExists("a@b.com"));

        User toSave = User.builder().id(20L).email("x@y.com").build();
        when(userRepository.save(toSave)).thenReturn(toSave);
        assertEquals(toSave, userService.save(toSave));

        when(userRepository.findAllById(List.of(1L, 3L)))
                .thenReturn(List.of(User.builder().id(1L).build(), User.builder().id(3L).build()));
        assertEquals(List.of(1L, 3L), userService.getExistingIds(List.of(1L, 3L)));

        when(userRepository.findAllById(List.of(5L)))
                .thenReturn(List.of(User.builder().id(5L).email("e@x.com").enabled(true).build()));
        List<UserSummaryResponse> summaries = userService.getUserSummariesByIds(List.of(5L));
        assertEquals(1, summaries.size());
        assertEquals("e@x.com", summaries.get(0).getEmail());

        when(userRepository.findByEmail("missing@x.com")).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, () -> userService.getByEmailOrThrow("missing@x.com"));
    }
}
