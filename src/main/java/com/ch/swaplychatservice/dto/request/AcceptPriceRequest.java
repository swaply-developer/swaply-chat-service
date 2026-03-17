package com.ch.swaplychatservice.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class AcceptPriceRequest {

    @NotNull(message = "협의 가격은 필수입니다.")
    @Min(value = 1, message = "협의 가격은 1원 이상이어야 합니다.")
    private Long price;
}
