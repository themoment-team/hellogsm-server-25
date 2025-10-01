package team.themoment.hellogsmv3.domain.member.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import team.themoment.hellogsmv3.domain.member.dto.response.FoundMemberFirstTestResDto;
import team.themoment.hellogsmv3.domain.member.entity.Member;
import team.themoment.hellogsmv3.domain.oneseo.entity.EntranceTestResult;
import team.themoment.hellogsmv3.domain.oneseo.entity.Oneseo;
import team.themoment.hellogsmv3.domain.oneseo.service.OneseoService;

@Service
@RequiredArgsConstructor
public class QueryMyFirstTestResultService {

    private final MemberService memberService;
    private final OneseoService oneseoService;

    @Transactional(readOnly = true)
    public FoundMemberFirstTestResDto execute(Long memberId) {
        Member member = memberService.findByIdOrThrow(memberId);
        Oneseo oneseo = oneseoService.findByMemberOrThrow(member);

        // no content response status
        if (oneseoService.validateFirstTestResultAnnouncement())
            return null;

        EntranceTestResult entranceTestResult = oneseo.getEntranceTestResult();
        return new FoundMemberFirstTestResDto(entranceTestResult.getFirstTestPassYn());
    }
}
