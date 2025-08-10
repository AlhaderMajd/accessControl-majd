package com.example.accesscontrol.dto.user.getUsers;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetUsersResponse {
    private List<UserSummaryResponse> users;
    private int page;
    private long total;
}