package com.example.accesscontrol.dto.user;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DeleteUsersResponse {
    private String message;
    private int deletedCount;
}
