package error.pirate.backend.workOrder.command.domain.aggregate.entity;

public enum WorkOrderStatus {
    BEFORE, // 결재전
    AFTER, // 결재후
    ONGOING, // 진행중
    COMPLETE , // 완료
    STOP,    // 중단
    DELETE  // 삭제
}
