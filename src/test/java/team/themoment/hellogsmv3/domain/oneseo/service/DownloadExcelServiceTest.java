package team.themoment.hellogsmv3.domain.oneseo.service;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import team.themoment.hellogsmv3.domain.member.entity.Member;
import team.themoment.hellogsmv3.domain.member.entity.type.Sex;
import team.themoment.hellogsmv3.domain.oneseo.entity.*;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.*;
import team.themoment.hellogsmv3.domain.oneseo.repository.EntranceTestResultRepository;
import team.themoment.hellogsmv3.domain.oneseo.repository.OneseoPrivacyDetailRepository;
import team.themoment.hellogsmv3.domain.oneseo.repository.OneseoRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static team.themoment.hellogsmv3.domain.oneseo.entity.type.YesNo.*;

@DisplayName("DownloadExcelService 클래스의")
public class DownloadExcelServiceTest {

    @Mock
    private OneseoRepository oneseoRepository;

    @Mock
    private EntranceTestResultRepository entranceTestResultRepository;

    @Mock
    private OneseoPrivacyDetailRepository oneseoPrivacyDetailRepository;

    @InjectMocks
    private DownloadExcelService downloadExcelService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Nested
    @DisplayName("execute 메소드는")
    class Describe_execute {
        Oneseo oneseoGeneral;
        Oneseo oneseoSpecial;
        Oneseo oneseoExtra;
        Oneseo oneseoFallen;

        EntranceTestResult generalEntranceTestResult;
        EntranceTestResult specialEntranceTestResult;
        EntranceTestResult extraEntranceTestResult;
        EntranceTestResult fallenEntranceTestResult;

        OneseoPrivacyDetail generalPrivacyDetail;
        OneseoPrivacyDetail specialPrivacyDetail;
        OneseoPrivacyDetail extraPrivacyDetail;
        OneseoPrivacyDetail fallenPrivacyDetail;

        @BeforeEach
        void setUp() {
            Long generalId = 1L;
            Long specialId = 2L;
            Long extraId = 3L;
            Long fallenId = 4L;

            oneseoGeneral = createOneseo(generalId, Screening.GENERAL, "A-1");
            oneseoSpecial = createOneseo(specialId, Screening.SPECIAL, "B-2");
            oneseoExtra = createOneseo(extraId, Screening.EXTRA_VETERANS, "C-3");
            oneseoFallen = createOneseo(fallenId, Screening.GENERAL, "A-4");

            generalEntranceTestResult = createEntranceTestResult(generalId, oneseoGeneral, YES);
            specialEntranceTestResult = createEntranceTestResult(specialId, oneseoSpecial, YES);
            extraEntranceTestResult = createEntranceTestResult(extraId, oneseoExtra, YES);
            fallenEntranceTestResult = createEntranceTestResult(fallenId, oneseoFallen, NO);

            generalPrivacyDetail = createOneseoPrivacyDetail(generalId, oneseoGeneral);
            specialPrivacyDetail = createOneseoPrivacyDetail(specialId, oneseoSpecial);
            extraPrivacyDetail = createOneseoPrivacyDetail(extraId, oneseoExtra);
            fallenPrivacyDetail = createOneseoPrivacyDetail(fallenId, oneseoFallen);

            given(oneseoRepository.findAllByScreeningDynamic(Screening.GENERAL)).willReturn(List.of(oneseoGeneral));
            given(oneseoRepository.findAllByScreeningDynamic(Screening.SPECIAL)).willReturn(List.of(oneseoSpecial));
            given(oneseoRepository.findAllByScreeningDynamic(Screening.EXTRA_VETERANS)).willReturn(List.of(oneseoExtra));
            given(entranceTestResultRepository.findAllByFirstTestPassYnOrSecondTestPassYn(NO, NO)).willReturn(List.of(fallenEntranceTestResult));

            given(entranceTestResultRepository.findByOneseo(oneseoGeneral)).willReturn(generalEntranceTestResult);
            given(entranceTestResultRepository.findByOneseo(oneseoSpecial)).willReturn(specialEntranceTestResult);
            given(entranceTestResultRepository.findByOneseo(oneseoExtra)).willReturn(extraEntranceTestResult);
            given(entranceTestResultRepository.findByOneseo(oneseoFallen)).willReturn(fallenEntranceTestResult);

            given(oneseoPrivacyDetailRepository.findByOneseo(oneseoSpecial)).willReturn(specialPrivacyDetail);
            given(oneseoPrivacyDetailRepository.findByOneseo(oneseoGeneral)).willReturn(generalPrivacyDetail);
            given(oneseoPrivacyDetailRepository.findByOneseo(oneseoExtra)).willReturn(extraPrivacyDetail);
            given(oneseoPrivacyDetailRepository.findByOneseo(oneseoFallen)).willReturn(fallenPrivacyDetail);
        }

