package team.themoment.hellogsmv3.domain.oneseo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import team.themoment.hellogsmv3.domain.common.testResult.dto.response.FoundTestResultResDto;
import team.themoment.hellogsmv3.domain.common.testResult.service.QueryTestResultService;
import team.themoment.hellogsmv3.domain.member.entity.Member;
import team.themoment.hellogsmv3.domain.member.entity.type.Sex;
import team.themoment.hellogsmv3.domain.member.service.CommonCodeService;
import team.themoment.hellogsmv3.domain.oneseo.entity.EntranceTestResult;
import team.themoment.hellogsmv3.domain.oneseo.entity.Oneseo;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.Major;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.YesNo;
import team.themoment.hellogsmv3.domain.oneseo.repository.OneseoRepository;
import team.themoment.hellogsmv3.global.exception.error.ExpectedException;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static team.themoment.hellogsmv3.domain.common.testResult.type.TestType.FIRST;
import static team.themoment.hellogsmv3.domain.common.testResult.type.TestType.SECOND;

@DisplayName("QueryTestResultService 클래스의")
class QueryTestResultServiceTest {
    @Mock
    private OneseoService oneseoService;
    @Mock
    private OneseoRepository oneseoRepository;
    @Mock
    private CommonCodeService commonCodeService;
    @InjectMocks
    private QueryTestResultService queryTestResultService;

    private final Long memberId = 1L;
    private final String code = "123456";
    private final String phoneNumber = "01012345678";
    private final String oneseoCode = "A-1";
    private final String examinationNumber = "0101";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private Member buildMember(String name) {
        return Member.builder()
                .id(memberId)
                .name(name)
                .birth(LocalDate.of(2009, 1, 1))
                .phoneNumber(phoneNumber)
                .sex(Sex.MALE)
                .build();
    }

    private Oneseo buildFirstTestOneseo(Member member) {
        return Oneseo.builder()
                .id(1L)
                .member(member)
                .entranceTestResult(EntranceTestResult.builder()
                        .id(1L)
                        .firstTestPassYn(YesNo.YES)
                        .secondTestPassYn(YesNo.NO)
                        .build())
                .build();
    }

    private Oneseo buildSecondTestOneseo(Member member) {
        return Oneseo.builder()
                .id(1L)
                .member(member)
                .entranceTestResult(EntranceTestResult.builder()
                        .id(1L)
                        .firstTestPassYn(YesNo.YES)
                        .secondTestPassYn(YesNo.YES)
                        .build())
                .decidedMajor(Major.SW)
                .build();
    }

    @Nested
    @DisplayName("execute 메소드는")
    class Describe_execute {

        @Nested
        @DisplayName("1차 전형 결과 조회 시")
        class Context_with_first_test_type {

            @Nested
            @DisplayName("발표 시간이 되었고 유효한 정보가 주어지면")
            class Context_with_valid_announcement_time_and_info {

                @Test
                @DisplayName("1차 전형 결과를 반환한다")
                void it_returns_first_test_result() {
                    Member member = buildMember("홍길동");
                    Oneseo oneseo = buildFirstTestOneseo(member);

                    given(oneseoService.validateFirstTestResultAnnouncement()).willReturn(false);
                    given(oneseoRepository.findByGuardianOrTeacherPhoneNumberAndSubmitCode(phoneNumber, oneseoCode))
                            .willReturn(Optional.of(oneseo));
                    doNothing().when(commonCodeService).validateAndDelete(anyLong(), anyString(), anyString(), any());

                    FoundTestResultResDto result = queryTestResultService.execute(memberId, code, phoneNumber, oneseoCode, FIRST);

                    assertEquals("홍길동", result.name());
                    assertEquals(YesNo.YES, result.firstTestPassYn());
                    assertNull(result.secondTestPassYn());
                    assertNull(result.decidedMajor());
                }
            }

            @Nested
            @DisplayName("발표 시간이 되지 않았으면")
            class Context_with_invalid_announcement_time {

