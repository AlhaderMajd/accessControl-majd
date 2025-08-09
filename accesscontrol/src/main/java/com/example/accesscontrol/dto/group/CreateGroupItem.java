package com.example.accesscontrol.dto.group;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateGroupItem {
    @NotBlank
    private String name;
}
