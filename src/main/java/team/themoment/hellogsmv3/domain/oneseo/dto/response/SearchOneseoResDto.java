package team.themoment.hellogsmv3.domain.oneseo.dto.response;

import lombok.Builder;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.Screening;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.YesNo;

import java.math.BigDecimal;

@Builder
public record SearchOneseoResDto(
        Long memberId,
        String submitCode,
        YesNo realOneseoArrivedYn,
        String name,
        Screening screening,
        String schoolName,
        String phoneNumber,
        String guardianPhoneNumber,
        String schoolTeacherPhoneNumber,
        String examinationNumber,
        YesNo firstTestPassYn,
        BigDecimal competencyEvaluationScore,
        BigDecimal interviewScore,
        YesNo secondTestPassYn,
        YesNo entranceIntentionYn
) {
}
