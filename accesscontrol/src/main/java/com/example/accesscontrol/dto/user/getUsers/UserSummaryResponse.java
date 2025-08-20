package com.example.accesscontrol.dto.user.getUsers;

import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSummaryResponse {
    private Long id;
    private String email;
    private boolean enabled;

    @Builder.Default
    private List<String> roles = new ArrayList<>();

    public UserSummaryResponse(Long id, String email, boolean enabled) {
        this.id = id;
        this.email = email;
        this.enabled = enabled;
        this.roles = new ArrayList<>();
    }
}
