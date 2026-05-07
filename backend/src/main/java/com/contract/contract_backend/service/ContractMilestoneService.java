package com.contract.contract_backend.service;

import com.contract.contract_backend.common.constant.ContractStatus;
import com.contract.contract_backend.entity.Contract;
import com.contract.contract_backend.entity.ContractMilestone;
import com.contract.contract_backend.entity.ContractMilestoneLog;
import com.contract.contract_backend.repository.ContractMilestoneLogRepository;
import com.contract.contract_backend.repository.ContractMilestoneRepository;
import com.contract.contract_backend.service.TransportService;
import com.contract.contract_backend.dto.MilestoneAlertDTO;
import com.contract.contract_backend.dto.AlertSummaryDTO;
import com.contract.contract_backend.repository.ContractRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.time.Duration;

@Service
public class ContractMilestoneService {

    @Autowired
    private ContractMilestoneRepository repository;

    @Autowired
    private ContractMilestoneLogRepository logRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private TransportService transportService;


    public List<ContractMilestone> list(Long contractId) {
        return repository.findByContractIdOrderBySortOrder(contractId);
    }

    public void complete(Long id, String role, String username) {

        ContractMilestone m = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("节点不存在"));

        Contract contract = contractRepository.findById(m.getContractId())
                .orElseThrow(() -> new RuntimeException("合同不存在"));

        if (!ContractStatus.ACTIVE.equals(contract.getStatus())) {
            throw new RuntimeException("合同未生效，不能执行履约");
        }

        if ("COMPLETED".equals(m.getStatus())) {
            throw new RuntimeException("节点已完成，不能重复操作");
        }

        if (m.getExpectedDate() == null) {
            throw new RuntimeException("请先填写预计完成时间");
        }

        List<ContractMilestone> list =
                repository.findByContractIdOrderBySortOrder(m.getContractId());

