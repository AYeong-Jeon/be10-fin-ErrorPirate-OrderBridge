package error.pirate.backend.purchase.query.dto;

import error.pirate.backend.common.Pagination;
import error.pirate.backend.purchase.command.domain.aggregate.entity.PurchaseStatus;

public class PurchaseRequest extends Pagination {

    private String clientName;  // 거래처명

    private PurchaseStatus purchaseStatus;

}