package com.example.accesscontrol.dto.user;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEmailRequest {
    private String newEmail;
}
