package team.themoment.hellogsmv3.domain.oneseo.service;

import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import team.themoment.hellogsmv3.domain.member.entity.Member;
import team.themoment.hellogsmv3.domain.member.service.MemberService;
import team.themoment.hellogsmv3.domain.oneseo.dto.request.CompetencyEvaluationScoreReqDto;
import team.themoment.hellogsmv3.domain.oneseo.entity.EntranceTestResult;
import team.themoment.hellogsmv3.domain.oneseo.entity.Oneseo;
import team.themoment.hellogsmv3.domain.oneseo.repository.EntranceTestResultRepository;

@Service
@RequiredArgsConstructor
public class ModifyCompetencyEvaluationScoreService {

  private final MemberService memberService;
  private final OneseoService oneseoService;
  private final EntranceTestResultRepository entranceTestResultRepository;

  public void execute(
      Long memberId, CompetencyEvaluationScoreReqDto competencyEvaluationScoreReqDto) {
    Member member = memberService.findByIdOrThrow(memberId);
    Oneseo oneseo = oneseoService.findByMemberOrThrow(member);

    EntranceTestResult entranceTestResult = oneseo.getEntranceTestResult();
    OneseoService.isBeforeSecondTest(entranceTestResult.getSecondTestPassYn());

    BigDecimal competencyEvaluationScore =
        competencyEvaluationScoreReqDto.competencyEvaluationScore();
    entranceTestResult.modifyCompetencyEvaluationScore(competencyEvaluationScore);

    entranceTestResultRepository.save(entranceTestResult);
  }
}
