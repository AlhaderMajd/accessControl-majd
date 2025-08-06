package com.example.accesscontrol.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CreateRoleResponse {
    private String message;
    private List<String> created;
}
