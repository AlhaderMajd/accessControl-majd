package com.example.accesscontrol.dto.user;

import lombok.Data;

import java.util.List;

@Data
public class DeleteUsersRequest {
    private List<Long> userIds;
}
