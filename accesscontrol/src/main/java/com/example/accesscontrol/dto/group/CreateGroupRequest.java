package com.example.accesscontrol.dto.group;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateGroupRequest {
    @NotBlank
    private String name;
}
