package team.themoment.hellogsmv3.domain.oneseo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import team.themoment.hellogsmv3.domain.oneseo.dto.request.MiddleSchoolAchievementReqDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.CalculatedScoreResDto;
import team.themoment.hellogsmv3.domain.oneseo.entity.Oneseo;
import team.themoment.hellogsmv3.domain.oneseo.entity.EntranceTestFactorsDetail;
import team.themoment.hellogsmv3.domain.oneseo.entity.EntranceTestResult;
import team.themoment.hellogsmv3.domain.oneseo.repository.EntranceTestFactorsDetailRepository;
import team.themoment.hellogsmv3.domain.oneseo.repository.EntranceTestResultRepository;

import java.math.BigDecimal;
import java.util.Arrays;

import static java.math.RoundingMode.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static team.themoment.hellogsmv3.domain.oneseo.entity.type.GraduationType.*;

@DisplayName("CalculateGradeService 클래스의")
class CalculateGradeServiceTest {

    @Mock
    private EntranceTestResultRepository entranceTestResultRepository;

    @Mock
    private EntranceTestFactorsDetailRepository entranceTestFactorsDetailRepository;

    @InjectMocks
    private CalculateGradeService calculateGradeService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Nested
    @DisplayName("execute 메소드는")
    class Describe_execute {

        private MiddleSchoolAchievementReqDto reqDto;
        private Oneseo oneseo;

        @BeforeEach
        void setUp() {
            oneseo = mock(Oneseo.class);
        }

        @Nested
        @DisplayName("올바른 graduationType이 주어지면")
        class Context_with_valid_graduationType {

            @Test
            @DisplayName("성적을 계산하고 결과를 저장한다")
            void it_calculates_and_saves_results() {
                reqDto = MiddleSchoolAchievementReqDto.builder()
                        .achievement1_2(Arrays.asList(5, 5, 5, 5, 5, 5, 5, 5, 0))
                        .achievement2_1(Arrays.asList(5, 5, 5, 5, 5, 5, 5, 5, 4))
                        .achievement2_2(Arrays.asList(5, 5, 5, 5, 5, 5, 5, 5, 4))
                        .artsPhysicalAchievement(Arrays.asList(5, 5, 5, 5, 5, 5, 5, 5, 5))
                        .absentDays(Arrays.asList(3, 0, 0))
                        .attendanceDays(Arrays.asList(0, 0, 0, 0, 0, 0, 0, 0, 0))
                        .volunteerTime(Arrays.asList(10, 0, 2))
                        .liberalSystem("자유학기제")
                        .freeSemester("3-1")
                        .build();

                calculateGradeService.execute(reqDto, oneseo, CANDIDATE);

                verify(entranceTestFactorsDetailRepository).save(any(EntranceTestFactorsDetail.class));
                verify(entranceTestResultRepository).save(any(EntranceTestResult.class));
            }

