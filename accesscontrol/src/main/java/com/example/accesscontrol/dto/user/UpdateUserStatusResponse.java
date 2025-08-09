package com.example.accesscontrol.dto.user;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserStatusResponse {
    private String message;
    private int updatedCount;
}
