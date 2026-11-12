package me.feseis.fish.logistics.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.feseis.fish.auth.api.dto.AccountLookupRespDTO;
import me.feseis.fish.logistics.client.AuthClient;
import me.feseis.fish.logistics.exp.BizException;
import me.feseis.fish.logistics.exp.ErrorCode;
import me.feseis.fish.logistics.mapper.SiteMapper;
import me.feseis.fish.logistics.mapper.StaffTrainingMapper;
import me.feseis.fish.logistics.mapper.model.SiteOptionRow;
import me.feseis.fish.logistics.mapper.model.StaffTrainingRow;
import me.feseis.fish.logistics.vo.staff.StaffTrainingIssueReqVO;
import me.feseis.fish.logistics.vo.staff.StaffTrainingIssueRespVO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/**
 * 签发培训凭证。姓名与手机号一律取自 auth，业务侧只存快照。
 * 同一账号同一岗位只保留一条凭证，靠 uk_account_role 兜底并发。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StaffTrainingService {

    private static final int RESULT_PASSED = 1;
    private static final int VOUCHER_TTL_DAYS = 7;
    private static final String ISSUER_SYSTEM = "System";

    /** 去掉 0/O/1/I 等易混字符。 */
    private static final char[] ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();
    private static final int CODE_LENGTH = 12;
    private static final int MAX_ATTEMPTS = 3;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final StaffTrainingMapper staffTrainingMapper;
    private final SiteMapper siteMapper;
    private final AuthClient authClient;

    public StaffTrainingIssueRespVO issue(StaffTrainingIssueReqVO req) {
        AccountLookupRespDTO account = authClient.findAccountByPhone(req.phone());
        if (account.realName() == null || account.realName().isBlank()) {
            throw BizException.of(ErrorCode.REAL_NAME_REQUIRED);
        }

        SiteOptionRow site = siteMapper.findByCode(req.siteCode())
                .orElseThrow(() -> BizException.of(ErrorCode.SITE_NOT_FOUND));

        if (staffTrainingMapper.findByAccountAndRole(account.accountId(), req.staffRole()).isPresent()) {
            throw BizException.of(ErrorCode.VOUCHER_EXISTS);
        }

        LocalDateTime now = LocalDateTime.now();
        StaffTrainingRow row = new StaffTrainingRow();
        row.setStaffTrainingId(UUID.randomUUID().toString());
        row.setStaffRole(req.staffRole());
        row.setLogisticsSiteId(site.getSiteId());
        row.setTraineeAuthAccountId(account.accountId());
        row.setTraineeName(account.realName());
        row.setTraineePhone(account.phone());
        row.setTrainedAt(now);
        row.setResult(RESULT_PASSED);
        row.setVoucherExpireAt(now.plusDays(VOUCHER_TTL_DAYS));
        row.setIssuedByStaffId(null);
        row.setIssuedByStaffName(ISSUER_SYSTEM);

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            row.setVoucherCode(newVoucherCode());
            try {
                staffTrainingMapper.insert(row);
                return toResponse(row, site);
            } catch (DuplicateKeyException e) {
                // 凭证码撞 uk_voucher，或并发撞 uk_account_role；重试一次再判定
                log.warn("发证冲突，第 {} 次重试 phone={} staffRole={}",
                        attempt + 1, req.phone(), req.staffRole());
            }
        }
        throw BizException.of(ErrorCode.VOUCHER_EXISTS, "凭证生成冲突，请重新提交");
    }

    private static StaffTrainingIssueRespVO toResponse(StaffTrainingRow row, SiteOptionRow site) {
        return new StaffTrainingIssueRespVO(
                row.getStaffTrainingId(),
                row.getVoucherCode(),
                row.getVoucherExpireAt(),
                row.getStaffRole(),
                site.getSiteCode(),
                site.getSiteName(),
                row.getTraineeName(),
                row.getTraineeAuthAccountId());
    }

    private static String newVoucherCode() {
        StringBuilder builder = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            builder.append(ALPHABET[RANDOM.nextInt(ALPHABET.length)]);
        }
        return builder.toString();
    }
}
