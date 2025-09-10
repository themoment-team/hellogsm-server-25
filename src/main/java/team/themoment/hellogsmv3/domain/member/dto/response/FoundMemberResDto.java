package team.themoment.hellogsmv3.domain.member.dto.response;

import lombok.Builder;
import team.themoment.hellogsmv3.domain.member.entity.type.Sex;

import java.time.LocalDate;

@Builder
public record FoundMemberResDto(
        Long memberId,
        String name,
        String phoneNumber,
        LocalDate birth,
        Sex sex
) {
}
