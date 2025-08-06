package com.example.accesscontrol.controller;

import com.example.accesscontrol.dto.*;
import com.example.accesscontrol.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<BulkCreateUsersResponse> createUsers(@RequestBody BulkCreateUsersRequest request) {
        BulkCreateUsersResponse response = userService.createUsers(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<GetUsersResponse> getUsers(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        if (page < 0 || size <= 0) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(userService.getUsers(search, page, size));
    }
}