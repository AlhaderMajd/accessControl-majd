package com.example.accesscontrol.dto;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserStatusRequest {
    private List<Long> userIds;
    private Boolean enabled;
}
