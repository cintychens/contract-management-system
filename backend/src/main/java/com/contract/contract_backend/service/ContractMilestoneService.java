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

import org.springframework.transaction.annotation.Transactional;

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
        complete(id, role, username, null);
    }

    public void complete(Long id, String role, String username, String remark) {

        ContractMilestone m = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("节点不存在"));

        Contract contract = contractRepository.findById(m.getContractId())
                .orElseThrow(() -> new RuntimeException("合同不存在"));

        // ⭐ 已终止直接禁止
        if (ContractStatus.TERMINATED.equals(contract.getStatus())) {
            throw new RuntimeException("合同已终止，禁止操作");
        }

// ⭐ 只有生效中的合同才能履约
        if (!ContractStatus.ACTIVE.equals(contract.getStatus())
                && !ContractStatus.IN_PROGRESS.equals(contract.getStatus())) {

            throw new RuntimeException("合同未生效，不能执行履约");
        }

        if ("COMPLETED".equals(m.getStatus())) {
            throw new RuntimeException("节点已完成，不能重复操作");
        }

        List<ContractMilestone> list =
                repository.findByContractIdOrderBySortOrder(m.getContractId());

        // ⭐ 顺序校验
        for (int i = 0; i < list.size(); i++) {

            if (list.get(i).getId().equals(id)) {

                if (i > 0) {

                    ContractMilestone prev = list.get(i - 1);

                    if (!"COMPLETED".equals(prev.getStatus())) {

                        if (!"REWORK".equals(prev.getStatus())
                                && !"REWORK".equals(m.getStatus())) {

                            throw new RuntimeException("必须按顺序完成节点");
                        }
                    }
                }

                break;
            }
        }

        LocalDateTime now = LocalDateTime.now();

        // ⭐ 日志
        ContractMilestoneLog log = new ContractMilestoneLog();

        log.setMilestoneId(id);
        log.setOldStatus(m.getStatus());
        log.setNewStatus("COMPLETED");
        log.setMilestoneName(m.getName());
        log.setOperator(username);
        log.setOperatorRole(role);
        log.setOperateTime(now);
        log.setRemark(normalizeRemark(remark));

        logRepository.save(log);

        // ⭐ 更新节点
        m.setActualDate(now);
        m.setStatus("COMPLETED");

        repository.save(m);

        // =========================
        // ⭐ 运输合同运单逻辑
        // =========================
        if (contract.getContractType() != null
                && contract.getContractType().startsWith("transport")) {

            // 发货 → 创建新运单
            if ("发货".equals(m.getName())) {

                transportService.createOnShip(m.getContractId());
            }

            // 到货 → 更新最新运单状态
            if ("到货".equals(m.getName())) {

                transportService.arrive(m.getContractId());
            }
        }

        // =========================
        // ⭐ 全部完成
        // =========================
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
        if ("结算".equals(m.getName())) {
            Contract contract = contractRepository.findById(m.getContractId())
                    .orElseThrow(() -> new RuntimeException("合同不存在"));

            contract.setStatus(ContractStatus.IN_PROGRESS);
            contractRepository.save(contract);
        }
    }

    public void reject(Long id, String role) {

        ContractMilestone m = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("节点不存在"));
        Contract contract = contractRepository.findById(m.getContractId())
                .orElseThrow(() -> new RuntimeException("合同不存在"));

        if (ContractStatus.TERMINATED.equals(contract.getStatus())) {
            throw new RuntimeException("合同已终止");
        }

        if (!isAcceptanceMilestone(m)) {
            throw new RuntimeException("只有验收节点可以失败");
        }

        m.setStatus("FAILED");

        // ⭐ 继续由业务处理
        m.setResponsibleRole("BUSINESS");

        repository.save(m);
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
            m4.setName("结算");
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

            // ⭐ 仓储验收：复用运输合同的验收通过/不通过逻辑
            ContractMilestone m4 = new ContractMilestone();
            m4.setContractId(contractId);
            m4.setName("验收");
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
        setExpected(id, expectedDate, role, null);
    }

    public void setExpected(Long id, LocalDateTime expectedDate, String role, String remark) {

        ContractMilestone m = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("节点不存在"));

        Contract contract = contractRepository.findById(m.getContractId())
                .orElseThrow(() -> new RuntimeException("合同不存在"));

        if (ContractStatus.TERMINATED.equals(contract.getStatus())) {
            throw new RuntimeException("合同已终止");
        }

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

        String baseRemark = "预计时间调整：" +
                (oldDate == null ? "未设置" : oldDate.toString()) +
                " → " +
                expectedDate.toLocalDate().toString();

        log.setRemark(appendRemark(baseRemark, remark));

        logRepository.save(log);
    }

    public void reportDelay(Long id, String reason, String role, String username) {
        reportDelay(id, reason, role, username, null);
    }

    public void reportDelay(Long id, String reason, String role, String username, String remark) {

        ContractMilestone m = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("节点不存在"));
        Contract contract = contractRepository.findById(m.getContractId())
                .orElseThrow(() -> new RuntimeException("合同不存在"));

        if (ContractStatus.TERMINATED.equals(contract.getStatus())) {
            throw new RuntimeException("合同已终止");
        }

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

        String delayReason = normalizeRemark(reason);
        if (delayReason == null) {
            delayReason = "未填写延期原因";
        }

        String oldStatus = m.getStatus();
        LocalDateTime now = LocalDateTime.now();

        // ⭐⭐⭐ 就是这里加
        m.setDelayReason(delayReason);
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
        log.setRemark(appendRemark(delayReason, remark));
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
        accept(id, status, reason, role, username, null);
    }

    public void accept(Long id, String status, String reason, String role, String username, String remark) {

        ContractMilestone m = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("节点不存在"));

        // ⭐ 权限：必须是当前负责人
        if (!role.equals(m.getResponsibleRole()) && !"ADMIN".equals(role)) {
            throw new RuntimeException("无权限操作");
        }

        if (!isAcceptanceMilestone(m)) {
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

            m.setResponsibleRole("BUSINESS");
            m.setDelayReason(reason);
            m.setDelayTime(now);
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
        log.setRemark(appendRemark(reason, remark));
        logRepository.save(log);
    }

    @Transactional
    public void restartTransport(Long contractId) {

        List<ContractMilestone> list =
                repository.findByContractIdOrderBySortOrder(contractId);

        for (ContractMilestone m : list) {

            String name = m.getName() == null
                    ? ""
                    : m.getName().trim();

            if (name.contains("发货")
                    || name.contains("到货")
                    || name.contains("验收")
                    || name.contains("入库")
                    || name.contains("在库")
                    || name.contains("出库")
                    || name.contains("签收确认")) {

                m.setStatus("PENDING");

                m.setActualDate(null);

                m.setExpectedDate(null);

                m.setDelayReason(null);

                m.setDelayReported(false);

                m.setDelayTime(null);

                if (name.contains("验收") || name.contains("签收确认")) {
                    m.setResponsibleRole("BUSINESS");
                }

                repository.save(m);
            }
        }

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

            // ========= 2. ⭐⭐⭐ 重置履约节点（运输从发货开始，仓储从入库开始）=========
            List<ContractMilestone> list =
                    repository.findByContractIdOrderBySortOrder(m.getContractId());

            boolean reset = false;

            for (ContractMilestone node : list) {

                // ⭐ 运输从发货开始重置；仓储从入库开始重置
                if ("发货".equals(node.getName()) || "入库".equals(node.getName())) {
                    reset = true;
                }

                if (reset) {

                    // ⭐ 重置状态
                    node.setStatus("PENDING");

                    // ⭐ 清空实际完成时间
                    node.setActualDate(null);

                    // ⭐ 清空预计时间（关键）
                    node.setExpectedDate(null);

                    // ⭐ 清空延期
                    node.setDelayReason(null);
                    node.setDelayReported(false);
                    node.setDelayTime(null);

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

    private boolean isAcceptanceMilestone(ContractMilestone milestone) {
        if (milestone == null || milestone.getName() == null) {
            return false;
        }

        String name = milestone.getName().trim();
        return "验收".equals(name) || "签收确认".equals(name);
    }

    private String normalizeRemark(String remark) {
        if (remark == null || remark.isBlank()) {
            return null;
        }

        return remark.trim();
    }

    private String appendRemark(String base, String remark) {
        String normalizedRemark = normalizeRemark(remark);

        if (normalizedRemark == null) {
            return base;
        }

        if (base == null || base.isBlank()) {
            return normalizedRemark;
        }

        if (base.trim().equals(normalizedRemark)) {
            return base;
        }

        return base + "；备注：" + normalizedRemark;
    }

    public void terminateContract(Long contractId) {

        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("合同不存在"));

        // ⭐ 合同状态改成终止
        contract.setStatus(ContractStatus.TERMINATED);

        contractRepository.save(contract);

        // ⭐ 锁定所有节点
        List<ContractMilestone> list =
                repository.findByContractIdOrderBySortOrder(contractId);

        for (ContractMilestone m : list) {

            // 已完成的不动
            if (!"COMPLETED".equals(m.getStatus())) {

                m.setStatus("TERMINATED");
            }
        }

        repository.saveAll(list);
    }

}