        @Test
        @DisplayName("모든 전형의 시트를 생성하고 데이터를 채운다.")
        void it_create_sheets_and_populate_data() {
            Workbook workbook = downloadExcelService.execute();

            assertNotNull(workbook);
            assertEquals(4, workbook.getNumberOfSheets());

            assertSheetData(workbook.getSheetAt(0), oneseoGeneral, generalEntranceTestResult, generalPrivacyDetail, 0);
            assertSheetData(workbook.getSheetAt(1), oneseoSpecial, specialEntranceTestResult, specialPrivacyDetail, 1);
            assertSheetData(workbook.getSheetAt(2), oneseoExtra, extraEntranceTestResult, extraPrivacyDetail, 2);
            assertSheetData(workbook.getSheetAt(3), oneseoFallen, fallenEntranceTestResult, fallenPrivacyDetail, 3);
        }

        private void assertSheetData(Sheet sheet, Oneseo oneseo, EntranceTestResult entranceTestResult, OneseoPrivacyDetail oneseoPrivacyDetail, int idx) {
            List<String> expectedHeader = List.of(
                    "순번", "접수번호", "성명", "1지망", "2지망", "3지망", "생년월일", "성별", "상세주소", "출신학교",
                    "학력", "초기전형", "적용되는 전형", "일반교과점수", "예체능점수", "출석점수", "봉사점수", "1차전형총점",
                    "역량평가점수", "심층면접점수", "최종점수", "최종학과", "지원자연락처", "보호자연락처", "담임연락처", "1차전형결과", "2차전형결과"
            );

            String wantedScreening = null;

            switch (oneseo.getWantedScreening()) {
                case GENERAL -> wantedScreening = "일반전형";
                case SPECIAL -> wantedScreening = "특별전형";
                case EXTRA_VETERANS -> wantedScreening = "국가보훈대상자";
                case EXTRA_ADMISSION -> wantedScreening = "특례입학대상자";
            }

            List<String> expectedData = Arrays.asList(
                    "1",
                    null,
                    String.valueOf(oneseo.getMember().getName()),
                    String.valueOf(oneseo.getDesiredMajors().getFirstDesiredMajor()),
                    String.valueOf(oneseo.getDesiredMajors().getSecondDesiredMajor()),
                    String.valueOf(oneseo.getDesiredMajors().getThirdDesiredMajor()),
                    String.valueOf(oneseo.getMember().getBirth()),
                    "남자",
                    (oneseoPrivacyDetail.getAddress() + oneseoPrivacyDetail.getDetailAddress()),
                    String.valueOf(oneseoPrivacyDetail.getSchoolName()),
                    "졸업자",
                    String.valueOf(wantedScreening),
                    "",
                    String.valueOf(entranceTestResult.getEntranceTestFactorsDetail().getGeneralSubjectsScore()),
                    String.valueOf(entranceTestResult.getEntranceTestFactorsDetail().getArtsPhysicalSubjectsScore()),
                    String.valueOf(entranceTestResult.getEntranceTestFactorsDetail().getAttendanceScore()),
                    String.valueOf(entranceTestResult.getEntranceTestFactorsDetail().getVolunteerScore()),
                    String.valueOf(entranceTestResult.getDocumentEvaluationScore()),
                    String.valueOf(entranceTestResult.getCompetencyEvaluationScore()),
                    String.valueOf(entranceTestResult.getInterviewScore()),
                    String.valueOf(46.334),
                    String.valueOf(oneseo.getDecidedMajor()),
                    String.valueOf(oneseo.getMember().getPhoneNumber()),
                    String.valueOf(oneseoPrivacyDetail.getGuardianPhoneNumber()),
                    String.valueOf(oneseoPrivacyDetail.getSchoolTeacherPhoneNumber()),
                    entranceTestResult.getFirstTestPassYn().equals(YES) ? "합격" : "불합격",
                    entranceTestResult.getSecondTestPassYn().equals(YES) ? "합격" : "불합격"
            );

            Row headerRow = sheet.getRow(0);
            for (int i = 0; i < expectedHeader.size(); i++) {
                Cell cell = headerRow.getCell(i);
                assertNotNull(cell);
                assertEquals(expectedHeader.get(i), cell.getStringCellValue());
            }

            Row dataRow = sheet.getRow(1);
            for (int i = 0; i < expectedData.size(); i++) {
                Cell cell = dataRow.getCell(i);
                assertNotNull(cell);
                if (i == 1) {
                    String submitCode =
                            idx == 0 ? "A-001" :
                            idx == 1 ? "B-002" :
                            idx == 2 ? "C-003" :
                            idx == 3 ? "A-004" : null;

                    assertEquals(submitCode, cell.getStringCellValue());
                } else {
                    assertEquals(expectedData.get(i), cell.getStringCellValue());
                }
            }
        }

