package com.example.accesscontrol.dto.user.updateUserStatus;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserStatusRequest {
    @NotEmpty
    private List<@NotNull Long> userIds;

    @NotNull
    private Boolean enabled;
}
