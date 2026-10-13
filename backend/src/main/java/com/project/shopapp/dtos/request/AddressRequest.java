package com.project.shopapp.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressRequest {

    @NotBlank
    @Size(max = 100)
    private String recipientName;

    @NotBlank
    @Pattern(regexp = "^[0-9+]{9,15}$")
    private String phoneNumber;

    @NotBlank
    @Size(max = 255)
    private String shippingAddress;

    private boolean defaultAddress;
}
