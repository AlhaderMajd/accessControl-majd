package com.example.accesscontrol.controller;

import com.example.accesscontrol.dto.common.MessageResponse;
import com.example.accesscontrol.dto.common.PageResponse;
import com.example.accesscontrol.dto.group.*;
import com.example.accesscontrol.service.GroupService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GroupController.class)
@AutoConfigureMockMvc(addFilters = false)
class GroupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GroupService groupService;

    @Test
    void getGroups_returnsPage() throws Exception {
        GroupResponse g = GroupResponse.builder().id(10L).name("Team A").build();
        PageResponse<GroupResponse> page = PageResponse.<GroupResponse>builder()
                .items(List.of(g)).page(0).size(10).total(1).build();
        when(groupService.getGroups("", 0, 10)).thenReturn(page);

        mockMvc.perform(get("/api/groups").param("q", "").param("page", "0").param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].name").value("Team A"));
    }

    @Test
    void createGroups_returns201() throws Exception {
        GroupResponse g = GroupResponse.builder().id(1L).name("A").build();
                CreateGroupsResponse resp = CreateGroupsResponse.builder().items(List.of(g)).createdCount(1).message("ok").build();
        when(groupService.createGroups(any())).thenReturn(resp);
        String body = "[{\"name\":\"A\"}]";
        mockMvc.perform(post("/api/groups").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("ok"))
                .andExpect(jsonPath("$.createdCount").value(1))
                .andExpect(jsonPath("$.items", hasSize(1)));
    }

    @Test
    void getGroupDetails_returnsOk() throws Exception {
        GroupDetailsResponse resp = GroupDetailsResponse.builder().id(1L).name("A").roles(List.of()).users(List.of()).build();
        when(groupService.getGroupDetails(1L)).thenReturn(resp);
        mockMvc.perform(get("/api/groups/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("A"));
    }

    @Test
    void updateGroupName_returnsOk() throws Exception {
        UpdateGroupNameResponse resp = UpdateGroupNameResponse.builder().message("updated").build();
        when(groupService.updateGroupName(eq(2L), any(UpdateGroupNameRequest.class))).thenReturn(resp);
        String body = "{\"name\":\"B\"}";
        mockMvc.perform(put("/api/groups/2").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("updated"));
    }

    @Test
    void deleteGroups_returnsOk() throws Exception {
        MessageResponse resp = MessageResponse.builder().message("deleted").build();
        when(groupService.deleteGroups(any())).thenReturn(resp);
        String body = "[1,2]";
        mockMvc.perform(delete("/api/groups").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("deleted"));
    }
}
