package vn.dynamicshop.order.dto;

import jakarta.validation.constraints.NotBlank;

public record TransitionOrderStatusRequest(@NotBlank String to, String reason) {
}
