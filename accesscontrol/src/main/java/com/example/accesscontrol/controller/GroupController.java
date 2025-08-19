package com.example.accesscontrol.controller;

import com.example.accesscontrol.config.logs;
import com.example.accesscontrol.dto.common.PageResponse;
import com.example.accesscontrol.dto.common.MessageResponse;
import com.example.accesscontrol.dto.group.*;
import com.example.accesscontrol.service.GroupService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@Slf4j
@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;
    private final logs logs;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<CreateGroupsResponse> createGroups(@RequestBody @Valid List<CreateGroupRequest> body) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String actor = auth == null ? "unknown" : auth.getName();
        log.info("groups.create request actor={} count={}", logs.mask(actor), body == null ? 0 : body.size());
        var resp = groupService.createGroups(body);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<PageResponse<GroupResponse>> getGroups(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {

        var auth = SecurityContextHolder.getContext().getAuthentication();
        String actor = auth == null ? "unknown" : auth.getName();
        log.info("groups.list request actor={} page={} size={} q_len={}",
                logs.mask(actor), page, size, q == null ? 0 : q.length());

        var resp = groupService.getGroups(q, page, size);
        return ResponseEntity.ok(resp);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{groupId}")
    public ResponseEntity<GroupDetailsResponse> getGroupDetails(@PathVariable @Min(1) Long groupId) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String actor = auth == null ? "unknown" : auth.getName();
        log.info("groups.details request actor={} groupId={}", logs.mask(actor), groupId);
        var resp = groupService.getGroupDetails(groupId);
        return ResponseEntity.ok(resp);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{groupId}")
    public ResponseEntity<UpdateGroupNameResponse> updateGroupName(
            @PathVariable @Min(1) Long groupId,
            @RequestBody @Valid UpdateGroupNameRequest body) {

        var auth = SecurityContextHolder.getContext().getAuthentication();
        String actor = auth == null ? "unknown" : auth.getName();
        log.info("groups.update_name request actor={} groupId={} new_len={}",
                logs.mask(actor), groupId, body.getName() == null ? 0 : body.getName().length());

        var resp = groupService.updateGroupName(groupId, body);
        return ResponseEntity.ok(resp);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping
    public ResponseEntity<MessageResponse> deleteGroups(@RequestBody List<Long> groupIds) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String actor = auth == null ? "unknown" : auth.getName();
        log.info("groups.delete request actor={} count={}", logs.mask(actor), groupIds == null ? 0 : groupIds.size());
        var resp = groupService.deleteGroups(groupIds);
        return ResponseEntity.ok(resp);
    }
}
