package com.project.shopapp.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewReplyRequest(
        @NotBlank @Size(max = 1000) String reply, @NotNull @Min(0) Long version) {}
