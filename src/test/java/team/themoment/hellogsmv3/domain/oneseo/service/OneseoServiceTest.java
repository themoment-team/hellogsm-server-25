package team.themoment.hellogsmv3.domain.oneseo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import team.themoment.hellogsmv3.domain.member.entity.Member;
import team.themoment.hellogsmv3.domain.oneseo.dto.request.OneseoReqDto;
import team.themoment.hellogsmv3.domain.oneseo.entity.Oneseo;
import team.themoment.hellogsmv3.domain.oneseo.repository.OneseoRepository;
import team.themoment.hellogsmv3.global.exception.error.ExpectedException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static team.themoment.hellogsmv3.domain.oneseo.entity.type.GraduationType.CANDIDATE;
import static team.themoment.hellogsmv3.domain.oneseo.entity.type.GraduationType.GED;
import static team.themoment.hellogsmv3.domain.oneseo.entity.type.Screening.*;

@DisplayName("OneseoService 클래스의")
public class OneseoServiceTest {

    @Mock
    private OneseoRepository oneseoRepository;

    @InjectMocks
    private OneseoService oneseoService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Nested
    @DisplayName("findByMemberOrThrow 메소드는")
    class Describe_findByMemberOrThrow {

        private final Long memberId = 1L;
        private final Member member = Member.builder().id(memberId).build();
        private final Oneseo oneseo = Oneseo.builder().member(member).build();

        @Nested
        @DisplayName("존재하는 회원과 원서가 주어지면")
        class Context_with_existing_member_and_oneseo {

            @BeforeEach
            void setUp() {
                given(oneseoRepository.findByMember(member)).willReturn(Optional.of(oneseo));
            }

            @Test
            @DisplayName("Oneseo 객체를 반환한다.")
            void it_returns_oneseo() {
                Oneseo foundOneseo = oneseoService.findByMemberOrThrow(member);
                assertEquals(oneseo, foundOneseo);
            }
        }

        @Nested
        @DisplayName("존재하지 않는 원서가 주어지면")
        class Context_with_non_existing_oneseo {

            @BeforeEach
            void setUp() {
                given(oneseoRepository.findByMember(member)).willReturn(Optional.empty());
            }

            @Test
            @DisplayName("ExpectedException을 던진다.")
            void it_throws_expected_exception() {
                ExpectedException exception = assertThrows(ExpectedException.class, () ->
                        oneseoService.findByMemberOrThrow(member)
                );

                assertEquals("해당 지원자의 원서를 찾을 수 없습니다. member ID: " + memberId, exception.getMessage());
                assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
            }
        }
    }

    @Nested
    @DisplayName("calcAbsentDaysCount 메소드는")
    class Describe_calcAbsentDaysCount {

        @Nested
        @DisplayName("결석 횟수 list와 지각, 조퇴, 결과 횟수 list를 보내면")
        class Context_with_absent_attendance_days {

            List<Integer> absentDays = List.of(3, 0, 0);
            List<Integer> attendanceDays = List.of(0, 0, 0, 1, 0, 1, 0, 2, 2);

            @Test
            @DisplayName("환산일수를 반환한다.")
            void it_returns_oneseo() {
                Integer absentDaysCount = OneseoService.calcAbsentDaysCount(absentDays, attendanceDays);
                assertEquals(absentDaysCount, 5);
            }
        }

        @Nested
        @DisplayName("null 값이 결석 횟수 list와 지각, 조퇴, 결과 횟수 list를 보내면")
        class Context_with_null_absent_attendance_days {

            List<Integer> nullAbsentDays = null;
            List<Integer> nullAttendanceDays = null;

            @Test
            @DisplayName("null 값을 반환한다.")
            void it_returns_oneseo() {
                Integer absentDaysCount = OneseoService.calcAbsentDaysCount(nullAbsentDays, nullAttendanceDays);
                assertNull(absentDaysCount);
            }
        }

