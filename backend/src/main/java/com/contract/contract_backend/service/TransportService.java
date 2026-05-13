package com.contract.contract_backend.service;

import com.contract.contract_backend.repository.TransportOrderRepository;
import com.contract.contract_backend.entity.TransportOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransportService {

    @Autowired
    private TransportOrderRepository repo;

    /**
     * 发货时调用：创建运单
     */
    public void createOnShip(Long contractId) {

        TransportOrder order = new TransportOrder();
        order.setContractId(contractId);
        order.setTrackingNo("YD" + contractId + "-" + System.currentTimeMillis());
        order.setStatus("SHIPPED");
        order.setShipTime(LocalDateTime.now());
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());

        repo.save(order);
    }

    /**
     * 到货时调用：更新状态为已到达
     */
    public void arrive(Long contractId) {

        List<TransportOrder> list =
                repo.findByContractIdOrderByIdDesc(contractId);

        if (list.isEmpty()) {
            throw new RuntimeException("运单不存在");
        }

        // ⭐ 取最后一个（最新运单）
        TransportOrder order =
                list.get(0);

        order.setStatus("ARRIVED");
        order.setArriveTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());

        repo.save(order);
    }

    /**
     * 查询（前端用）
     */
    public List<TransportOrder> getByContract(Long contractId) {
        return repo.findByContractIdOrderByIdDesc(contractId);
    }

    /**
     * 运单号生成：YD + 合同ID + 时间戳
     */
    private String generateNo(Long contractId) {
        return "YD" + contractId + "-" + System.currentTimeMillis();
    }
}