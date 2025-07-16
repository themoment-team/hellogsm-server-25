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
import team.themoment.hellogsmv3.domain.oneseo.entity.EntranceTestFactorsDetail;
import team.themoment.hellogsmv3.domain.oneseo.entity.EntranceTestResult;
import team.themoment.hellogsmv3.domain.oneseo.entity.Oneseo;
import team.themoment.hellogsmv3.domain.oneseo.repository.EntranceTestFactorsDetailRepository;
import team.themoment.hellogsmv3.domain.oneseo.repository.EntranceTestResultRepository;

import java.math.BigDecimal;
import java.util.List;

import static java.math.RoundingMode.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static team.themoment.hellogsmv3.domain.oneseo.entity.type.GraduationType.CANDIDATE;
import static team.themoment.hellogsmv3.domain.oneseo.entity.type.GraduationType.GED;

@DisplayName("CalculateGedService 클래스의")
class CalculateGedServiceTest {

    @Mock
    private EntranceTestResultRepository entranceTestResultRepository;

    @Mock
    private EntranceTestFactorsDetailRepository entranceTestFactorsDetailRepository;

    @InjectMocks
    private CalculateGedService calculateGedService;

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
                        .gedAvgScore(BigDecimal.valueOf(80))
                        .build();

                calculateGedService.execute(reqDto, oneseo, GED);

                verify(entranceTestFactorsDetailRepository).save(any(EntranceTestFactorsDetail.class));
                verify(entranceTestResultRepository).save(any(EntranceTestResult.class));
            }

            @Test
            @DisplayName("graduationType이 GED라면 올바른 내신 성적을 계산하고 결과를 저장한다")
            void it_ged_calculates_and_save_results() {
                reqDto = MiddleSchoolAchievementReqDto.builder()
                        .gedAvgScore(BigDecimal.valueOf(89.485))
                        .build();

                calculateGedService.execute(reqDto, oneseo, GED);

                ArgumentCaptor<EntranceTestFactorsDetail> entranceTestFactorsDetailArgumentCaptor = ArgumentCaptor.forClass(EntranceTestFactorsDetail.class);
                ArgumentCaptor<EntranceTestResult> entranceTestResultArgumentCaptor = ArgumentCaptor.forClass(EntranceTestResult.class);

                verify(entranceTestFactorsDetailRepository).save(entranceTestFactorsDetailArgumentCaptor.capture());
                verify(entranceTestResultRepository).save(entranceTestResultArgumentCaptor.capture());

                EntranceTestFactorsDetail capturedEntranceTestFactorsDetailArgument = entranceTestFactorsDetailArgumentCaptor.getValue();
                EntranceTestResult capturedEntranceTestResult = entranceTestResultArgumentCaptor.getValue();

                assertEquals(BigDecimal.valueOf(189.528).setScale(3, UP), capturedEntranceTestFactorsDetailArgument.getTotalSubjectsScore());
                assertEquals(BigDecimal.valueOf(30).setScale(3, UP), capturedEntranceTestFactorsDetailArgument.getAttendanceScore().setScale(3, UP));
                assertEquals(BigDecimal.valueOf(24.743).setScale(3, UP), capturedEntranceTestFactorsDetailArgument.getVolunteerScore());
                assertEquals(BigDecimal.valueOf(54.743).setScale(3, UP), capturedEntranceTestFactorsDetailArgument.getTotalNonSubjectsScore());

                assertEquals(BigDecimal.valueOf(244.271).setScale(3, UP), capturedEntranceTestResult.getDocumentEvaluationScore());
            }

        }

        @Nested
        @DisplayName("유효하지 않은 graduationType이 주어지면")
        class Context_with_invalid_graduationType {

            @Test
            @DisplayName("예외를 던진다")
            void it_throw_exception() {
                reqDto = MiddleSchoolAchievementReqDto.builder()
                        .gedAvgScore(BigDecimal.valueOf(90))
                        .build();

                IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () ->
                        calculateGedService.execute(reqDto, oneseo, CANDIDATE));

                assertEquals("올바르지 않은 graduationType입니다.", illegalArgumentException.getMessage());
            }
        }
    }
}
