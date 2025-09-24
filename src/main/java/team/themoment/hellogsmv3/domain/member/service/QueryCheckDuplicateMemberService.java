package team.themoment.hellogsmv3.domain.member.service;

import static team.themoment.hellogsmv3.domain.oneseo.entity.type.YesNo.*;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import team.themoment.hellogsmv3.domain.member.dto.response.FoundDuplicateMemberResDto;
import team.themoment.hellogsmv3.domain.member.repository.MemberRepository;

@Service
@RequiredArgsConstructor
public class QueryCheckDuplicateMemberService {

  private final MemberRepository memberRepository;

  public FoundDuplicateMemberResDto execute(String phoneNumber) {
    return new FoundDuplicateMemberResDto(
        memberRepository.existsByPhoneNumber(phoneNumber) ? YES : NO);
  }
}
