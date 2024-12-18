package error.pirate.backend.shippingInstruction.query.dto;

import error.pirate.backend.exception.CustomException;
import error.pirate.backend.exception.ErrorCodeType;
import error.pirate.backend.productionReceiving.command.domain.aggregate.entity.ProductionReceivingStatus;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ShippingInstructionStatus {
    BEFORE("결재 전"), // 결재전
    AFTER("결재 후"),  // 결재후
    DELETE("삭제");  // 삭제

    private final String value;

    @Data
    public static class ShippingInstructionStatusResponse {
        private String key;
        private String value;

        public ShippingInstructionStatusResponse(String key, ShippingInstructionStatus shippingInstructionStatus) {
            this.key = key;
            this.value = shippingInstructionStatus.getValue();
        }
    }

    public static String statusValue(String status) {
        for (ShippingInstructionStatus s : values()) {
            if (s.name().equals(status)) {
                return s.value;
            }
        }
        throw new CustomException(ErrorCodeType.SHIPPING_INSTRUCTION_STATE_BAD_REQUEST);
    }
}
