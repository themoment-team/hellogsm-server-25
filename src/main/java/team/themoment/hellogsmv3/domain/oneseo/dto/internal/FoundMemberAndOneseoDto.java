package team.themoment.hellogsmv3.domain.oneseo.dto.internal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import team.themoment.hellogsmv3.domain.member.entity.Member;
import team.themoment.hellogsmv3.domain.oneseo.entity.Oneseo;

@Getter
@Setter
@AllArgsConstructor
public class FoundMemberAndOneseoDto {
    private Member member;
    private Oneseo oneseo;
}
