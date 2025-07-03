package team.themoment.hellogsmv3.domain.common.testResult.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record FoundTestResultReqDto(
        @NotNull String phoneNumber,
        @NotNull String code,
        @NotNull String submitCode
) {
}
