package team.themoment.hellogsmv3.domain.member.repository;

import org.springframework.data.repository.CrudRepository;
import team.themoment.hellogsmv3.domain.member.entity.AuthenticationCode;
import team.themoment.hellogsmv3.domain.member.entity.type.AuthCodeType;

import java.util.Optional;

public interface CodeRepository extends CrudRepository<AuthenticationCode, String> {
    Optional<AuthenticationCode> findByMemberIdAndAuthCodeType(Long memberId, AuthCodeType authCodeType);
    Optional<AuthenticationCode> findByCode(String code);
    void deleteByMemberId(Long memberId);
}
