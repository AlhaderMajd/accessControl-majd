package com.example.accesscontrol.controller;

import com.example.accesscontrol.dto.common.MessageResponse;
import com.example.accesscontrol.dto.common.PageResponse;
import com.example.accesscontrol.dto.group.*;
import com.example.accesscontrol.service.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<CreateGroupsResponse> createGroups(@RequestBody @Valid List<CreateGroupRequest> body) {
        var resp = groupService.createGroups(body);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<PageResponse<GroupResponse>> getGroups(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        var resp = groupService.getGroups(q, page, size);
        return ResponseEntity.ok(resp);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{groupId}")
    public ResponseEntity<GroupDetailsResponse> getGroupDetails(@PathVariable Long groupId) {
        var resp = groupService.getGroupDetails(groupId);
        return ResponseEntity.ok(resp);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{groupId}")
    public ResponseEntity<UpdateGroupNameResponse> updateGroupName(
            @PathVariable Long groupId,
            @RequestBody @Valid UpdateGroupNameRequest body
    ) {
        var resp = groupService.updateGroupName(groupId, body);
        return ResponseEntity.ok(resp);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping
    public ResponseEntity<MessageResponse> deleteGroups(@RequestBody List<Long> groupIds) {
        var resp = groupService.deleteGroups(groupIds);
        return ResponseEntity.ok(resp);
    }
}
