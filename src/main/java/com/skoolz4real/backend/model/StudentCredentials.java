package com.skoolz4real.backend.model;


import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StudentCredentials {
    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;
}
