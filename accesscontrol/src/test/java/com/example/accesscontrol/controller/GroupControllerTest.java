package com.example.accesscontrol.controller;

import com.example.accesscontrol.dto.common.MessageResponse;
import com.example.accesscontrol.dto.common.PageResponse;
import com.example.accesscontrol.dto.group.*;
import com.example.accesscontrol.dto.role.RoleResponse;
import com.example.accesscontrol.dto.user.getUsers.UserSummaryResponse;
import com.example.accesscontrol.exception.*;
import com.example.accesscontrol.service.GroupService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(GroupController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class GroupControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean
    private GroupService groupService;

    private String json(Object o) throws Exception {
        return objectMapper.writeValueAsString(o);
    }

    @Nested
    @DisplayName("POST /api/groups (create groups)")
    class CreateGroupsTests {

        @Test
        @DisplayName("201 Created on valid list")
        void create_ok_201() throws Exception {
            var body = List.of(
                    new CreateGroupRequest("Engineering"),
                    new CreateGroupRequest("QA")
            );
            var resp = CreateGroupsResponse.builder()
                    .message("Groups created successfully")
                    .createdCount(2)
                    .items(List.of(
                            GroupResponse.builder().id(10L).name("Engineering").build(),
                            GroupResponse.builder().id(11L).name("QA").build()
                    ))
                    .build();

            when(groupService.createGroups(anyList())).thenReturn(resp);

            mockMvc.perform(post("/api/groups")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(body)))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.createdCount").value(2))
                    .andExpect(jsonPath("$.items[0].name").value("Engineering"));

            ArgumentCaptor<List<CreateGroupRequest>> cap = ArgumentCaptor.forClass(List.class);
            verify(groupService).createGroups(cap.capture());
            assertThat(cap.getValue()).hasSize(2);
        }

        @Test
        @DisplayName("400 Bad Request when body has invalid item (@NotBlank)")
        void create_validation_400() throws Exception {
            var body = List.of(new CreateGroupRequest(""), new CreateGroupRequest("QA")); // blank name

            mockMvc.perform(post("/api/groups")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(body)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(groupService);
        }

        @Test
        @DisplayName("400 Bad Request when service rejects input (IllegalArgumentException)")
        void create_service_illegalArg_400() throws Exception {
            var body = List.of(new CreateGroupRequest("Sales"));
            when(groupService.createGroups(anyList()))
                    .thenThrow(new IllegalArgumentException("Group names are required"));

            mockMvc.perform(post("/api/groups")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(body)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Group names are required"));
        }

        @Test
        @DisplayName("500 Internal Server Error on unexpected service error")
        void create_unexpected_500() throws Exception {
            var body = List.of(new CreateGroupRequest("Ops"));
            when(groupService.createGroups(anyList()))
                    .thenThrow(new RuntimeException("boom"));

            mockMvc.perform(post("/api/groups")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(body)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.error", containsString("Something went wrong")));
        }
    }

    @Nested
    @DisplayName("GET /api/groups (list with search & pagination)")
    class ListGroupsTests {

        @Test
        @DisplayName("200 OK with page payload")
        void list_ok_200() throws Exception {
            var pageResp = PageResponse.<GroupResponse>builder()
                    .items(List.of(GroupResponse.builder().id(1L).name("Engineering").build()))
                    .page(0)
                    .size(1)
                    .total(2L)
                    .build();

            when(groupService.getGroups(eq("eng"), eq(0), eq(1))).thenReturn(pageResp);

            mockMvc.perform(get("/api/groups")
                            .param("q", "eng")
                            .param("page", "0")
                            .param("size", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items[0].name").value("Engineering"))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(1))
                    .andExpect(jsonPath("$.total").value(2));

            verify(groupService).getGroups("eng", 0, 1);
        }

        @Test
        @DisplayName("400 Bad Request on invalid page/size (validation on controller params)")
        void list_badPagination_400() throws Exception {
            mockMvc.perform(get("/api/groups")
                            .param("q", "")
                            .param("page", "-1")
                            .param("size", "0"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(groupService);
        }
    }

    @Nested
    @DisplayName("GET /api/groups/{groupId} (details)")
    class DetailsTests {

        @Test
        @DisplayName("200 OK returns details with users & roles")
        void details_ok_200() throws Exception {
            var resp = GroupDetailsResponse.builder()
                    .id(10L)
                    .name("Engineering")
                    .users(List.of(
                            UserSummaryResponse.builder().id(1L).email("a@x").enabled(true).build()
                    ))
                    .roles(List.of(
                            RoleResponse.builder().id(100L).name("ADMIN").build()
                    ))
                    .build();

            when(groupService.getGroupDetails(10L)).thenReturn(resp);

            mockMvc.perform(get("/api/groups/{groupId}", 10))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(10))
                    .andExpect(jsonPath("$.name").value("Engineering"))
                    .andExpect(jsonPath("$.users[0].email").value("a@x"))
                    .andExpect(jsonPath("$.roles[0].name").value("ADMIN"));

            verify(groupService).getGroupDetails(10L);
        }

        @Test
        @DisplayName("400 Bad Request when groupId < 1")
        void details_badId_400() throws Exception {
            mockMvc.perform(get("/api/groups/{groupId}", 0))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(groupService);
        }

        @Test
        @DisplayName("404 Not Found when service throws ResourceNotFoundException")
        void details_notFound_404() throws Exception {
            when(groupService.getGroupDetails(999L)).thenThrow(new ResourceNotFoundException("Group not found"));

            mockMvc.perform(get("/api/groups/{groupId}", 999))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("Group not found"));
        }
    }

    @Nested
    @DisplayName("PUT /api/groups/{groupId} (update name)")
    class UpdateNameTests {

        @Test
        @DisplayName("200 OK on valid update")
        void update_ok_200() throws Exception {
            var body = new UpdateGroupNameRequest("Quality");
            var resp = UpdateGroupNameResponse.builder()
                    .message("Group name updated successfully")
                    .id(11L)
                    .oldName("QA")
                    .newName("Quality")
                    .build();

            when(groupService.updateGroupName(eq(11L), any(UpdateGroupNameRequest.class))).thenReturn(resp);

            mockMvc.perform(put("/api/groups/{groupId}", 11)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Group name updated successfully"))
                    .andExpect(jsonPath("$.newName").value("Quality"));

            ArgumentCaptor<UpdateGroupNameRequest> cap = ArgumentCaptor.forClass(UpdateGroupNameRequest.class);
            verify(groupService).updateGroupName(eq(11L), cap.capture());
            assertThat(cap.getValue().getName()).isEqualTo("Quality");
        }

        @Test
        @DisplayName("400 Bad Request when groupId < 1")
        void update_badId_400() throws Exception {
            var body = new UpdateGroupNameRequest("X");

            mockMvc.perform(put("/api/groups/{groupId}", 0)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(body)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(groupService);
        }

        @Test
        @DisplayName("400 Bad Request when body invalid (@NotBlank)")
        void update_badBody_400() throws Exception {
            var body = new UpdateGroupNameRequest(""); // invalid

            mockMvc.perform(put("/api/groups/{groupId}", 12)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(body)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(groupService);
        }

        @Test
        @DisplayName("404 Not Found when service throws ResourceNotFoundException")
        void update_notFound_404() throws Exception {
            var body = new UpdateGroupNameRequest("NewName");
            when(groupService.updateGroupName(eq(13L), any(UpdateGroupNameRequest.class)))
                    .thenThrow(new ResourceNotFoundException("Group not found"));

            mockMvc.perform(put("/api/groups/{groupId}", 13)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(body)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("Group not found"));
        }

        @Test
        @DisplayName("409 Conflict when service throws DuplicateResourceException")
        void update_duplicate_409() throws Exception {
            var body = new UpdateGroupNameRequest("Engineering");
            when(groupService.updateGroupName(eq(14L), any(UpdateGroupNameRequest.class)))
                    .thenThrow(new DuplicateResourceException("Group name already exists"));

            mockMvc.perform(put("/api/groups/{groupId}", 14)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(body)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.error").value("Group name already exists"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/groups (batch)")
    class DeleteGroupsTests {

        @Test
        @DisplayName("200 OK on valid delete")
        void delete_ok_200() throws Exception {
            var req = List.of(10L, 11L);
            var resp = MessageResponse.builder().message("Group(s) deleted successfully").build();

            when(groupService.deleteGroups(eq(req))).thenReturn(resp);

            mockMvc.perform(delete("/api/groups")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Group(s) deleted successfully"));

            verify(groupService).deleteGroups(eq(req));
        }

        @Test
        @DisplayName("400 Bad Request when service throws IllegalArgumentException")
        void delete_illegalArg_400() throws Exception {
            var req = List.of();
            when(groupService.deleteGroups(anyList()))
                    .thenThrow(new IllegalArgumentException("Invalid or empty group IDs list"));

            mockMvc.perform(delete("/api/groups")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Invalid or empty group IDs list"));
        }

        @Test
        @DisplayName("404 Not Found when service throws ResourceNotFoundException")
        void delete_notFound_404() throws Exception {
            var req = List.of(999L);
            when(groupService.deleteGroups(eq(req)))
                    .thenThrow(new ResourceNotFoundException("No matching groups found"));

            mockMvc.perform(delete("/api/groups")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("No matching groups found"));
        }
    }
}
