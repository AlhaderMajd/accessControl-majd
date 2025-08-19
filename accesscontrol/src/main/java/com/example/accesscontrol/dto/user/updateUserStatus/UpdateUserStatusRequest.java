package com.example.accesscontrol.dto.user.updateUserStatus;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserStatusRequest {
    private List<Long> userIds;
    private Boolean enabled;
}
