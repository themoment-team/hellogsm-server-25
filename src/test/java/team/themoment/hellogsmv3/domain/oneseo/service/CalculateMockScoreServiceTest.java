//package team.themoment.hellogsmv3.domain.oneseo.service;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.MockitoAnnotations;
//import team.themoment.hellogsmv3.domain.oneseo.dto.internal.MiddleSchoolAchievementCalcDto;
//import team.themoment.hellogsmv3.domain.oneseo.dto.request.MiddleSchoolAchievementReqDto;
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.verify;
//import static team.themoment.hellogsmv3.domain.oneseo.entity.type.GraduationType.*;
//
//@DisplayName("CalculateMockScoreService 클래스의")
//class CalculateMockScoreServiceTest {
//
//    @Mock
//    private OneseoService oneseoService;
//
//    @Mock
//    private CalculateGradeService calculateGradeService;
//
//    @Mock
//    private CalculateGedService calculateGedService;
//
//    @InjectMocks
//    private CalculateMockScoreService calculateMockScoreService;
//
//    @BeforeEach
//    void setUp() {
//        MockitoAnnotations.openMocks(this);
//    }
//
//    @Nested
//    @DisplayName("execute 메서드는")
//    class Describe_execute {
//
//        @Nested
//        @DisplayName("유효한 성적 데이터가 주어지면")
//        class Context_with_valid_candidate_achievement {
//
//            MiddleSchoolAchievementReqDto reqDto = mock(MiddleSchoolAchievementReqDto.class);
//
//            @Test
//            @DisplayName("졸업예정자라면 CalculateGradeService 클래스의 성적환산 메서드를 호출한다.")
//            void it_candidate_calculate_score() {
//                MiddleSchoolAchievementCalcDto calcDto = oneseoService.buildCalcDtoWithFillEmpty(reqDto, CANDIDATE);
//
//                calculateMockScoreService.execute(reqDto, CANDIDATE);
//
//                verify(calculateGradeService).execute(calcDto, null, CANDIDATE);
//            }
//
//            @Test
//            @DisplayName("졸업자라면 CalculateGradeService 클래스의 성적환산 메서드를 호출한다.")
//            void it_graduate_calculate_score() {
//                MiddleSchoolAchievementCalcDto calcDto = oneseoService.buildCalcDtoWithFillEmpty(reqDto, GRADUATE);
//
//                calculateMockScoreService.execute(reqDto, GRADUATE);
//
//                verify(calculateGradeService).execute(calcDto, null, GRADUATE);
//            }
//
//            @Test
//            @DisplayName("검정고시 응시자라면 CalculateGedService 클래스의 성적환산 메서드를 호출한다.")
//            void it_ged_calculate_score() {
//                MiddleSchoolAchievementCalcDto calcDto = oneseoService.buildCalcDtoWithFillEmpty(reqDto, GED);
//
//                calculateMockScoreService.execute(reqDto, GED);
//
//                verify(calculateGedService).execute(calcDto, null, GED);
//            }
//        }
//    }
//}
