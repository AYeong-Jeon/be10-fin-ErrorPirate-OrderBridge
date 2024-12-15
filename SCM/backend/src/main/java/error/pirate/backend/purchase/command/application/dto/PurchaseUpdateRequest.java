package error.pirate.backend.purchase.command.application.dto;

import error.pirate.backend.purchase.command.domain.aggregate.entity.PurchaseStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseUpdateRequest {

    private Long purchaseSeq;  // 구매서 번호

    private Long purchaseOrderSeq;  // 발주서 번호

    private Long userSeq; // 구매서 담당자

    private Long warehouseSeq;  // 입고창고 번호

    private PurchaseStatus purchaseStatus = PurchaseStatus.PROGRESS; // 구매서 상태

    private LocalDateTime purchaseContractDate; // 구매서 계약일

    private LocalDateTime purchaseRegdate; // 구매서 등록일

    private LocalDateTime purchaseModDate; // 구매서 수정일

    private Integer purchaseExtendedPrice; // 구매서 총금액

    private String purchaseNote; // 구매서 비고

    private List<PurchaseItemDto> purchaseItemDtoList;   // 구매 품목 리스트

}