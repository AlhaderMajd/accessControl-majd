package com.example.accesscontrol.dto.group;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UpdateGroupNameRequest {
    @NotBlank @Size(max = 100)
    private String name;
}