        private Oneseo createOneseo(Long id, Screening screening, String submitCode) {
            Member member = Member.builder()
                    .id(id)
                    .name("홍길동")
                    .sex(Sex.MALE)
                    .birth(LocalDate.of(2024, 7, 31))
                    .phoneNumber("01012345678")
                    .build();

            DesiredMajors desiredMajors = DesiredMajors.builder()
                    .firstDesiredMajor(Major.AI)
                    .secondDesiredMajor(Major.SW)
                    .thirdDesiredMajor(Major.IOT)
                    .build();

            return Oneseo.builder()
                    .id(id)
                    .member(member)
                    .oneseoSubmitCode(submitCode)
                    .desiredMajors(desiredMajors)
                    .wantedScreening(screening)
                    .decidedMajor(Major.IOT)
                    .build();
        }

        private EntranceTestResult createEntranceTestResult(Long id, Oneseo oneseo, YesNo yn) {
            EntranceTestFactorsDetail factorsDetail = EntranceTestFactorsDetail.builder()
                    .id(id)
                    .generalSubjectsScore(BigDecimal.valueOf(80))
                    .artsPhysicalSubjectsScore(BigDecimal.valueOf(70))
                    .attendanceScore(BigDecimal.valueOf(60))
                    .volunteerScore(BigDecimal.valueOf(50))
                    .build();

            return EntranceTestResult.builder()
                    .id(id)
                    .oneseo(oneseo)
                    .entranceTestFactorsDetail(factorsDetail)
                    .documentEvaluationScore(BigDecimal.valueOf(80))
                    .firstTestPassYn(yn)
                    .secondTestPassYn(yn)
                    .competencyEvaluationScore(BigDecimal.valueOf(70))
                    .interviewScore(BigDecimal.valueOf(60))
                    .build();
        }

        private OneseoPrivacyDetail createOneseoPrivacyDetail(Long id, Oneseo oneseo) {
            return OneseoPrivacyDetail.builder()
                    .id(id)
                    .oneseo(oneseo)
                    .schoolName("광주소프트웨어마이스터고등학교")
                    .address("광주광역시 광산구 송정동 상무대로 312")
                    .detailAddress("동행관")
                    .guardianPhoneNumber("01087654321")
                    .schoolTeacherPhoneNumber("01012344321")
                    .graduationType(GraduationType.GRADUATE)
                    .build();
        }
    }
}
