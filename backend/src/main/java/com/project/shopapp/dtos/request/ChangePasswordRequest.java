package com.project.shopapp.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.ToString;

@Data
public class ChangePasswordRequest {
    @NotBlank
    @Size(max = 1024)
    @ToString.Exclude
    private String currentPassword;

    @NotBlank
    @Size(min = 8, max = 72)
    @ToString.Exclude
    private String newPassword;
}
