package com.project.shopapp.dtos.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductInventoryRequest {

    @NotNull
    @Min(0)
    private Long version;

    @Min(0)
    @Max(1000000)
    private Integer stock;

    @NotNull
    @Size(max = 50)
    private List<@NotNull @Valid ProductVariantRequest> variants;

    @NotNull
    @Size(max = 10)
    private List<
                    @NotNull @Size(max = 500)
                    @Pattern(regexp = "^(https?://[^\\s]+|/api/v1/media/[a-f0-9-]+\\.(png|jpg))$")
                    String>
            images;
}
