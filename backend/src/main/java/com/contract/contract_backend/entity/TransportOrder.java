package com.contract.contract_backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

    @Entity
    @Table(name = "transport_order")

public class TransportOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long contractId;     // 合同ID（唯一，一个合同一个运单）

    private String trackingNo;   // 运单号

    /**
     * SHIPPED：已发货
     * ARRIVED：已到达
     */
    private String status;

    private LocalDateTime shipTime;     // 发货时间
    private LocalDateTime arriveTime;   // 到货时间

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // ===== getter / setter =====
    public Long getId() { return id; }
    public Long getContractId() { return contractId; }
    public String getTrackingNo() { return trackingNo; }
    public String getStatus() { return status; }
    public LocalDateTime getShipTime() { return shipTime; }
    public LocalDateTime getArriveTime() { return arriveTime; }
    public LocalDateTime getCreateTime() { return createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }

    public void setId(Long id) { this.id = id; }
    public void setContractId(Long contractId) { this.contractId = contractId; }
    public void setTrackingNo(String trackingNo) { this.trackingNo = trackingNo; }
    public void setStatus(String status) { this.status = status; }
    public void setShipTime(LocalDateTime shipTime) { this.shipTime = shipTime; }
    public void setArriveTime(LocalDateTime arriveTime) { this.arriveTime = arriveTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}