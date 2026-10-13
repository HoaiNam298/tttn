package com.project.shopapp.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank
    @Size(max = 100)
    private String fullName;

    @NotBlank
    @Pattern(regexp = "^[0-9]{9,15}$", message = "Phone number must contain 9 to 15 digits")
    private String phoneNumber;

    @NotBlank
    @Size(min = 8, max = 72)
    @ToString.Exclude
    private String password;

    @NotBlank
    @Size(max = 255)
    private String address;

    @Past private LocalDate dateOfBirth;
}
