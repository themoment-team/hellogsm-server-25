package team.themoment.hellogsmv3.domain.member.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import team.themoment.hellogsmv3.domain.member.dto.response.FoundMemberSecondTestResDto;
import team.themoment.hellogsmv3.domain.member.entity.Member;
import team.themoment.hellogsmv3.domain.oneseo.entity.EntranceTestResult;
import team.themoment.hellogsmv3.domain.oneseo.entity.Oneseo;
import team.themoment.hellogsmv3.domain.oneseo.service.OneseoService;

@Service
@RequiredArgsConstructor
public class QueryMySecondTestResultService {

    private final MemberService memberService;
    private final OneseoService oneseoService;

    public FoundMemberSecondTestResDto execute(Long memberId) {
        Member member = memberService.findByIdOrThrow(memberId);
        Oneseo oneseo = oneseoService.findByMemberOrThrow(member);

        // no content response status
        if (oneseoService.validateSecondTestResultAnnouncement())
            return null;

        EntranceTestResult entranceTestResult = oneseo.getEntranceTestResult();
        return new FoundMemberSecondTestResDto(entranceTestResult.getSecondTestPassYn(), oneseo.getDecidedMajor());
    }
}
