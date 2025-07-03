package team.themoment.hellogsmv3.domain.oneseo.dto.internal;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record GeneralSubjectsSemesterScoreCalcDto(
        BigDecimal score1_2,
        BigDecimal score2_1,
        BigDecimal score2_2,
        BigDecimal score3_1,
        BigDecimal score3_2
) {
}
