package team.themoment.hellogsmv3.domain.oneseo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import team.themoment.hellogsmv3.domain.member.dto.response.FoundMemberTestResDto;
import team.themoment.hellogsmv3.domain.member.entity.Member;
import team.themoment.hellogsmv3.domain.member.entity.type.Sex;
import team.themoment.hellogsmv3.domain.member.service.MemberService;
import team.themoment.hellogsmv3.domain.member.service.QueryTestResultService;
import team.themoment.hellogsmv3.domain.oneseo.entity.EntranceTestResult;
import team.themoment.hellogsmv3.domain.oneseo.entity.Oneseo;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.YesNo;
import team.themoment.hellogsmv3.global.exception.error.ExpectedException;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;

@DisplayName("QueryTestResultService 클래스의")
public class QueryTestResultServiceTest {
    @Mock
    private MemberService memberService;
    @Mock
    private OneseoService oneseoService;
    @InjectMocks
    private QueryTestResultService queryTestResultService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Nested
    @DisplayName("execute 메소드는")
    class Describe_execute {

        private final Long memberId = 1L;

        @Nested
        @DisplayName("존재하는 회원 ID가 주어지고")
        class Context_with_existing_member_id {

            private Member member;
            private Long OneseoId = 1L;

            @BeforeEach
            void setUp() {
                member = Member.builder()
                        .id(memberId)
                        .name("홍길동")
                        .birth(LocalDate.of(2009,1, 1))
                        .phoneNumber("01012345678")
                        .sex(Sex.MALE)
                        .build();
                given(memberService.findByIdOrThrow(memberId)).willReturn(member);
            }

            @Nested
            @DisplayName("해당 회원의 시험 결과가 존재하면")
            class Context_with_existing_test_result {

                private Oneseo oneseo;

                @BeforeEach
                void setUp() {
                    oneseo = Oneseo.builder()
                            .id(OneseoId)
                            .member(member)
                            .entranceTestResult(
                                    EntranceTestResult.builder()
                                            .id(1L)
                                            .firstTestPassYn(YesNo.YES)
                                            .secondTestPassYn(YesNo.YES)
                                            .build()
                            )
                            .build();
                    given(oneseoService.findByMemberOrThrow(member)).willReturn(oneseo);
                }

                @Test
                @DisplayName("해당 회원의 시험 결과를 반환한다.")
                void it_returns_test_result_of_the_member() {
                    FoundMemberTestResDto result = queryTestResultService.execute(memberId);

                    assertEquals(YesNo.YES, result.firstTestPassYn());
                    assertEquals(YesNo.YES, result.secondTestPassYn());
                }
            }

            @Nested
            @DisplayName("해당 회원의 시험 결과가 존재하지 않으면")
            class Context_with_non_existing_test_result {

                @BeforeEach
                void setUp() {
                    given(oneseoService.findByMemberOrThrow(member)).willThrow(new ExpectedException("해당 지원자의 원서를 찾을 수 없습니다. member ID: " + member.getId(), HttpStatus.NOT_FOUND));
                }

                @Test
                @DisplayName("ExpectedException을 던진다.")
                void it_returns_expected_exception() {
                    ExpectedException exception = assertThrows(ExpectedException.class, () -> queryTestResultService.execute(memberId));

                    assertEquals("해당 지원자의 원서를 찾을 수 없습니다. member ID: " + member.getId(), exception.getMessage());
                    assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
                }
            }
        }

        @Nested
        @DisplayName("존재하지 않는 회원 ID가 주어지면")
        class Context_with_non_existing_member_id {

            @BeforeEach
            void setUp() {
                given(memberService.findByIdOrThrow(memberId)).willThrow(new ExpectedException("존재하지 않는 지원자입니다. member ID: " + memberId, HttpStatus.NOT_FOUND));
            }

            @Test
            @DisplayName("ExpectedException을 던진다.")
            void it_throws_expected_exception() {
                ExpectedException exception = assertThrows(ExpectedException.class, () -> queryTestResultService.execute(memberId));

                assertEquals("존재하지 않는 지원자입니다. member ID: " + memberId, exception.getMessage());
                assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
            }
        }
    }
}
