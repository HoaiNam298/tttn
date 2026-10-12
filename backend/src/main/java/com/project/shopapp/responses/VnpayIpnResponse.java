package com.project.shopapp.responses;

import com.fasterxml.jackson.annotation.JsonProperty;

public record VnpayIpnResponse(
        @JsonProperty("RspCode") String rspCode, @JsonProperty("Message") String message) {}
