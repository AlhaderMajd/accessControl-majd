package com.example.accesscontrol.dto.group;

import lombok.*;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateGroupsResponse {
    private String message;
    private int createdCount;
    private List<GroupResponse> items;
}
