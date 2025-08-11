package com.example.accesscontrol.dto.user.deleteUsers;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class DeleteUsersRequest {

    @NotEmpty
    private List<@NotNull Long> userIds;
}
