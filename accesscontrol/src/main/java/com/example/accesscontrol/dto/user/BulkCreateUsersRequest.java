package com.example.accesscontrol.dto.user;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkCreateUsersRequest {
    private List<CreateUserRequest> users;
}