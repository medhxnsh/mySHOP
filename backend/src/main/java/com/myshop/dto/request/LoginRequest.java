package com.myshop.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** LoginRequest — email + password for authentication. */
@Data
public class LoginRequest {

    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;
}
