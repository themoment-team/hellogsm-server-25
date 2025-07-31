package team.themoment.hellogsmv3.domain.oneseo.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import team.themoment.hellogsmv3.domain.oneseo.dto.internal.SecondTestResultDto;
import team.themoment.hellogsmv3.domain.oneseo.entity.EntranceTestResult;
import team.themoment.hellogsmv3.domain.oneseo.repository.EntranceTestResultRepository;
import team.themoment.hellogsmv3.domain.oneseo.repository.OneseoRepository;
import team.themoment.hellogsmv3.global.exception.error.ExpectedException;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

    @Transactional
    public void execute(MultipartFile file) {
        Workbook workbook = createWorkbookFromFile(file);
        Sheet sheet = workbook.getSheetAt(0);
        Map<String,SecondTestResultDto> excelResults = getExcelTestResult(sheet);

        Set<String> examinationNumberToProcess = excelResults.keySet();
        Map<String, EntranceTestResult> queryResult = oneseoRepository.findEntranceTestResultByExaminationNumbersIn(examinationNumberToProcess.stream().toList());

        validateNotFoundExaminationNumbers(examinationNumberToProcess, queryResult.keySet());

        saveAllEntranceTestResults(queryResult, excelResults);
    }

    private Map<String,SecondTestResultDto> getExcelTestResult(Sheet sheet){
        Map<String,SecondTestResultDto> excelResults = new HashMap<>();
        Set<String> seenExaminationNumbers = new HashSet<>();
        Set<String> duplicateExaminationNumbers = new HashSet<>();
        // 0번쨰 행은 헤더
        for(int i = 1;i<=sheet.getLastRowNum();i++) {
            Row row = sheet.getRow(i);
            if(row == null) continue;

            String examinationNumber = readCell(row, CellIndex.EXAMINATION_NUMBER);
            BigDecimal competencyEvaluationScore = readScoreCell(row, CellIndex.COMPETENCY_EVALUATION_SCORE);
            BigDecimal interviewScore = readScoreCell(row, CellIndex.INTERVIEW_SCORE);

            if(!seenExaminationNumbers.add(examinationNumber)){
                duplicateExaminationNumbers.add(examinationNumber);
            }

            excelResults.put(examinationNumber, new SecondTestResultDto(competencyEvaluationScore, interviewScore));
        }
        if(!duplicateExaminationNumbers.isEmpty()){
            throw new ExpectedException("다음 수험번호가 중복되었습니다: " + String.join(", ", duplicateExaminationNumbers), HttpStatus.BAD_REQUEST);
        }
        return excelResults;
    }

    private void validateNotFoundExaminationNumbers(Set<String> expected,Set<String> found){
        Set<String> notFoundExaminationNumbers = expected.stream()
                .filter(examinationNumber -> !found.contains(examinationNumber))
                .collect(Collectors.toSet());
        if (!notFoundExaminationNumbers.isEmpty()) {
            throw new ExpectedException("다음 수험번호에 대한 응시결과(원서)가 존재하지 않습니다: " + String.join(", ", notFoundExaminationNumbers), HttpStatus.NOT_FOUND);
        }
    }

    private void saveAllEntranceTestResults(Map<String, EntranceTestResult> resultToModify,
                                            Map<String, SecondTestResultDto> excelResults) {
        resultToModify.forEach((examinationNumber, entranceTestResult) -> {
            entranceTestResult.modifyCompetencyEvaluationScore(excelResults.get(examinationNumber).competencyEvaluationScore());
            entranceTestResult.modifyInterviewScore(excelResults.get(examinationNumber).interviewScore());
        });
        entranceTestResultRepository.saveAll(resultToModify.values());
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
        BigDecimal score = new BigDecimal(cellValue).setScale(2, RoundingMode.HALF_UP);
        if(score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(new BigDecimal("100")) > 0) {
            throw new ExpectedException("점수는 0 이상 100 이하의 값이어야 합니다.", HttpStatus.BAD_REQUEST);
        }
        return score;
    }
    private String readCell(Row row, CellIndex cellIndex){
        if (row.getCell(cellIndex.getIndex()) == null) {
            throw new ExpectedException("엑셀 파일에 필수 정보가 누락되었습니다.", HttpStatus.BAD_REQUEST);
        }else if(row.getCell(cellIndex.getIndex()).getCellType() != CellType.STRING){
            throw new ExpectedException("엑셀 파일의 셀 타입이 잘못되었습니다. 문자열 타입이어야 합니다.", HttpStatus.BAD_REQUEST);
        }
        return row.getCell(cellIndex.getIndex()).getStringCellValue();
    }
}
