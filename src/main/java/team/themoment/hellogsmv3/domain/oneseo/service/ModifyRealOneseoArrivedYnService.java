package team.themoment.hellogsmv3.domain.oneseo.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import team.themoment.hellogsmv3.domain.member.entity.Member;
import team.themoment.hellogsmv3.domain.member.service.MemberService;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.ArrivedStatusResDto;
import team.themoment.hellogsmv3.domain.oneseo.entity.EntranceTestResult;
import team.themoment.hellogsmv3.domain.oneseo.entity.Oneseo;
import team.themoment.hellogsmv3.domain.oneseo.repository.OneseoRepository;

@Service
@RequiredArgsConstructor
public class ModifyRealOneseoArrivedYnService {

    private final MemberService memberService;
    private final OneseoService oneseoService;
    private final OneseoRepository oneseoRepository;

    @Transactional
    public ArrivedStatusResDto execute(Long memberId) {
        Member member = memberService.findByIdOrThrow(memberId);
        Oneseo oneseo = oneseoService.findByMemberOrThrow(member);

        EntranceTestResult entranceTestResult = oneseo.getEntranceTestResult();
        OneseoService.isBeforeFirstTest(entranceTestResult.getFirstTestPassYn());

        oneseo.switchRealOneseoArrivedYn();
        Oneseo modifiedOneseo = oneseoRepository.save(oneseo);

        return new ArrivedStatusResDto(modifiedOneseo.getRealOneseoArrivedYn());
    }
}
