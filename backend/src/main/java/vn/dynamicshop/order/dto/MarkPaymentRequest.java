package vn.dynamicshop.order.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * {@code to} nhận cả 4 giá trị {@code PaymentStatus} chứ không phải chỉ {@code PAID} —
 * nút trên app hiện chỉ có "Đã nhận tiền", nhưng hoàn tiền (`REFUNDED`) và thu một phần
 * (`PARTIAL`) là chuyện có thật ở quán, và state machine đã biết luật chuyển giữa chúng.
 * Chặn bớt ở tầng HTTP chỉ để rồi phải mở lại ở sprint sau là việc thừa.
 */
public record MarkPaymentRequest(@NotBlank String to, String reason) {
}
