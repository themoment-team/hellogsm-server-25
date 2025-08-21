package team.themoment.hellogsmv3.domain.common.utility.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.themoment.hellogsmv3.domain.member.entity.type.Role;
import team.themoment.hellogsmv3.domain.member.repository.MemberRepository;

@Service
@RequiredArgsConstructor
@Profile("!prod")
public class ModifyMemberRoleService {

    private final MemberRepository memberRepository;

    @Transactional
    public void execute(String phoneNumber, Role role) {
        memberRepository.findByPhoneNumber(phoneNumber)
                .ifPresent(member -> {
                    member.modifyMemberRole(role);
                });
    }
}