package me.feseis.fish.logistics.service;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.feseis.fish.logistics.client.AuthClient;
import me.feseis.fish.logistics.exp.BizException;
import me.feseis.fish.logistics.exp.ErrorCode;
import me.feseis.fish.logistics.mapper.ApprovalMapper;
import me.feseis.fish.logistics.mapper.EventOutboxMapper;
import me.feseis.fish.logistics.mapper.SiteMapper;
import me.feseis.fish.logistics.mapper.StaffApplyMapper;
import me.feseis.fish.logistics.mapper.StaffMapper;
import me.feseis.fish.logistics.mapper.StaffTrainingMapper;
import me.feseis.fish.logistics.mapper.model.ApprovalRow;
import me.feseis.fish.logistics.mapper.model.SiteOptionRow;
import me.feseis.fish.logistics.mapper.model.StaffRow;
import me.feseis.fish.logistics.mapper.model.StaffTrainingRow;
import me.feseis.fish.logistics.support.StaffNoGenerator;
import me.feseis.fish.logistics.vo.staff.StaffApplyReqVO;
import me.feseis.fish.logistics.vo.staff.StaffApplyRespVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 入职：校验证书 → 建档 → 写申请与审核 → 凭证失效 → 授予员工角色。
 * 授予角色同步调用，失败整笔回滚；outbox 仅留审计痕迹。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StaffApplyService {

    private static final int RESULT_PASSED = 1;
    private static final int STATUS_ENABLED = 1;

    private static final int OP_USER = 4;
    private static final int OP_SYSTEM = 1;
    private static final int APPROVAL_PASSED = 2;
    private static final int BIZ_TYPE_STAFF_APPLY = 3;

    private static final int EVENT_TARGET_AUTH = 1;
    private static final int EVENT_TYPE_GRANT_ROLE = 1;
    private static final int AUTH_ROLE_STAFF = 2;

    private final StaffTrainingMapper staffTrainingMapper;
    private final StaffMapper staffMapper;
    private final StaffApplyMapper staffApplyMapper;
    private final ApprovalMapper approvalMapper;
    private final EventOutboxMapper eventOutboxMapper;
    private final SiteMapper siteMapper;
    private final StaffNoGenerator staffNoGenerator;
    private final AuthClient authClient;

    @Transactional
    public StaffApplyRespVO apply(String accountId, StaffApplyReqVO req) {
        // 先判员工身份：重复提交时给出 409 而不是「凭证已失效」
        if (staffMapper.findByAccountId(accountId).isPresent()) {
            throw BizException.of(ErrorCode.ALREADY_STAFF);
        }

        String voucherCode = req.voucherCode().trim().toUpperCase();
        StaffTrainingRow training = staffTrainingMapper.findByVoucherForUpdate(voucherCode)
                .orElseThrow(() -> BizException.of(ErrorCode.VOUCHER_NOT_FOUND));
        LocalDateTime now = LocalDateTime.now();

        if (!Integer.valueOf(RESULT_PASSED).equals(training.getResult())
                || training.getVoucherExpireAt() == null
                || !training.getVoucherExpireAt().isAfter(now)) {
            throw BizException.of(ErrorCode.VOUCHER_EXPIRED);
        }
        if (!accountId.equals(training.getTraineeAuthAccountId())) {
            throw BizException.of(ErrorCode.VOUCHER_ACCOUNT_MISMATCH);
        }

        StaffRow staff = new StaffRow();
        staff.setLogisticsStaffId(UUID.randomUUID().toString());
        staff.setAuthAccountId(accountId);
        staff.setStaffNo(staffNoGenerator.next());
        staff.setStaffName(training.getTraineeName());
        staff.setStaffPhone(training.getTraineePhone());
        staff.setStaffRole(training.getStaffRole());
        staff.setLogisticsSiteId(training.getLogisticsSiteId());
        staff.setEnabled(STATUS_ENABLED);
        staff.setHiredAt(now);
        staffMapper.insert(staff);

        String applyId = UUID.randomUUID().toString();
        staffApplyMapper.insert(applyId, accountId, training.getStaffTrainingId(),
                training.getStaffRole(), req.applyRemark());

        ApprovalRow approval = new ApprovalRow();
        approval.setApprovalId(UUID.randomUUID().toString());
        approval.setBizType(BIZ_TYPE_STAFF_APPLY);
        approval.setBizId(applyId);
        approval.setStatus(APPROVAL_PASSED);
        approval.setApplyOperatorType(OP_USER);
        approval.setApplyOperatorId(accountId);
        approval.setApplyOperatorName(training.getTraineeName());
        approval.setApplyRemark(req.applyRemark());
        approval.setAuditOperatorType(OP_SYSTEM);
        approval.setAuditOperatorName("系统审核");
        approval.setAuditRemark("入职申请即审");
        approval.setAuditedAt(now);
        approvalMapper.insert(approval);

        staffTrainingMapper.expireVoucher(training.getStaffTrainingId(), now);

        authClient.grantRole(accountId, AUTH_ROLE_STAFF);

        eventOutboxMapper.insertRoleGrant(EVENT_TARGET_AUTH, EVENT_TYPE_GRANT_ROLE,
                accountId, AUTH_ROLE_STAFF, BIZ_TYPE_STAFF_APPLY, applyId);

        SiteOptionRow site = siteMapper.findById(training.getLogisticsSiteId()).orElse(null);
        return new StaffApplyRespVO(
                staff.getLogisticsStaffId(),
                staff.getStaffNo(),
                staff.getStaffName(),
                staff.getStaffRole(),
                site == null ? null : site.getSiteCode(),
                site == null ? null : site.getSiteName(),
                now);
    }
}
