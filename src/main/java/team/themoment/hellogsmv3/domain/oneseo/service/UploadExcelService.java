package team.themoment.hellogsmv3.domain.oneseo.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import team.themoment.hellogsmv3.domain.oneseo.repository.EntranceTestResultRepository;
import team.themoment.hellogsmv3.domain.oneseo.repository.OneseoRepository;
import team.themoment.hellogsmv3.global.exception.error.ExpectedException;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class UploadExcelService {

    private final EntranceTestResultRepository entranceTestResultRepository;
    private final OneseoRepository oneseoRepository;

    @Getter
    private enum CellIndex {
        EXAMINATION_NUMBER(0),
        COMPETENCY_EVALUATION_SCORE(1),
        INTERVIEW_SCORE(2);
        private final int index;
        CellIndex(int index) {
            this.index = index;
        }
    }

    public void execute(MultipartFile file) {
        Workbook workbook = createWorkbookFromFile(file);
        Sheet sheet = workbook.getSheetAt(0);
        // 0번쨰 행은 헤더
        for(int i = 1;i<=sheet.getLastRowNum();i++) {
            Row row = sheet.getRow(i);
            if(row == null) continue;
            String examinationNumber = readCell(row, CellIndex.EXAMINATION_NUMBER);
            BigDecimal competencyEvaluationScore = readScoreCell(row, CellIndex.COMPETENCY_EVALUATION_SCORE);
            BigDecimal interviewScore = readScoreCell(row, CellIndex.INTERVIEW_SCORE);
            saveSecondTestResult(examinationNumber, competencyEvaluationScore, interviewScore);
        }
    }

    private void saveSecondTestResult(String examinationNumber, BigDecimal competencyEvaluationScore, BigDecimal interviewScore) {
        oneseoRepository.findEntranceTestResultByExaminationNumber(examinationNumber)
                .ifPresentOrElse(
                        entranceTestResult -> {
                            entranceTestResult.modifyInterviewScore(interviewScore);
                            entranceTestResult.modifyCompetencyEvaluationScore(competencyEvaluationScore);
                            entranceTestResultRepository.save(entranceTestResult);
                        },
                        () -> {
                            throw new ExpectedException("해당 수험번호에 대한 응시결과(원서)가 존재하지 않습니다.", HttpStatus.NOT_FOUND);
                        }
                );
    }

    private Workbook createWorkbookFromFile(MultipartFile file){
        if (file.isEmpty()) {
            throw new ExpectedException("엑셀 파일이 비어있습니다.", HttpStatus.BAD_REQUEST);
        }
        try {
            return WorkbookFactory.create(file.getInputStream());
        } catch (IOException e) {
            throw new ExpectedException("엑셀 파일을 읽는 데 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    private BigDecimal readScoreCell(Row row, CellIndex cellIndex) {
        String cellValue = readCell(row, cellIndex);
        return (new BigDecimal(cellValue)).setScale(2, RoundingMode.HALF_UP);
    }
    private String readCell(Row row, CellIndex cellIndex){
        if (row.getCell(cellIndex.getIndex()) == null) {
            throw new ExpectedException("엑셀 파일에 필수 정보가 누락되었습니다.", HttpStatus.BAD_REQUEST);
        }
        return row.getCell(cellIndex.getIndex()).getStringCellValue();
    }
}
