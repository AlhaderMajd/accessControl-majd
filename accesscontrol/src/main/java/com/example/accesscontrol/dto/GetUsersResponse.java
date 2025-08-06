package com.example.accesscontrol.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetUsersResponse {
    private List<UserDto> users;
    private int page;
    private long total;
}