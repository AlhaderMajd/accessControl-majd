package com.example.accesscontrol.dto.user.createUsers;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateUsersRequest {
    private List<CreateUserRequest> users;
}