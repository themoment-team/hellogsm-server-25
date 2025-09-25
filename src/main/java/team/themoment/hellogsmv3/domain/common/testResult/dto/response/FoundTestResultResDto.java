package team.themoment.hellogsmv3.domain.common.testResult.dto.response;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.*;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.Major;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.YesNo;

@Builder
@JsonInclude(NON_NULL)
public record FoundTestResultResDto(String name, YesNo firstTestPassYn, YesNo secondTestPassYn, Major decidedMajor) {
}
