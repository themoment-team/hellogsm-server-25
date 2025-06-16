package team.themoment.hellogsmv3.domain.oneseo.service;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Service;
import team.themoment.hellogsmv3.domain.oneseo.entity.*;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.Screening;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.YesNo;
import team.themoment.hellogsmv3.domain.oneseo.repository.EntranceTestResultRepository;
import team.themoment.hellogsmv3.domain.oneseo.repository.OneseoPrivacyDetailRepository;
import team.themoment.hellogsmv3.domain.oneseo.repository.OneseoRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static team.themoment.hellogsmv3.domain.oneseo.entity.type.Screening.*;
import static team.themoment.hellogsmv3.domain.oneseo.entity.type.YesNo.NO;

@Service
@RequiredArgsConstructor
public class DownloadExcelService {

    private final OneseoRepository oneseoRepository;
    private final EntranceTestResultRepository entranceTestResultRepository;
    private final OneseoPrivacyDetailRepository oneseoPrivacyDetailRepository;

    private static final List<String> HEADER_NAMES = List.of(
            "순번", "접수번호", "성명", "1지망", "2지망", "3지망", "생년월일", "성별", "상세주소", "출신학교",
            "학력", "초기전형", "적용되는 전형", "일반교과점수", "예체능점수", "출석점수", "봉사점수", "1차전형총점",
            "역량평가점수", "심층면접점수", "최종점수", "최종학과", "지원자연락처", "보호자연락처", "담임연락처", "1차전형결과", "2차전형결과"
    );

    public Workbook execute() {
        Workbook workbook = new SXSSFWorkbook();
        List<Sheet> sheets = createSheets(workbook);
        List<List<List<String>>> allSheetData = getAllSheetData();

        for (int i = 0; i < sheets.size(); i++) {
            Sheet sheet = sheets.get(i);
            List<List<String>> sheetData = allSheetData.get(i);
            populateSheet(sheet, sheetData);
        }

        return workbook;
    }

    private List<Sheet> createSheets(Workbook workbook) {
        return List.of(
                workbook.createSheet("일반전형"),
                workbook.createSheet("특별전형"),
                workbook.createSheet("정원외 특별전형"),
                workbook.createSheet("불합격")
        );
    }

    private void populateSheet(Sheet sheet, List<List<String>> data) {
        int rowCount = 0;
        Row headerRow = sheet.createRow(rowCount++);
        addDataToRow(headerRow, HEADER_NAMES);

        for (List<String> rowData : data) {
            Row row = sheet.createRow(rowCount++);
            addDataToRow(row, rowData);
        }
    }

    private void addDataToRow(Row row, List<String> dataList) {
        int cellCount = 0;
        for (String data : dataList) {
            Cell cell = row.createCell(cellCount++);
            cell.setCellValue(data);
        }
    }

    private List<List<List<String>>> getAllSheetData() {
        return List.of(
                getOneseoData(Screening.GENERAL),
                getOneseoData(Screening.SPECIAL),
                getCombinedExtraScreeningData(),
                getFallenData()
        );
    }

    private List<List<String>> getOneseoData(Screening screening) {
        List<Oneseo> oneseoList = oneseoRepository.findAllByScreeningDynamic(screening);
        return oneseoToExcelDataList(oneseoList);
    }

    private List<List<String>> getCombinedExtraScreeningData() {
        List<Oneseo> extraOneseoList = Stream.concat(
                oneseoRepository.findAllByScreeningDynamic(EXTRA_VETERANS).stream(),
                oneseoRepository.findAllByScreeningDynamic(Screening.EXTRA_ADMISSION).stream()
        ).collect(Collectors.toList());

        return oneseoToExcelDataList(extraOneseoList);
    }

    private List<List<String>> getFallenData() {
        List<Oneseo> fallenOneseo = entranceTestResultRepository
                .findAllByFirstTestPassYnOrSecondTestPassYn(NO, NO).stream()
                .map(EntranceTestResult::getOneseo)
                .collect(Collectors.toList());

        return oneseoToExcelDataList(fallenOneseo);
    }