        @Nested
        @DisplayName("결석 횟수 list나 지각, 조퇴, 결과 횟수 list에 null 값이 포함되어 있으면")
        class Context_with_null_in_absent_attendance_days_list {

            List<Integer> nullInAbsentDays = Arrays.asList(3, 0, null);
            List<Integer> nullInAttendanceDays = Arrays.asList(0, 0, 0, 1, 0, 1, null, 2, 2);

            @Test
            @DisplayName("null 값을 반환한다.")
            void it_returns_oneseo() {
                Integer absentDaysCount = OneseoService.calcAbsentDaysCount(nullInAbsentDays, nullInAttendanceDays);
                assertNull(absentDaysCount);
            }
        }
    }

    @Nested
    @DisplayName("isValidMiddleSchoolInfo 메소드는")
    class Describe_isValidMiddleSchoolInfo {

        private final OneseoReqDto validCandidateReqDto = OneseoReqDto.builder()
                .schoolName("금호중앙중학교")
                .schoolAddress("어딘가")
                .schoolTeacherName("김선생")
                .schoolTeacherPhoneNumber("01000000000")
                .graduationType(CANDIDATE)
                .build();

        private final OneseoReqDto validGedReqDto = OneseoReqDto.builder()
                .schoolName("")
                .schoolAddress("")
                .schoolTeacherName("")
                .schoolTeacherPhoneNumber("")
                .graduationType(GED)
                .build();

        private final OneseoReqDto invalidCandidateReqDto = OneseoReqDto.builder()
                .schoolName("")
                .schoolAddress("")
                .schoolTeacherName("")
                .schoolTeacherPhoneNumber("")
                .graduationType(CANDIDATE)
                .build();

        @Nested
        @DisplayName("졸업 예정자(CANDIDATE)이고 모든 중학교 정보가 유효하면")
        class Context_with_valid_candidate {

            @Test
            @DisplayName("예외를 던지지 않는다.")
            void it_does_not_throw_exception() {
                OneseoService.isValidMiddleSchoolInfo(validCandidateReqDto);
            }
        }

        @Nested
        @DisplayName("검정고시 지원자(GED)이고 중학교 정보를 작성하지 않았다면")
        class Context_with_valid_ged {

            @Test
            @DisplayName("예외를 던지지 않는다.")
            void it_does_not_throw_exception() {
                OneseoService.isValidMiddleSchoolInfo(validGedReqDto);
            }
        }

        @Nested
        @DisplayName("졸업 예정자(CANDIDATE)이고 필수 중학교 정보가 비어 있으면")
        class Context_with_invalid_candidate {

            @Test
            @DisplayName("ExpectedException을 던진다.")
            void it_throws_expected_exception() {
                ExpectedException exception = assertThrows(ExpectedException.class, () ->
                        OneseoService.isValidMiddleSchoolInfo(invalidCandidateReqDto)
                );

                assertEquals("중학교 졸업예정인 지원자는 현재 재학 중인 중학교 정보를 필수로 입력해야 합니다.", exception.getMessage());
                assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            }
        }
    }

    @Nested
    @DisplayName("assignSubmitCode 메소드는")
    class Describe_assignSubmitCode {

        private final int maxSubmitCodeNumber = 10;
        private final Oneseo oneseo = mock(Oneseo.class);

        @Nested
        @DisplayName("일반전형 원서가 주어지면")
        class Context_with_existing_member_and_oneseo {

            @BeforeEach
            void setUp() {
                given(oneseo.getWantedScreening()).willReturn(GENERAL);
                given(oneseoRepository.findMaxSubmitCodeByScreening(oneseo.getWantedScreening())).willReturn(maxSubmitCodeNumber);
            }

            @Test
            @DisplayName("A-N 번대의 접수번호가 생성된다.")
            void it_returns_oneseo() {
                oneseoService.assignSubmitCode(oneseo, null);
                verify(oneseo).setOneseoSubmitCode("A-" + (maxSubmitCodeNumber + 1));
            }
        }
    }

}