                @Test
                @DisplayName("ExpectedException을 던진다")
                void it_throws_expected_exception() {
                    given(oneseoService.validateFirstTestResultAnnouncement()).willReturn(true);

                    ExpectedException exception = assertThrows(ExpectedException.class, 
                            () -> queryTestResultService.execute(memberId, code, phoneNumber, oneseoCode, FIRST));

                    assertEquals("1차 전형 결과 발표 전 입니다.", exception.getMessage());
                    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
                }
            }

            @Nested
            @DisplayName("해당 전화번호와 접수번호로 원서를 찾을 수 없으면")
            class Context_with_non_existing_oneseo {

                @Test
                @DisplayName("ExpectedException을 던진다")
                void it_throws_expected_exception() {
                    given(oneseoService.validateFirstTestResultAnnouncement()).willReturn(false);
                    given(oneseoRepository.findByGuardianOrTeacherPhoneNumberAndSubmitCode(phoneNumber, oneseoCode))
                            .willReturn(Optional.empty());

                    ExpectedException exception = assertThrows(ExpectedException.class, 
                            () -> queryTestResultService.execute(memberId, code, phoneNumber, oneseoCode, FIRST));

                    assertEquals("해당 전화번호, 접수번호로 작성된 원서를 찾을 수 없습니다.", exception.getMessage());
                    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
                }
            }
        }

        @Nested
        @DisplayName("2차 전형 결과 조회 시")
        class Context_with_second_test_type {

            @Nested
            @DisplayName("발표 시간이 되었고 유효한 정보가 주어지면")
            class Context_with_valid_announcement_time_and_info {

                @Test
                @DisplayName("2차 전형 결과를 반환한다")
                void it_returns_second_test_result() {
                    Member member = buildMember("김철수");
                    Oneseo oneseo = buildSecondTestOneseo(member);

                    given(oneseoService.validateSecondTestResultAnnouncement()).willReturn(false);
                    given(oneseoRepository.findByGuardianOrTeacherPhoneNumberAndExaminationNumber(phoneNumber, examinationNumber))
                            .willReturn(Optional.of(oneseo));
                    doNothing().when(commonCodeService).validateAndDelete(anyLong(), anyString(), anyString(), any());

                    FoundTestResultResDto result = queryTestResultService.execute(memberId, code, phoneNumber, examinationNumber, SECOND);

                    assertEquals("김철수", result.name());
                    assertEquals(YesNo.YES, result.firstTestPassYn());
                    assertEquals(YesNo.YES, result.secondTestPassYn());
                    assertEquals(Major.SW, result.decidedMajor());
                }
            }

            @Nested
            @DisplayName("발표 시간이 되지 않았으면")
            class Context_with_invalid_announcement_time {

                @Test
                @DisplayName("ExpectedException을 던진다")
                void it_throws_expected_exception() {
                    given(oneseoService.validateSecondTestResultAnnouncement()).willReturn(true);

                    ExpectedException exception = assertThrows(ExpectedException.class, 
                            () -> queryTestResultService.execute(memberId, code, phoneNumber, examinationNumber, SECOND));

                    assertEquals("2차 전형 결과 발표 전 입니다.", exception.getMessage());
                    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
                }
            }

            @Nested
            @DisplayName("해당 전화번호와 수험번호로 원서를 찾을 수 없으면")
            class Context_with_non_existing_oneseo {

                @Test
                @DisplayName("ExpectedException을 던진다.")
                void it_throws_expected_exception() {
                    given(oneseoService.validateSecondTestResultAnnouncement()).willReturn(false);
                    given(oneseoRepository.findByGuardianOrTeacherPhoneNumberAndExaminationNumber(phoneNumber, examinationNumber))
                            .willReturn(Optional.empty());

                    ExpectedException exception = assertThrows(ExpectedException.class, 
                            () -> queryTestResultService.execute(memberId, code, phoneNumber, examinationNumber, SECOND));

                    assertEquals("해당 전화번호, 수험번호로 작성된 원서를 찾을 수 없습니다.", exception.getMessage());
                    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
                }
            }
        }
    }
}