package com.example.accesscontrol.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DeleteUsersResponse {
    private String message;
    private int deletedCount;
}
