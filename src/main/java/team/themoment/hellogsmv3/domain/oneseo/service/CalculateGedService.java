package team.themoment.hellogsmv3.domain.oneseo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import team.themoment.hellogsmv3.domain.oneseo.dto.internal.MiddleSchoolAchievementCalcDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.CalculatedScoreResDto;
import team.themoment.hellogsmv3.domain.oneseo.entity.*;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.GraduationType;
import team.themoment.hellogsmv3.domain.oneseo.repository.EntranceTestFactorsDetailRepository;
import team.themoment.hellogsmv3.domain.oneseo.repository.EntranceTestResultRepository;
import team.themoment.hellogsmv3.global.exception.error.ExpectedException;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static team.themoment.hellogsmv3.domain.oneseo.entity.type.GraduationType.*;

@Service
@RequiredArgsConstructor
public class CalculateGedService {

    private final EntranceTestResultRepository entranceTestResultRepository;
    private final EntranceTestFactorsDetailRepository entranceTestFactorsDetailRepository;

    public CalculatedScoreResDto execute(MiddleSchoolAchievementCalcDto dto, Oneseo oneseo, GraduationType graduationType) {

        if (!graduationType.equals(GED))
            throw new IllegalArgumentException("올바르지 않은 graduationType입니다.");

        // 검정고시 평균 점수
        BigDecimal averageScore = dto.gedAvgScore();

        if (averageScore.compareTo(BigDecimal.valueOf(60)) < 0 || averageScore.compareTo(BigDecimal.valueOf(100)) > 0)
            throw new ExpectedException("검정고시 평균점은 60점 이상, 100점 이하여야 합니다.", HttpStatus.BAD_REQUEST);


        // 검정고시 교과 성적 환산 점수 (총점: 240점)
        BigDecimal gedTotalSubjectsScore = calcGedTotalSubjectsScore(averageScore);

        // 검정고시 봉사 성적 환산 점수 (총점: 30점)
        BigDecimal gedVolunteerScore = calcGedVolunteerScore(averageScore);

        // 검정고시 출결 성적 점수 (총점: 30점)
        BigDecimal gedAttendanceScore = BigDecimal.valueOf(30);

        // 검정고시 비 교과 성적 환산 점수 (총점: 60점)
        BigDecimal gedTotalNonSubjectsScore = gedVolunteerScore.add(gedAttendanceScore)
                .setScale(3, RoundingMode.HALF_UP);

        // 검정고시 총 점수 (교과 성적 + 비교과 성적) (총점: 300점)
        BigDecimal totalScore = gedTotalSubjectsScore.add(gedTotalNonSubjectsScore)
                .setScale(3, RoundingMode.HALF_UP);

        if (oneseo != null) {
            EntranceTestResult findEntranceTestResult = entranceTestResultRepository.findByOneseo(oneseo);

            if (findEntranceTestResult == null) {
                EntranceTestFactorsDetail entranceTestFactorsDetail = EntranceTestFactorsDetail.builder()
                        .attendanceScore(gedAttendanceScore)
                        .volunteerScore(gedVolunteerScore)
                        .totalSubjectsScore(gedTotalSubjectsScore)
                        .totalNonSubjectsScore(gedTotalNonSubjectsScore)
                        .build();

                EntranceTestResult entranceTestResult = new EntranceTestResult(oneseo, entranceTestFactorsDetail, totalScore);

                entranceTestFactorsDetailRepository.save(entranceTestFactorsDetail);
                entranceTestResultRepository.save(entranceTestResult);
            } else {
                EntranceTestFactorsDetail findEntranceTestFactorsDetail = findEntranceTestResult.getEntranceTestFactorsDetail();

                findEntranceTestFactorsDetail.updateGedEntranceTestFactorsDetail(
                        gedAttendanceScore, gedVolunteerScore,
                        gedTotalSubjectsScore, gedTotalNonSubjectsScore
                );

                findEntranceTestResult.modifyDocumentEvaluationScore(totalScore);

                entranceTestFactorsDetailRepository.save(findEntranceTestFactorsDetail);
                entranceTestResultRepository.save(findEntranceTestResult);
            }
        }

        return CalculatedScoreResDto.builder()
                .totalSubjectsScore(gedTotalSubjectsScore)
                .attendanceScore(gedAttendanceScore)
                .volunteerScore(gedVolunteerScore)
                .totalScore(totalScore)
                .build();
    }

    private BigDecimal calcGedTotalSubjectsScore(BigDecimal averageScore) {
        BigDecimal totalSubjectsScore = averageScore.subtract(BigDecimal.valueOf(50))
                .divide(BigDecimal.valueOf(50), 5, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(240))
                .setScale(3, RoundingMode.HALF_UP);

        return totalSubjectsScore.compareTo(BigDecimal.ZERO) < 0
                ? BigDecimal.ZERO
                : totalSubjectsScore;
    }

    private BigDecimal calcGedVolunteerScore(BigDecimal averageScore) {
        BigDecimal volunteerScore = averageScore.subtract(BigDecimal.valueOf(40))
                .divide(BigDecimal.valueOf(60), 5, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(30))
                .setScale(3, RoundingMode.HALF_UP);

        return volunteerScore.compareTo(BigDecimal.ZERO) < 0
                ? BigDecimal.ZERO
                : volunteerScore;
    }

}
