package error.pirate.backend.productionDisbursement.command.domain.aggregate.entity;

import error.pirate.backend.item.command.domain.aggregate.entity.Item;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tb_production_disbursement_item") // 생산불출 품목
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductionDisbursementItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productionDisbursementItemId;

    @ManyToOne(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    @JoinColumn(name = "productionDisbursementItemSeq")
    private ProductionDisbursement productionDisbursement; // 생산불출

    @ManyToOne(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    @JoinColumn(name = "itemSeq")
    private Item item; // 품목

    private Integer productionDisbursementQuantity; // 생산불출 품목 수량

    private String productionDisbursementNote; // 생산불출 품목 비고
}
