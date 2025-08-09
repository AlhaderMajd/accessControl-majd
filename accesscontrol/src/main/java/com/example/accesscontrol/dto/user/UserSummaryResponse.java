package com.example.accesscontrol.dto.user;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserSummaryResponse {
    private Long id;
    private String email;
    private boolean enabled;
}