        // ⭐ 顺序校验（保持不变）
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId().equals(id)) {
                if (i > 0) {
                    ContractMilestone prev = list.get(i - 1);
                    if (!"COMPLETED".equals(prev.getStatus())) {

                        if (!"REWORK".equals(prev.getStatus()) &&
                                !"REWORK".equals(m.getStatus())) {

                            throw new RuntimeException("必须按顺序完成节点");
                        }
                    }
                }
                break;
            }
        }

        LocalDateTime now = LocalDateTime.now();

        // ⭐ 日志（保持不变）
        ContractMilestoneLog log = new ContractMilestoneLog();
        log.setMilestoneId(id);
        log.setOldStatus(m.getStatus());
        log.setNewStatus("COMPLETED");
        log.setMilestoneName(m.getName());
        log.setOperator(username);
        log.setOperatorRole(role);
        log.setOperateTime(now);
        logRepository.save(log);

        m.setActualDate(now);
        m.setStatus("COMPLETED");
        repository.save(m);

        // =========================
        // ⭐⭐⭐ 这里是唯一修改点
        // =========================
        if (contract.getContractType() != null &&
                contract.getContractType().startsWith("transport")) {

            if ("发货".equals(m.getName())) {
                transportService.createOnShip(m.getContractId());
            }

            if ("到货".equals(m.getName())) {
                transportService.arrive(m.getContractId());
            }
        }
        // =========================

        // ⭐ 完成判断（保持不变）
        boolean allDone = repository
                .findByContractIdOrderBySortOrder(m.getContractId())
                .stream()
                .allMatch(x -> "COMPLETED".equals(x.getStatus()));

        if (allDone) {
            contract.setStatus(ContractStatus.COMPLETED);
            contractRepository.save(contract);
        }
    }


    public void complete(Long id) {
        complete(id, "SYSTEM", "SYSTEM");
    }

    public void completeWithState(Long id, String role, String username) {

        ContractMilestone m = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("节点不存在"));

        complete(id, role, username);

        // ⭐ 可选：付款后切状态
        if ("付款".equals(m.getName())) {
            Contract contract = contractRepository.findById(m.getContractId())
                    .orElseThrow(() -> new RuntimeException("合同不存在"));

            contract.setStatus(ContractStatus.IN_PROGRESS);
            contractRepository.save(contract);
        }
    }

    public void initMilestones(Long contractId, LocalDate startDate) {

        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("合同不存在"));

        String type = contract.getContractType();

        // =========================
        // 🚚 运输合同（保持你原来的逻辑）
        // =========================
        if (type != null && type.startsWith("transport")) {

            ContractMilestone m1 = new ContractMilestone();
            m1.setContractId(contractId);
            m1.setName("发货");
            m1.setResponsibleRole("BUSINESS");
            m1.setSortOrder(1);
            m1.setStatus("PENDING");

            ContractMilestone m2 = new ContractMilestone();
            m2.setContractId(contractId);
            m2.setName("到货");
            m2.setResponsibleRole("BUSINESS");
            m2.setSortOrder(2);
            m2.setStatus("PENDING");

            ContractMilestone m3 = new ContractMilestone();
            m3.setContractId(contractId);
            m3.setName("验收");
            m3.setResponsibleRole("BUSINESS");
            m3.setSortOrder(3);
            m3.setStatus("PENDING");

            ContractMilestone m4 = new ContractMilestone();
            m4.setContractId(contractId);
            m4.setName("付款");
            m4.setResponsibleRole("FINANCE");
            m4.setSortOrder(4);
            m4.setStatus("PENDING");

            ContractMilestone m5 = new ContractMilestone();
            m5.setContractId(contractId);
            m5.setName("最终确认");
            m5.setResponsibleRole("BUSINESS");
            m5.setSortOrder(5);
            m5.setStatus("PENDING");

            repository.saveAll(List.of(m1, m2, m3, m4, m5));
        }

        // =========================
        // 📦 仓储合同（只改名字，逻辑完全一样）
        // =========================
        else if (type != null && type.startsWith("warehouse")) {

            ContractMilestone m1 = new ContractMilestone();
            m1.setContractId(contractId);
            m1.setName("入库");
            m1.setResponsibleRole("BUSINESS");
            m1.setSortOrder(1);
            m1.setStatus("PENDING");

            ContractMilestone m2 = new ContractMilestone();
            m2.setContractId(contractId);
            m2.setName("在库");
            m2.setResponsibleRole("BUSINESS");
            m2.setSortOrder(2);
            m2.setStatus("PENDING");

            ContractMilestone m3 = new ContractMilestone();
            m3.setContractId(contractId);
            m3.setName("出库");
            m3.setResponsibleRole("BUSINESS");
            m3.setSortOrder(3);
            m3.setStatus("PENDING");

            // ⭐ 新增：签收确认
            ContractMilestone m4 = new ContractMilestone();
            m4.setContractId(contractId);
            m4.setName("签收确认");
            m4.setResponsibleRole("BUSINESS");
            m4.setSortOrder(4);
            m4.setStatus("PENDING");

// 结算（顺序往后挪）
            ContractMilestone m5 = new ContractMilestone();
            m5.setContractId(contractId);
            m5.setName("结算");
            m5.setResponsibleRole("FINANCE");
            m5.setSortOrder(5);
            m5.setStatus("PENDING");

// 最终确认
            ContractMilestone m6 = new ContractMilestone();
            m6.setContractId(contractId);
            m6.setName("最终确认");
            m6.setResponsibleRole("BUSINESS");
            m6.setSortOrder(6);
            m6.setStatus("PENDING");

            repository.saveAll(List.of(m1, m2, m3, m4, m5, m6));
        }

        // =========================
        // ❗兜底（防止未知类型）
        // =========================
        else {
            throw new RuntimeException("未知合同类型：" + type);
        }
    }

    public void setExpected(Long id, LocalDateTime expectedDate, String role) {

        ContractMilestone m = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("节点不存在"));

        if (!role.equals(m.getResponsibleRole()) && !"ADMIN".equals(role)) {
            throw new RuntimeException("无权限设置预计时间");
        }

        if ("COMPLETED".equals(m.getStatus())) {
            throw new RuntimeException("节点已完成，不能修改预计时间");
        }

        if (m.getExpectedDate() != null &&
                LocalDateTime.now().isAfter(m.getExpectedDate()) &&
                (m.getDelayReason() == null || m.getDelayReason().isEmpty())) {

            throw new RuntimeException("请先填写延期原因，再调整预计时间");
        }

        LocalDate oldDate = m.getExpectedDate() == null
                ? null
                : m.getExpectedDate().toLocalDate();

        m.setExpectedDate(expectedDate);
        repository.save(m);

        LocalDateTime now = LocalDateTime.now();

        ContractMilestoneLog log = new ContractMilestoneLog();
        log.setMilestoneId(id);
        log.setOldStatus("EXPECTED_CHANGE");
        log.setNewStatus("EXPECTED_CHANGE");
        log.setOldDate(oldDate);
        log.setNewDate(expectedDate.toLocalDate());
        log.setMilestoneName(m.getName());
        log.setOperator(role);
        log.setOperatorRole(role);
        log.setOperateTime(now);

        log.setRemark("预计时间调整：" +
                (oldDate == null ? "未设置" : oldDate.toString()) +
                " → " +
                expectedDate.toLocalDate().toString());

        logRepository.save(log);
    }

    public void reportDelay(Long id, String reason, String role, String username) {

        ContractMilestone m = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("节点不存在"));

        if (!role.equals(m.getResponsibleRole()) && !"ADMIN".equals(role)) {
            throw new RuntimeException("无权限操作");
        }

        if (m.getExpectedDate() == null) {
            throw new RuntimeException("请先设置预计时间");
        }

        if (!LocalDateTime.now().isAfter(m.getExpectedDate())) {
            throw new RuntimeException("未逾期，无需上报延期");
        }

        if ("COMPLETED".equals(m.getStatus())) {
            throw new RuntimeException("节点已完成，不能上报延期");
        }

        if (reason == null || reason.isBlank()) {
            throw new RuntimeException("延期原因不能为空");
        }

        String oldStatus = m.getStatus();
        LocalDateTime now = LocalDateTime.now();

        // ⭐⭐⭐ 就是这里加
        m.setDelayReason(reason);
        m.setDelayReported(true);
        m.setDelayTime(now);
        repository.save(m);

        // 日志（你已经写对了）
        ContractMilestoneLog log = new ContractMilestoneLog();
        log.setMilestoneId(id);
        log.setOldStatus(oldStatus);
        log.setNewStatus("DELAY_REPORTED");
        log.setMilestoneName(m.getName());
        log.setOperator(username);
        log.setOperatorRole(role);
        log.setOperateTime(now);
        log.setDelayTime(now);
        log.setRemark(reason);
        logRepository.save(log);
    }

    public List<MilestoneAlertDTO> getMilestoneAlerts() {

        List<ContractMilestone> list = repository.findAll();
        LocalDateTime now = LocalDateTime.now();

        List<MilestoneAlertDTO> result = new ArrayList<>();

        for (ContractMilestone m : list) {

            // 已完成，不进入预警
            if (m.getActualDate() != null) {
                continue;
            }

            // 没有预计完成时间，不进入预警
            if (m.getExpectedDate() == null) {
                continue;
            }

            long diffDays = Duration.between(now, m.getExpectedDate()).toDays();

            String type;

// 已逾期：用精确时间判断，不要用 diffDays < 0
            if (m.getExpectedDate().isBefore(now)) {
                type = "critical";
            }
// 3天内到期
            else if (m.getExpectedDate().isBefore(now.plusDays(3))) {
                type = "warning";
            }
// 正常
            else {
                continue;
            }

            MilestoneAlertDTO dto = new MilestoneAlertDTO();
            dto.setMilestoneId(m.getId());
            dto.setContractId(m.getContractId());
            dto.setMilestoneName(m.getName());
            dto.setExpectedDate(m.getExpectedDate());
            dto.setResponsibleRole(m.getResponsibleRole());
            dto.setType(type);
            dto.setDiffDays(diffDays);
            dto.setDelayReason(m.getDelayReason());

            Contract c = contractRepository.findById(m.getContractId()).orElse(null);
            if (c != null) {
                dto.setContractTitle(c.getTitle());
                dto.setContractNo(c.getContractNo());
            }

            result.add(dto);
        }

        return result;
    }

    public AlertSummaryDTO getSummary() {

        List<Contract> contracts = contractRepository.findAll();

        AlertSummaryDTO dto = new AlertSummaryDTO();

        LocalDateTime now = LocalDateTime.now();

        for (Contract c : contracts) {

            List<ContractMilestone> ms =
                    repository.findByContractIdOrderBySortOrder(c.getContractId());

            boolean allDone = ms.stream()
                    .allMatch(m -> m.getActualDate() != null);

            if (allDone) {
                dto.setCompleted(dto.getCompleted() + 1);
                continue;
            }

            boolean hasWarning = false;
            boolean hasProcessing = false;

            for (ContractMilestone m : ms) {

                if (m.getActualDate() != null) continue;

                if (m.getExpectedDate() == null) continue;

                if (m.getExpectedDate().isBefore(now.plusDays(3))) {
                    hasWarning = true;
                }

                hasProcessing = true;
            }

            if (hasWarning) {
                dto.setWarning(dto.getWarning() + 1);
            } else if (hasProcessing) {
                dto.setProcessing(dto.getProcessing() + 1);
            } else {
                dto.setNormal(dto.getNormal() + 1);
            }
        }

        return dto;
    }

    public void accept(Long id, String status, String reason, String role, String username) {

        ContractMilestone m = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("节点不存在"));

        // ⭐ 权限：必须是当前负责人
        if (!role.equals(m.getResponsibleRole()) && !"ADMIN".equals(role)) {
            throw new RuntimeException("无权限操作");
        }

        if (!"验收".equals(m.getName())) {
            throw new RuntimeException("当前节点不是验收节点");
        }

        LocalDateTime now = LocalDateTime.now();

        // ✅ 验收通过（业务完成）
        if ("PASSED".equals(status)) {

            m.setStatus("COMPLETED");
            m.setActualDate(now);

        }
        // ❌ 验收失败（转交法务）
        else if ("FAILED".equals(status)) {

            if (reason == null || reason.isBlank()) {
                throw new RuntimeException("验收不通过必须填写原因");
            }

            m.setStatus("FAILED");
            m.setDelayReason(reason);
            m.setDelayTime(now);

            // ⭐⭐⭐ 核心：切换负责人！！
            m.setResponsibleRole("LEGAL");
        }
        else {
            throw new RuntimeException("未知验收状态");
        }

        repository.save(m);

        // 日志
        ContractMilestoneLog log = new ContractMilestoneLog();
        log.setMilestoneId(id);
        log.setOldStatus("验收");
        log.setNewStatus(status);
        log.setMilestoneName(m.getName());
        log.setOperator(username);
        log.setOperatorRole(role);
        log.setOperateTime(now);
        log.setRemark(reason);
        logRepository.save(log);
    }

    public void legalProcess(Long id, String result, String role,
                             String reason, String responsibility, String action,
                             String username) {

        ContractMilestone m = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("节点不存在"));

        // 权限校验
        if (!role.equals(m.getResponsibleRole()) && !"ADMIN".equals(role)) {
            throw new RuntimeException("无权限操作");
        }

        // 只允许在验收失败时进入法务处理
        if (!"FAILED".equals(m.getStatus())) {
            throw new RuntimeException("当前不是验收失败状态");
        }

        // ⭐ 记录旧状态（必须在修改前拿）
        String oldStatus = m.getStatus();

        LocalDateTime now = LocalDateTime.now();

        if ("CONTINUE".equals(result)) {

            // ========= 1. 验收节点进入整改 =========
            m.setStatus("REWORK");
            m.setResponsibleRole("BUSINESS");

            m.setDelayReason(reason);
            m.setResponsibility(responsibility);
            m.setHandleAction(action);

            m.setActualDate(null);     // 允许再次验收
            m.setExpectedDate(null);   // ⭐ 建议清空，重新填写时间

            // ========= 2. ⭐⭐⭐ 重置运输节点（核心！！！）=========
            List<ContractMilestone> list =
                    repository.findByContractIdOrderBySortOrder(m.getContractId());

            boolean reset = false;

            for (ContractMilestone node : list) {

                // ⭐ 从发货开始重置
                if ("发货".equals(node.getName())) {
                    reset = true;
                }

                if (reset) {
                    node.setActualDate(null);
                    node.setStatus("PENDING");

                    repository.save(node);
                }
            }

            // ========= 3. 写日志（FAILED → REWORK）=========
            ContractMilestoneLog log = new ContractMilestoneLog();
            log.setMilestoneId(id);
            log.setOldStatus(oldStatus);   // ⭐ 用修改前的
            log.setNewStatus("REWORK");
            log.setMilestoneName(m.getName());
            log.setOperator(username);
            log.setOperatorRole(role);
            log.setOperateTime(now);
            log.setRemark("法务处理：" + reason + " / " + action);

            logRepository.save(log);

        } else {

            // ========= 终止合同 =========
            Contract contract = contractRepository.findById(m.getContractId())
                    .orElseThrow(() -> new RuntimeException("合同不存在"));

            contract.setStatus(ContractStatus.TERMINATED);
            contractRepository.save(contract);

            // ========= 终止日志 =========
            ContractMilestoneLog log = new ContractMilestoneLog();
            log.setMilestoneId(id);
            log.setOldStatus(oldStatus);
            log.setNewStatus("TERMINATED");
            log.setMilestoneName(m.getName());
            log.setOperator(username);
            log.setOperatorRole(role);
            log.setOperateTime(now);
            log.setRemark("法务终止合同：" + reason);

            logRepository.save(log);
        }

        // ⭐ 最后统一保存验收节点
        repository.save(m);
    }

    public void terminateContract(Long contractId) {

        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("合同不存在"));

        contract.setStatus(ContractStatus.TERMINATED);
        contractRepository.save(contract);
    }

}