package com.example.accesscontrol.dto;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkCreateUsersRequest {
    private List<CreateUserRequest> users;
}