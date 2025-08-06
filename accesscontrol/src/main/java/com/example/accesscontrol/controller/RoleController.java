package com.example.accesscontrol.controller;

import com.example.accesscontrol.dto.CreateRoleRequest;
import com.example.accesscontrol.dto.CreateRoleResponse;
import com.example.accesscontrol.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @PostMapping
    public ResponseEntity<CreateRoleResponse> createRoles(@RequestBody List<CreateRoleRequest> requestList) {
        CreateRoleResponse response = roleService.createRoles(requestList);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
