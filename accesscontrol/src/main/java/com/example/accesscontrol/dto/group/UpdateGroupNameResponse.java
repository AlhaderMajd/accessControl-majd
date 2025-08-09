package com.example.accesscontrol.dto.group;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UpdateGroupNameResponse {
    private String message;
    private Long id;
    private String oldName;
    private String newName;
}
