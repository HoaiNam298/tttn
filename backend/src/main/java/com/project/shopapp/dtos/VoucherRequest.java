package com.project.shopapp.dtos;

import com.project.shopapp.enums.VoucherType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoucherRequest {

    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9_-]{3,40}$")
    private String code;

    @NotNull private VoucherType type;

    @NotNull
    @DecimalMin("0.01")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal amount;

    @NotNull
    @DecimalMin("0")
    @Digits(integer = 12, fraction = 2)
    private BigDecimal minimumSubtotal;

    @NotNull
    @DecimalMin("0.01")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal maximumDiscount;

    @NotNull private Instant startsAt;
    @NotNull private Instant endsAt;

    @NotNull
    @Min(1)
    @Max(1000000)
    private Integer usageLimit;

    @NotNull
    @Min(1)
    @Max(1000000)
    private Integer perUserLimit;

    @Min(1)
    private Long targetUserId;

    private boolean active;

    @Min(0)
    private Long version;
}