            @Test
            @DisplayName("graduationType이 CANDIDATE라면 올바른 내신 성적을 계산하고 결과를 저장한다")
            void it_candidate_calculates_and_save_results() {
                reqDto = MiddleSchoolAchievementReqDto.builder()
                        .achievement1_2(Arrays.asList(5, 5, 5, 5, 5, 5, 5, 5, 0))
                        .achievement2_1(Arrays.asList(5, 5, 5, 5, 5, 5, 5, 5, 4))
                        .achievement2_2(Arrays.asList(5, 5, 5, 5, 5, 5, 5, 5, 4))
                        .artsPhysicalAchievement(Arrays.asList(5, 5, 5, 5, 0, 5, 0, 5, 0))
                        .absentDays(Arrays.asList(3, 0, 0))
                        .attendanceDays(Arrays.asList(0, 0, 0, 0, 0, 0, 0, 0, 0))
                        .volunteerTime(Arrays.asList(10, 0, 2))
                        .liberalSystem("자유학기제")
                        .freeSemester("3-1")
                        .build();

                CalculatedScoreResDto resDto = calculateGradeService.execute(reqDto, oneseo, CANDIDATE);

                ArgumentCaptor<EntranceTestFactorsDetail> entranceTestFactorsDetailArgumentCaptor = ArgumentCaptor.forClass(EntranceTestFactorsDetail.class);
                ArgumentCaptor<EntranceTestResult> entranceTestResultArgumentCaptor = ArgumentCaptor.forClass(EntranceTestResult.class);

                verify(entranceTestFactorsDetailRepository).save(entranceTestFactorsDetailArgumentCaptor.capture());
                verify(entranceTestResultRepository).save(entranceTestResultArgumentCaptor.capture());

                EntranceTestFactorsDetail capturedEntranceTestFactorsDetailArgument = entranceTestFactorsDetailArgumentCaptor.getValue();
                EntranceTestResult capturedEntranceTestResult = entranceTestResultArgumentCaptor.getValue();

                assertEquals(BigDecimal.valueOf(177.2).setScale(3, UP), capturedEntranceTestFactorsDetailArgument.getGeneralSubjectsScore());
                assertEquals(BigDecimal.valueOf(60).setScale(3, UP), capturedEntranceTestFactorsDetailArgument.getArtsPhysicalSubjectsScore());
                assertEquals(BigDecimal.valueOf(237.2).setScale(3, UP), capturedEntranceTestFactorsDetailArgument.getTotalSubjectsScore());
                assertEquals(BigDecimal.valueOf(21).setScale(3, UP), capturedEntranceTestFactorsDetailArgument.getAttendanceScore());
                assertEquals(BigDecimal.valueOf(14).setScale(3, UP), capturedEntranceTestFactorsDetailArgument.getVolunteerScore());
                assertEquals(BigDecimal.valueOf(35).setScale(3, UP), capturedEntranceTestFactorsDetailArgument.getTotalNonSubjectsScore());
                assertEquals(BigDecimal.valueOf(54).setScale(3, UP), capturedEntranceTestFactorsDetailArgument.getScore1_2());
                assertEquals(BigDecimal.valueOf(52.8).setScale(3, UP), capturedEntranceTestFactorsDetailArgument.getScore2_1());
                assertEquals(BigDecimal.valueOf(70.4).setScale(3, UP), capturedEntranceTestFactorsDetailArgument.getScore2_2());
                assertEquals(BigDecimal.valueOf(0.0).setScale(3, UP), capturedEntranceTestFactorsDetailArgument.getScore3_1().setScale(3, UP));
                assertEquals(BigDecimal.valueOf(0.0).setScale(3, UP), capturedEntranceTestFactorsDetailArgument.getScore3_2().setScale(3, UP));

                assertEquals(BigDecimal.valueOf(272.2).setScale(3, UP), capturedEntranceTestResult.getDocumentEvaluationScore());

                assertEquals(resDto.artsPhysicalSubjectsScoreDetail().score1_2(), BigDecimal.valueOf(30).setScale(3, HALF_UP));
                assertEquals(resDto.artsPhysicalSubjectsScoreDetail().score2_1(), BigDecimal.valueOf(20).setScale(3, HALF_UP));
                assertEquals(resDto.artsPhysicalSubjectsScoreDetail().score2_2(), BigDecimal.valueOf(10).setScale(3, HALF_UP));
            }

