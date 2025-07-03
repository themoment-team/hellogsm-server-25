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
import team.themoment.hellogsmv3.domain.common.testResult.type.TestType;
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

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Nested
    @DisplayName("execute 메소드는")
    class Describe_execute {

        private final Long memberId = 1L;
        private final String code = "123456";
        private final String phoneNumber = "01012345678";
        private final String oneseoCode = "2024001";
        private final String examinationNumber = "TEST001";

        @Nested
        @DisplayName("1차 전형 결과 조회 시")
        class Context_with_first_test_type {

            private Member member;
            private Oneseo oneseo;
            private final TestType testType = FIRST;

            @BeforeEach
            void setUp() {
                member = Member.builder()
                        .id(memberId)
                        .name("홍길동")
                        .birth(LocalDate.of(2009, 1, 1))
                        .phoneNumber(phoneNumber)
                        .sex(Sex.MALE)
                        .build();
                
                oneseo = Oneseo.builder()
                        .id(1L)
                        .member(member)
                        .entranceTestResult(
                                EntranceTestResult.builder()
                                        .id(1L)
                                        .firstTestPassYn(YesNo.YES)
                                        .secondTestPassYn(YesNo.NO)
                                        .build()
                        )
                        .build();
            }

            @Nested
            @DisplayName("발표 시간이 되었고 유효한 정보가 주어지면")
            class Context_with_valid_announcement_time_and_info {

                @BeforeEach
                void setUp() {
                    given(oneseoService.validateFirstTestResultAnnouncement()).willReturn(false);
                    given(oneseoRepository.findByGuardianOrTeacherPhoneNumberAndSubmitCode(phoneNumber, oneseoCode))
                            .willReturn(Optional.of(oneseo));
                    doNothing().when(commonCodeService).validateAndDelete(anyLong(), anyString(), anyString(), any());
                }

                @Test
                @DisplayName("1차 전형 결과를 반환한다")
                void it_returns_first_test_result() {
                    FoundTestResultResDto result = queryTestResultService.execute(memberId, code, phoneNumber, oneseoCode, testType);

                    assertEquals("홍길동", result.name());
                    assertEquals(YesNo.YES, result.firstTestPassYn());
                    assertNull(result.secondTestPassYn());
                    assertNull(result.decidedMajor());
                }
            }

            @Nested
            @DisplayName("발표 시간이 되지 않았으면")
            class Context_with_invalid_announcement_time {

                @BeforeEach
                void setUp() {
                    given(oneseoService.validateFirstTestResultAnnouncement()).willReturn(true);
                }

                @Test
                @DisplayName("ExpectedException을 던진다")
                void it_throws_expected_exception() {
                    ExpectedException exception = assertThrows(ExpectedException.class, 
                            () -> queryTestResultService.execute(memberId, code, phoneNumber, oneseoCode, testType));

                    assertEquals("1차 전형 결과 발표 전 입니다.", exception.getMessage());
                    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
                }
            }

            @Nested
            @DisplayName("해당 전화번호와 접수번호로 원서를 찾을 수 없으면")
            class Context_with_non_existing_oneseo {

                @BeforeEach
                void setUp() {
                    given(oneseoService.validateFirstTestResultAnnouncement()).willReturn(false);
                    given(oneseoRepository.findByGuardianOrTeacherPhoneNumberAndSubmitCode(phoneNumber, oneseoCode))
                            .willReturn(Optional.empty());
                }

                @Test
                @DisplayName("ExpectedException을 던진다")
                void it_throws_expected_exception() {
                    ExpectedException exception = assertThrows(ExpectedException.class, 
                            () -> queryTestResultService.execute(memberId, code, phoneNumber, oneseoCode, testType));

                    assertEquals("해당 전화번호, 접수번호로 작성된 원서를 찾을 수 없습니다.", exception.getMessage());
                    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
                }
            }
        }

        @Nested
        @DisplayName("2차 전형 결과 조회 시")
        class Context_with_second_test_type {

            private Member member;
            private Oneseo oneseo;
            private final TestType testType = SECOND;

            @BeforeEach
            void setUp() {
                member = Member.builder()
                        .id(memberId)
                        .name("김철수")
                        .birth(LocalDate.of(2009, 1, 1))
                        .phoneNumber(phoneNumber)
                        .sex(Sex.MALE)
                        .build();
                
                oneseo = Oneseo.builder()
                        .id(1L)
                        .member(member)
                        .decidedMajor(Major.SW)
                        .entranceTestResult(
                                EntranceTestResult.builder()
                                        .id(1L)
                                        .firstTestPassYn(YesNo.YES)
                                        .secondTestPassYn(YesNo.YES)
                                        .build()
                        )
                        .build();
            }

            @Nested
            @DisplayName("발표 시간이 되었고 유효한 정보가 주어지면")
            class Context_with_valid_announcement_time_and_info {

                @BeforeEach
                void setUp() {
                    given(oneseoService.validateSecondTestResultAnnouncement()).willReturn(false);
                    given(oneseoRepository.findByGuardianOrTeacherPhoneNumberAndExaminationNumber(phoneNumber, examinationNumber))
                            .willReturn(Optional.of(oneseo));
                    doNothing().when(commonCodeService).validateAndDelete(anyLong(), anyString(), anyString(), any());
                }

                @Test
                @DisplayName("2차 전형 결과를 반환한다")
                void it_returns_second_test_result() {
                    FoundTestResultResDto result = queryTestResultService.execute(memberId, code, phoneNumber, examinationNumber, testType);

                    assertEquals("김철수", result.name());
                    assertEquals(YesNo.YES, result.firstTestPassYn());
                    assertEquals(YesNo.YES, result.secondTestPassYn());
                    assertEquals(Major.SW, result.decidedMajor());
                }
            }

            @Nested
            @DisplayName("발표 시간이 되지 않았으면")
            class Context_with_invalid_announcement_time {

                @BeforeEach
                void setUp() {
                    given(oneseoService.validateSecondTestResultAnnouncement()).willReturn(true);
                }

                @Test
                @DisplayName("ExpectedException을 던진다")
                void it_throws_expected_exception() {
                    ExpectedException exception = assertThrows(ExpectedException.class, 
                            () -> queryTestResultService.execute(memberId, code, phoneNumber, examinationNumber, testType));

                    assertEquals("2차 전형 결과 발표 전 입니다.", exception.getMessage());
                    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
                }
            }

            @Nested
            @DisplayName("해당 전화번호와 수험번호로 원서를 찾을 수 없으면")
            class Context_with_non_existing_oneseo {

                @BeforeEach
                void setUp() {
                    given(oneseoService.validateSecondTestResultAnnouncement()).willReturn(false);
                    given(oneseoRepository.findByGuardianOrTeacherPhoneNumberAndExaminationNumber(phoneNumber, examinationNumber))
                            .willReturn(Optional.empty());
                }

                @Test
                @DisplayName("ExpectedException을 던진다.")
                void it_throws_expected_exception() {
                    ExpectedException exception = assertThrows(ExpectedException.class, 
                            () -> queryTestResultService.execute(memberId, code, phoneNumber, examinationNumber, testType));

                    assertEquals("해당 전화번호, 수험번호로 작성된 원서를 찾을 수 없습니다.", exception.getMessage());
                    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
                }
            }
        }
    }
}
