package me.feseis.fish.logistics.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.feseis.fish.logistics.service.StaffTrainingService;
import me.feseis.fish.logistics.vo.staff.StaffTrainingIssueReqVO;
import me.feseis.fish.logistics.vo.staff.StaffTrainingIssueRespVO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 内部发证接口。只给发证脚本或后续培训管理端调用。
 */
@RestController
@RequiredArgsConstructor
public class InternalStaffTrainingController {

    private static final String ISSUE = "/logistics/internal/staff-trainings";

    private final StaffTrainingService staffTrainingService;

    @PostMapping(ISSUE)
    public ResponseEntity<StaffTrainingIssueRespVO> issue(
            @Valid @RequestBody StaffTrainingIssueReqVO req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(staffTrainingService.issue(req));
    }
}