            @Test
            @DisplayName("graduationType이 GRADUATE라면 올바른 내신 성적을 계산하고 결과를 저장한다")
            void it_graduate_calculates_and_save_results() {
                reqDto = MiddleSchoolAchievementReqDto.builder()
                        .achievement1_2(Arrays.asList(5, 5, 5, 5, 5, 5, 5, 5, 0))
                        .achievement2_2(Arrays.asList(5, 5, 5, 5, 5, 5, 5, 5, 0))
                        .achievement3_1(Arrays.asList(5, 5, 5, 5, 5, 5, 5, 5, 5))
                        .achievement3_2(Arrays.asList(5, 5, 5, 5, 5, 5, 5, 5, 5))
                        .artsPhysicalAchievement(Arrays.asList(5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5))
                        .absentDays(Arrays.asList(3, 0, 0))
                        .attendanceDays(Arrays.asList(0, 0, 0, 0, 0, 0, 0, 0, 0))
                        .volunteerTime(Arrays.asList(10, 0, 10))
                        .liberalSystem("자유학기제")
                        .freeSemester("2-1")
                        .build();

                CalculatedScoreResDto resDto = calculateGradeService.execute(reqDto, oneseo, GRADUATE);

                ArgumentCaptor<EntranceTestFactorsDetail> entranceTestFactorsDetailArgumentCaptor = ArgumentCaptor.forClass(EntranceTestFactorsDetail.class);
                ArgumentCaptor<EntranceTestResult> entranceTestResultArgumentCaptor = ArgumentCaptor.forClass(EntranceTestResult.class);

                verify(entranceTestFactorsDetailRepository).save(entranceTestFactorsDetailArgumentCaptor.capture());
                verify(entranceTestResultRepository).save(entranceTestResultArgumentCaptor.capture());

                EntranceTestFactorsDetail capturedEntranceTestFactorsDetailArgument = entranceTestFactorsDetailArgumentCaptor.getValue();
                EntranceTestResult capturedEntranceTestResult = entranceTestResultArgumentCaptor.getValue();

                assertEquals(BigDecimal.valueOf(180).setScale(3, UP), capturedEntranceTestFactorsDetailArgument.getGeneralSubjectsScore());
                assertEquals(BigDecimal.valueOf(60).setScale(3, UP), capturedEntranceTestFactorsDetailArgument.getArtsPhysicalSubjectsScore());
                assertEquals(BigDecimal.valueOf(240).setScale(3, UP), capturedEntranceTestFactorsDetailArgument.getTotalSubjectsScore());
                assertEquals(BigDecimal.valueOf(21).setScale(3, UP), capturedEntranceTestFactorsDetailArgument.getAttendanceScore());
                assertEquals(BigDecimal.valueOf(22).setScale(3, UP), capturedEntranceTestFactorsDetailArgument.getVolunteerScore());
                assertEquals(BigDecimal.valueOf(43).setScale(3, UP), capturedEntranceTestFactorsDetailArgument.getTotalNonSubjectsScore());
                assertEquals(BigDecimal.valueOf(36).setScale(3, UP), capturedEntranceTestFactorsDetailArgument.getScore1_2());
                assertEquals(BigDecimal.valueOf(0.0).setScale(3, UP), capturedEntranceTestFactorsDetailArgument.getScore2_1().setScale(3, UP));
                assertEquals(BigDecimal.valueOf(36).setScale(3, UP), capturedEntranceTestFactorsDetailArgument.getScore2_2());
                assertEquals(BigDecimal.valueOf(54).setScale(3, UP), capturedEntranceTestFactorsDetailArgument.getScore3_1().setScale(3, UP));
                assertEquals(BigDecimal.valueOf(54).setScale(3, UP), capturedEntranceTestFactorsDetailArgument.getScore3_2().setScale(3, UP));

                assertEquals(BigDecimal.valueOf(283).setScale(3, UP), capturedEntranceTestResult.getDocumentEvaluationScore());

                assertEquals(resDto.artsPhysicalSubjectsScoreDetail().score1_2(), BigDecimal.valueOf(15).setScale(3, HALF_UP));
                assertEquals(resDto.artsPhysicalSubjectsScoreDetail().score2_2(), BigDecimal.valueOf(15).setScale(3, HALF_UP));
                assertEquals(resDto.artsPhysicalSubjectsScoreDetail().score3_1(), BigDecimal.valueOf(15).setScale(3, HALF_UP));
                assertEquals(resDto.artsPhysicalSubjectsScoreDetail().score3_2(), BigDecimal.valueOf(15).setScale(3, HALF_UP));
            }
        }

        @Nested
        @DisplayName("유효하지 않은 graduationType이 주어지면")
        class Context_with_invalid_graduationType {

            @Test
            @DisplayName("예외를 던진다")
            void it_throw_exception() {
                reqDto = MiddleSchoolAchievementReqDto.builder()
                        .achievement1_2(Arrays.asList(5, 5, 5, 5, 5, 5, 5, 5, 0))
                        .achievement2_2(Arrays.asList(5, 5, 5, 5, 5, 5, 5, 5, 0))
                        .achievement3_1(Arrays.asList(5, 5, 5, 5, 5, 5, 5, 5, 5))
                        .achievement3_2(Arrays.asList(5, 5, 5, 5, 5, 5, 5, 5, 5))
                        .artsPhysicalAchievement(Arrays.asList(5, 5, 5, 5, 5, 5, 5, 5, 5))
                        .absentDays(Arrays.asList(3, 0, 0))
                        .attendanceDays(Arrays.asList(0, 0, 0, 0, 0, 0, 0, 0, 0))
                        .volunteerTime(Arrays.asList(10, 0, 10))
                        .liberalSystem("자유학기제")
                        .freeSemester("2-1")
                        .build();

                IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () ->
                        calculateGradeService.execute(reqDto, oneseo, GED));

                assertEquals("올바르지 않은 graduationType입니다.", illegalArgumentException.getMessage());
            }
        }
    }
}
