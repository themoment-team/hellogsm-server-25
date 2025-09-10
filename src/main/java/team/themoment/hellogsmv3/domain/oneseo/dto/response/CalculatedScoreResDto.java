package team.themoment.hellogsmv3.domain.oneseo.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record CalculatedScoreResDto(
        @JsonInclude(JsonInclude.Include.NON_NULL)
        BigDecimal generalSubjectsScore,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        GeneralSubjectsScoreDetailResDto generalSubjectsScoreDetail,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        BigDecimal artsPhysicalSubjectsScore,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        BigDecimal totalSubjectsScore,
        BigDecimal attendanceScore,
        BigDecimal volunteerScore,
        BigDecimal totalScore
) {
}
