package com.project.shopapp.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ProductInventoryRequest(
        @NotNull @Min(0) Long version,
        @Min(0) @Max(1000000) Integer stock,
        @NotNull @Size(max = 50) List<@NotNull @Valid ProductVariantRequest> variants,
        @NotNull @Size(max = 10)
                List<
                                @NotNull @Size(max = 500)
                                @jakarta.validation.constraints.Pattern(
                                        regexp =
                                                "^(https?://[^\\s]+|/api/v1/media/[a-f0-9-]+\\.(png|jpg))$")
                                String>
                        images) {}
