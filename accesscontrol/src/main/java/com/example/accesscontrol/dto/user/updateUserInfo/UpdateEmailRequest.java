package com.example.accesscontrol.dto.user.updateUserInfo;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEmailRequest {

    @Email
    @NotBlank
    @Size(max = 150)
    private String newEmail;
}
