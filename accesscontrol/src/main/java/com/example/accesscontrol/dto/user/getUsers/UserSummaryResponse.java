package com.example.accesscontrol.dto.user.getUsers;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSummaryResponse {
    private Long id;
    private String email;
    private boolean enabled;
}
