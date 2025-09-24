package team.themoment.hellogsmv3.domain.oneseo.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CompetencyEvaluationScoreReqDto(
    @NotNull
        @DecimalMin(value = "0.0", message = "0점 이상이여야 합니다.")
        @DecimalMax(value = "100.0", message = "100점 이하여야 합니다.")
        BigDecimal competencyEvaluationScore) {}
