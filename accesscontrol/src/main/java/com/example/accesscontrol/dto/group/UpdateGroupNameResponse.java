package com.example.accesscontrol.dto.group;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class UpdateGroupNameResponse {
    private String message;
    private Long id;
    private String oldName;
    private String newName;
}
