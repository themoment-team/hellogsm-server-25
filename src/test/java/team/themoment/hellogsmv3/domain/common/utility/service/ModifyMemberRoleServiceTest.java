package team.themoment.hellogsmv3.domain.common.utility.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import team.themoment.hellogsmv3.domain.member.entity.Member;
import team.themoment.hellogsmv3.domain.member.entity.type.Role;
import team.themoment.hellogsmv3.domain.member.repository.MemberRepository;

import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@DisplayName("ModifyMemberRoleService 클래스의")
class ModifyMemberRoleServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private ModifyMemberRoleService modifyMemberRoleService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Nested
    @DisplayName("execute 메소드는")
    class Describe_execute {

        private final String phoneNumber = "01012345678";
        private final Role targetRole = Role.ADMIN;

        @Nested
        @DisplayName("존재하는 전화번호가 주어지면")
        class Context_with_existing_phone_number {

            @Test
            @DisplayName("해당 회원의 권한을 수정한다")
            void it_modifies_member_role() {
                // given
                Member member = mock(Member.class);
                given(memberRepository.findByPhoneNumber(phoneNumber)).willReturn(Optional.of(member));

                // when
                modifyMemberRoleService.execute(phoneNumber, targetRole);

                // then
                verify(memberRepository).findByPhoneNumber(phoneNumber);
                verify(member).modifyMemberRole(targetRole);
                verifyNoMoreInteractions(memberRepository);
            }
        }

        @Nested
        @DisplayName("존재하지 않는 전화번호가 주어지면")
        class Context_with_non_existing_phone_number {

            @Test
            @DisplayName("아무 동작도 하지 않는다")
            void it_does_nothing() {
                // given
                given(memberRepository.findByPhoneNumber(phoneNumber)).willReturn(Optional.empty());

                // when
                modifyMemberRoleService.execute(phoneNumber, targetRole);

                // then
                verify(memberRepository).findByPhoneNumber(phoneNumber);
                verifyNoMoreInteractions(memberRepository);
            }
        }
    }
}

