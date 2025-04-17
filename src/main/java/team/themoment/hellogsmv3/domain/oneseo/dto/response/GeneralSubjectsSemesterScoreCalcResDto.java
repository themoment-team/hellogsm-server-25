package team.themoment.hellogsmv3.domain.oneseo.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GeneralSubjectsSemesterScoreCalcResDto(
        BigDecimal score1_2,
        BigDecimal score2_1,
        BigDecimal score2_2,
        BigDecimal score3_1,
        BigDecimal score3_2
) {
}
