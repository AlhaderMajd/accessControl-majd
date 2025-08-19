package com.example.accesscontrol.dto.user.createUsers;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateUsersRequest {
    private List<CreateUserRequest> users;
}
