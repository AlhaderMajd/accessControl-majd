package com.example.accesscontrol.controller;

import com.example.accesscontrol.dto.common.PageResponse;
import com.example.accesscontrol.dto.group.CreateGroupItem;
import com.example.accesscontrol.dto.group.CreateGroupsResponse;
import com.example.accesscontrol.dto.group.GroupDetailsResponse;
import com.example.accesscontrol.dto.group.GroupResponse;
import com.example.accesscontrol.service.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @PostMapping
    public ResponseEntity<CreateGroupsResponse> createGroups(@RequestBody @Valid List<CreateGroupItem> body) {
        CreateGroupsResponse resp = groupService.createGroups(body);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @GetMapping
    public ResponseEntity<PageResponse<GroupResponse>> getGroups(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        var resp = groupService.getGroups(q, page, size);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<GroupDetailsResponse> getGroupDetails(@PathVariable Long groupId) {
        var resp = groupService.getGroupDetails(groupId);
        return ResponseEntity.ok(resp);
    }

    @PutMapping("/{groupId}")
    public ResponseEntity<com.example.accesscontrol.dto.group.UpdateGroupNameResponse> updateGroupName(
            @PathVariable Long groupId,
            @RequestBody @jakarta.validation.Valid com.example.accesscontrol.dto.group.UpdateGroupNameRequest body
    ) {
        var resp = groupService.updateGroupName(groupId, body);
        return ResponseEntity.ok(resp);
    }

    @DeleteMapping
    public ResponseEntity<com.example.accesscontrol.dto.common.MessageResponse> deleteGroups(
            @RequestBody java.util.List<Long> groupIds
    ) {
        var resp = groupService.deleteGroups(groupIds);
        return ResponseEntity.ok(resp);
    }

}