    private List<List<String>> oneseoToExcelDataList(List<Oneseo> oneseoList) {
        List<List<String>> sheetData = new ArrayList<>();
        int index = 1;

        for (Oneseo oneseo : oneseoList) {
            EntranceTestResult entranceTestResult = entranceTestResultRepository.findByOneseo(oneseo);

            OneseoPrivacyDetail oneseoPrivacyDetail = oneseoPrivacyDetailRepository.findByOneseo(oneseo);

            BigDecimal finalScore = calculateFinalScore(entranceTestResult);

            String sex = null;
            String graduationType = null;

            switch (oneseo.getMember().getSex()) {
                case MALE -> sex = "남자";
                case FEMALE -> sex = "여자";
            }

            switch (oneseoPrivacyDetail.getGraduationType()) {
                case CANDIDATE -> graduationType = "졸업예정자";
                case GRADUATE -> graduationType = "졸업자";
                case GED -> graduationType = "검정고시";
            }

            String wantedScreening = convertScreening(oneseo.getWantedScreening());
            String appliedScreening = convertScreening(oneseo.getAppliedScreening());

            String submitCode = oneseo.getOneseoSubmitCode();

            List<String> rowData = List.of(
                    String.valueOf(index++),
                    formatSubmitCode(submitCode),
                    String.valueOf(oneseo.getMember().getName()),
                    String.valueOf(oneseo.getDesiredMajors().getFirstDesiredMajor()),
                    String.valueOf(oneseo.getDesiredMajors().getSecondDesiredMajor()),
                    String.valueOf(oneseo.getDesiredMajors().getThirdDesiredMajor()),
                    String.valueOf(oneseo.getMember().getBirth()),
                    String.valueOf(sex),
                    (oneseoPrivacyDetail.getAddress() + oneseoPrivacyDetail.getDetailAddress()),
                    String.valueOf(oneseoPrivacyDetail.getSchoolName()),
                    String.valueOf(graduationType),
                    String.valueOf(wantedScreening),
                    String.valueOf(appliedScreening),
                    String.valueOf(entranceTestResult.getEntranceTestFactorsDetail().getGeneralSubjectsScore()),
                    String.valueOf(entranceTestResult.getEntranceTestFactorsDetail().getArtsPhysicalSubjectsScore()),
                    String.valueOf(entranceTestResult.getEntranceTestFactorsDetail().getAttendanceScore()),
                    String.valueOf(entranceTestResult.getEntranceTestFactorsDetail().getVolunteerScore()),
                    String.valueOf(entranceTestResult.getDocumentEvaluationScore()),
                    String.valueOf(entranceTestResult.getCompetencyEvaluationScore()),
                    String.valueOf(entranceTestResult.getInterviewScore()),
                    String.valueOf(finalScore),
                    String.valueOf(oneseo.getDecidedMajor()),
                    String.valueOf(oneseo.getMember().getPhoneNumber()),
                    String.valueOf(oneseoPrivacyDetail.getGuardianPhoneNumber()),
                    String.valueOf(oneseoPrivacyDetail.getSchoolTeacherPhoneNumber()),
                    String.valueOf(convertTestPassYn(entranceTestResult.getFirstTestPassYn())),
                    String.valueOf(convertTestPassYn(entranceTestResult.getSecondTestPassYn()))
            );

            sheetData.add(rowData.stream().map(data -> data.equals("null") ? "" : data).collect(Collectors.toList()));
        }

        return sheetData;
    }

    private String formatSubmitCode(String submitCode) {
        // submitCode format: A-001
        String[] parts = submitCode.split("-");
        String prefix = parts[0];
        String number = String.format("%03d", Integer.parseInt(parts[1]));
        return prefix + "-" + number;
    }

    private String convertScreening(Screening screening) {
        if (screening == null) return null;

        return switch (screening) {
            case GENERAL -> "일반전형";
            case SPECIAL -> "특별전형";
            case EXTRA_VETERANS -> "국가보훈대상자";
            case EXTRA_ADMISSION -> "특례입학대상자";
        };
    }

    private String convertTestPassYn(YesNo yn) {
        if (yn == null) return null;

        return switch (yn) {
            case YES -> "합격";
            case NO -> "불합격";
        };
    }

    private BigDecimal calculateFinalScore(EntranceTestResult entranceTestResult) {

        BigDecimal competencyEvaluationScore = entranceTestResult.getCompetencyEvaluationScore();
        BigDecimal interviewScore = entranceTestResult.getInterviewScore();

        if (
                entranceTestResult.getSecondTestPassYn() == null
                        || competencyEvaluationScore == null
                        || interviewScore == null
        ) {
            return null;
        }

        BigDecimal documentEvaluationScore = entranceTestResult.getDocumentEvaluationScore()
                .divide(BigDecimal.valueOf(3), 3, RoundingMode.HALF_UP);

        return documentEvaluationScore.multiply(BigDecimal.valueOf(0.5))
                .add(competencyEvaluationScore.multiply(BigDecimal.valueOf(0.3)))
                .add(interviewScore.multiply(BigDecimal.valueOf(0.2)))
                .setScale(3, RoundingMode.HALF_UP);
    }
}
