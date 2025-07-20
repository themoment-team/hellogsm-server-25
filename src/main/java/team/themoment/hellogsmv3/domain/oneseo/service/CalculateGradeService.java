package team.themoment.hellogsmv3.domain.oneseo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import team.themoment.hellogsmv3.domain.oneseo.dto.internal.MiddleSchoolAchievementCalcDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.CalculatedScoreResDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.internal.GeneralSubjectsSemesterScoreCalcDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.GeneralSubjectsScoreDetailResDto;
import team.themoment.hellogsmv3.domain.oneseo.entity.*;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.GraduationType;
import team.themoment.hellogsmv3.domain.oneseo.repository.EntranceTestFactorsDetailRepository;
import team.themoment.hellogsmv3.domain.oneseo.repository.EntranceTestResultRepository;
import team.themoment.hellogsmv3.global.exception.error.ExpectedException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static team.themoment.hellogsmv3.domain.oneseo.entity.type.GraduationType.*;

@Service
@RequiredArgsConstructor
public class CalculateGradeService {

    private final EntranceTestResultRepository entranceTestResultRepository;
    private final EntranceTestFactorsDetailRepository entranceTestFactorsDetailRepository;

    public CalculatedScoreResDto execute(MiddleSchoolAchievementCalcDto dto, Oneseo oneseo, GraduationType graduationType) {
        validGraduationType(graduationType);
        validFreeSemester(dto.liberalSystem(), dto.freeSemester());

        GeneralSubjectsSemesterScoreCalcDto generalSubjectsSemesterScore = calcGeneralSubjectsSemesterScore(dto, graduationType);

        // 일반 교과 성적 환산값 (총점: 180점)
        BigDecimal generalSubjectsScore = calcGeneralSubjectsTotalScore(generalSubjectsSemesterScore);

        // 예체능 성적 환산값 (총점: 60점)
        BigDecimal artsPhysicalSubjectsScore = calcArtSportsScore(dto.artsPhysicalAchievement());

        // 교과 성적 환산값 (예체능 성적 + 일반 교과 성적, 총점: 240점)
        BigDecimal totalSubjectsScore = artsPhysicalSubjectsScore
                .add(generalSubjectsScore)
                .setScale(3, RoundingMode.HALF_UP);

        // 출결 성적 (총점: 30점)
        BigDecimal attendanceScore = calcAttendanceScore(
                dto.absentDays(), dto.attendanceDays()
        ).setScale(3, RoundingMode.HALF_UP);

        // 봉사 성적 (총점: 30점)
        BigDecimal volunteerScore = calcVolunteerScore(dto.volunteerTime())
                .setScale(3, RoundingMode.HALF_UP);

        // 비 교과 성적 환산값 (총점: 60점)
        BigDecimal totalNonSubjectsScore = attendanceScore.add(volunteerScore)
                .setScale(3, RoundingMode.HALF_UP);

        // 내신 성적 총 점수 (총점: 300점)
        BigDecimal totalScore = totalSubjectsScore.add(totalNonSubjectsScore)
                .setScale(3, RoundingMode.HALF_UP);

        if (oneseo != null) {
            EntranceTestResult findEntranceTestResult = entranceTestResultRepository.findByOneseo(oneseo);

            if (findEntranceTestResult == null) {
                EntranceTestFactorsDetail entranceTestFactorsDetail = EntranceTestFactorsDetail.builder()
                        .generalSubjectsScore(generalSubjectsScore)
                        .artsPhysicalSubjectsScore(artsPhysicalSubjectsScore)
                        .totalSubjectsScore(totalSubjectsScore)
                        .attendanceScore(attendanceScore)
                        .volunteerScore(volunteerScore)
                        .totalNonSubjectsScore(totalNonSubjectsScore)
                        .score1_2(generalSubjectsSemesterScore.score1_2())
                        .score2_1(generalSubjectsSemesterScore.score2_1())
                        .score2_2(generalSubjectsSemesterScore.score2_2())
                        .score3_1(generalSubjectsSemesterScore.score3_1())
                        .score3_2(generalSubjectsSemesterScore.score3_2())
                        .build();

                EntranceTestResult entranceTestResult = new EntranceTestResult(oneseo, entranceTestFactorsDetail, totalScore);

                entranceTestFactorsDetailRepository.save(entranceTestFactorsDetail);
                entranceTestResultRepository.save(entranceTestResult);
            } else {
                EntranceTestFactorsDetail findEntranceTestFactorsDetail = findEntranceTestResult.getEntranceTestFactorsDetail();

                findEntranceTestFactorsDetail.updateGradeEntranceTestFactorsDetail(
                        generalSubjectsScore, artsPhysicalSubjectsScore, totalSubjectsScore,
                        attendanceScore, volunteerScore, totalNonSubjectsScore,
                        generalSubjectsSemesterScore.score1_2(), generalSubjectsSemesterScore.score2_1(), generalSubjectsSemesterScore.score2_2(), generalSubjectsSemesterScore.score3_1(), generalSubjectsSemesterScore.score3_2()
                );

                findEntranceTestResult.modifyDocumentEvaluationScore(totalScore);

                oneseo.modifyEntranceTestResult(findEntranceTestResult);
                entranceTestFactorsDetailRepository.save(findEntranceTestFactorsDetail);
                entranceTestResultRepository.save(findEntranceTestResult);
            }

            GeneralSubjectsScoreDetailResDto generalSubjectsScoreDetailResDto = GeneralSubjectsScoreDetailResDto.builder()
                    .score1_2(generalSubjectsSemesterScore.score1_2())
                    .score2_1(generalSubjectsSemesterScore.score2_1())
                    .score2_2(generalSubjectsSemesterScore.score2_2())
                    .score3_1(generalSubjectsSemesterScore.score3_1())
                    .score3_2(generalSubjectsSemesterScore.score3_2())
                    .build();

            return CalculatedScoreResDto.builder()
                    .generalSubjectsScore(generalSubjectsScore)
                    .artsPhysicalSubjectsScore(artsPhysicalSubjectsScore)
                    .attendanceScore(attendanceScore)
                    .volunteerScore(volunteerScore)
                    .totalScore(totalScore)
                    .generalSubjectsScoreDetail(generalSubjectsScoreDetailResDto)
                    .build();
        }

        return CalculatedScoreResDto.builder()
                .generalSubjectsScore(generalSubjectsScore)
                .artsPhysicalSubjectsScore(artsPhysicalSubjectsScore)
                .attendanceScore(attendanceScore)
                .volunteerScore(volunteerScore)
                .totalScore(totalScore)
                .build();
    }

    private GeneralSubjectsSemesterScoreCalcDto calcGeneralSubjectsSemesterScore(MiddleSchoolAchievementCalcDto dto, GraduationType graduationType) {

        GeneralSubjectsSemesterScoreCalcDto.GeneralSubjectsSemesterScoreCalcDtoBuilder builder = GeneralSubjectsSemesterScoreCalcDto.builder();

        switch (graduationType) {
            case CANDIDATE -> builder
                    .score1_2(calcGeneralSubjectsScore(
                            dto.achievement1_2(),
                            BigDecimal.valueOf(18)
                    ))
                    .score2_1(calcGeneralSubjectsScore(
                            dto.achievement2_1(),
                            BigDecimal.valueOf(45)
                    ))
                    .score2_2(calcGeneralSubjectsScore(
                            dto.achievement2_2(),
                            BigDecimal.valueOf(45)
                    ))
                    .score3_1(calcGeneralSubjectsScore(
                            dto.achievement3_1(),
                            BigDecimal.valueOf(72)
                    ))
                    .score3_2(BigDecimal.ZERO);
            case GRADUATE -> builder
                    .score1_2(BigDecimal.ZERO)
                    .score2_1(calcGeneralSubjectsScore(
                            dto.achievement2_1(),
                            BigDecimal.valueOf(36)
                    ))
                    .score2_2(calcGeneralSubjectsScore(
                            dto.achievement2_2(),
                            BigDecimal.valueOf(36)
                    ))
                    .score3_1(calcGeneralSubjectsScore(
                            dto.achievement3_1(),
                            BigDecimal.valueOf(54)
                    ))
                    .score3_2(calcGeneralSubjectsScore(
                            dto.achievement3_2(),
                            BigDecimal.valueOf(54)
                    ));
        }

        return builder.build();
    }

    private BigDecimal calcGeneralSubjectsTotalScore(GeneralSubjectsSemesterScoreCalcDto generalSubjectsSemesterScore) {
        return Stream.of(
                        generalSubjectsSemesterScore.score1_2(),
                        generalSubjectsSemesterScore.score2_1(),
                        generalSubjectsSemesterScore.score2_2(),
                        generalSubjectsSemesterScore.score3_1(),
                        generalSubjectsSemesterScore.score3_2())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(3, RoundingMode.HALF_UP);
    }

    private BigDecimal calcGeneralSubjectsScore(List<Integer> achievements, BigDecimal maxPoint) {
        // 해당 학기의 등급별 점수 배열이 비어있거나 해당 학기의 배점이 없다면 0.000을 반환
        if (achievements == null || achievements.isEmpty() || maxPoint.equals(BigDecimal.ZERO)) return BigDecimal.valueOf(0).setScale(3, RoundingMode.HALF_UP);

        // Integer 리스트를 BigDecimal 리스트로 변경 & 등급 유효성 검사
        List<BigDecimal> convertedAchievements = new ArrayList<>();
        achievements.forEach(achievement -> {
            if (achievement > 5 || achievement < 0) throw new ExpectedException("올바르지 않은 일반교과 등급이 입력되었습니다.", HttpStatus.BAD_REQUEST);
            convertedAchievements.add(BigDecimal.valueOf(achievement));
        });

        // 해당 학기에 수강하지 않은 과목이 있다면 제거한 리스트를 반환 (점수가 0인 원소 제거)
        List<BigDecimal> noZeroAchievements = convertedAchievements.stream().filter(score -> score.compareTo(BigDecimal.ZERO) != 0).toList();
        // 위에서 구한 리스트가 비어있다면 0.000을 반환
        if (noZeroAchievements.isEmpty()) return BigDecimal.valueOf(0).setScale(3, RoundingMode.HALF_UP);

        // 1. 점수로 환산된 등급을 모두 더한다.
        BigDecimal reduceResult = convertedAchievements.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        // 2. 더한값 / (과목 수 * 5) (소수점 6째자리에서 반올림)
        BigDecimal divideResult = reduceResult.divide(BigDecimal.valueOf(noZeroAchievements.size() * 5L), 5, RoundingMode.HALF_UP);
        // 3. 각 학기당 배점 * 나눈값 (소수점 4째자리에서 반올림)
        return divideResult.multiply(maxPoint).setScale(3, RoundingMode.HALF_UP);
    }

    private BigDecimal calcArtSportsScore(List<Integer> achievements) {

        validateArtSportsScore(achievements);

        // 1. 각 등급별 갯수에 등급별 배점을 곱한 값을 더한다.
        int totalScores = achievements.stream().reduce(0, Integer::sum);
        // 2. 각 등급별 갯수를 모두 더해 성취 수를 구한다.
        int achievementCount = achievements.stream().filter(achievement -> achievement != 0).toList().size();
        // 3. 각 등급별 갯수를 더한 값(성취 수)에 5를 곰해 총점을 구한다.
        int maxScore = 5 * achievementCount;

        // 과목 수가 0일시 0점 반환
        if (achievementCount == 0) {
            return BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
        }

        BigDecimal averageOfAchievementScale = BigDecimal.valueOf(totalScores).divide(BigDecimal.valueOf(maxScore), 3, RoundingMode.HALF_UP);
        return BigDecimal.valueOf(60).multiply(averageOfAchievementScale).setScale(3, RoundingMode.HALF_UP);
    }

    private BigDecimal calcAttendanceScore(List<Integer> absentDays, List<Integer> attendanceDays) {
        // Integer 리스트를 BigDecimal 리스트로 변경
        List<BigDecimal> convertedAbsentDays = new ArrayList<>();
        List<BigDecimal> convertedAttendanceDays = new ArrayList<>();
        absentDays.forEach(absentDay -> {
            if (absentDay < 0)  throw new ExpectedException("결석일수는 음수가 될 수 없습니다.", HttpStatus.BAD_REQUEST);
            convertedAbsentDays.add(BigDecimal.valueOf(absentDay));
        });
        attendanceDays.forEach(attendanceDay -> {
            if (attendanceDay < 0)  throw new ExpectedException("출결일수는 음수가 될 수 없습니다.", HttpStatus.BAD_REQUEST);
            convertedAttendanceDays.add(BigDecimal.valueOf(attendanceDay));
        });

        validateAttendanceScore(convertedAbsentDays, convertedAttendanceDays);

        // 결석 횟수를 더함
        BigDecimal absentDay = convertedAbsentDays.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        // 결석 횟수가 10회 이상 0점을 반환
        if (absentDay.compareTo(BigDecimal.TEN) >= 0) return BigDecimal.valueOf(0);

        // 1. 모든 지각, 조퇴, 결과 횟수를 더함
        BigDecimal attendanceDay = convertedAttendanceDays.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        // 2. 지각, 조퇴, 결과 횟수는 3개당 결석 1회
        BigDecimal absentResult = attendanceDay.divide(BigDecimal.valueOf(3), 0, RoundingMode.DOWN);
        // 3. 총점(30점) - (3 * 총 결석 횟수)
        BigDecimal result = BigDecimal.valueOf(30).subtract(absentDay.add(absentResult).multiply(BigDecimal.valueOf(3)));

        // 총 점수가 0점 이하라면 0점을 반환
        if (result.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;

        return result;
    }

    private BigDecimal calcVolunteerScore(List<Integer> volunteerHours) {
        // Integer 리스트를 BigDecimal 리스트로 변경
        List<BigDecimal> convertedVolunteerHours = new ArrayList<>();
        volunteerHours.forEach(volunteerHour -> {
            if (volunteerHour < 0)  throw new ExpectedException("봉사일수는 음수가 될 수 없습니다.", HttpStatus.BAD_REQUEST);
            convertedVolunteerHours.add(BigDecimal.valueOf(volunteerHour));
        });

        validateVolunteerScore(convertedVolunteerHours);

        return convertedVolunteerHours.stream().reduce(BigDecimal.valueOf(0), (current, hour) -> {
            // 연간 7시간 이상
            if (hour.compareTo(BigDecimal.valueOf(7)) >= 0) {
                return current.add(BigDecimal.valueOf(10));
            }
            // 연간 6시간 이상
            else if (hour.compareTo(BigDecimal.valueOf(6)) >= 0) {
                return current.add(BigDecimal.valueOf(8));
            }
            // 연간 5시간 이상
            else if (hour.compareTo(BigDecimal.valueOf(5)) >= 0) {
                return current.add(BigDecimal.valueOf(6));
            }
            // 연간 4시간 이상
            else if (hour.compareTo(BigDecimal.valueOf(4)) >= 0) {
                return current.add(BigDecimal.valueOf(4));
            }
            // 연간 3시간 이하
            else {
                return current.add(BigDecimal.valueOf(2));
            }
        });
    }

    private void validGraduationType(GraduationType graduationType) {
        if (!graduationType.equals(CANDIDATE) && !graduationType.equals(GRADUATE))
            throw new IllegalArgumentException("올바르지 않은 graduationType입니다.");
    }

    private void validFreeSemester(String liberalSystem, String freeSemester) {
        List<String> validSemesterList = List.of("", "1-1", "1-2", "2-1", "2-2", "3-1", "3-2");
        /*학년제일경우 freeSemester는 null임
        * 학기제일 경우 freeSemester는 "1-1", "1-2", "2-1", "2-2", "3-1", "3-2" 중 하나임
        * 시행한 학기제가 만약 `입력 받는 성적` 외부에 있다면 공백임
        * ex) 졸업자는 2,3학년 성적만 받는데, 1-1 또는 1-2에 자유학기제를 시행한 경우
        *
        * + 2025년도 코드부터는 freeSemester를 더 이상 계산을 위해 사용하지 않으므로 BE에서 따로 사용/처리하지는 않음.
        */

        // liberalSystem가 null이거나 자유학기제 or 자유학년제가 아니라면 예외 발생
        if (liberalSystem == null || (!liberalSystem.equals("자유학기제") && !liberalSystem.equals("자유학년제")))
            throw new ExpectedException("올바른 liberalSystem을 입력해주세요.", HttpStatus.BAD_REQUEST);

        // 자유학기제 && 자유학기제가 적용된 학기를 입력하지 않았다면 예외 발생
        if (liberalSystem.equals("자유학기제") && freeSemester == null)
            throw new ExpectedException("자유학기가 적용된 학기를 입력해주세요.", HttpStatus.BAD_REQUEST);

        // 자유학기제 && 올바른 학기를 입력하지 않았다면 예외 발생
        if (liberalSystem.equals("자유학기제") && validSemesterList.stream().noneMatch(s -> s.equals(freeSemester)))
            throw new ExpectedException(String.format("%s(은)는 유효한 학기가 아닙니다.", freeSemester), HttpStatus.BAD_REQUEST);
    }

    private void validateArtSportsScore(List<Integer> achievements) {
        if (achievements == null || achievements.isEmpty()) {
            throw new ExpectedException("예체능 등급을 입력해주세요", HttpStatus.BAD_REQUEST);
        }

        achievements.forEach(achievement -> {
            if (achievement != 0 && (achievement > 5 || achievement < 3)) throw new ExpectedException("올바르지 않은 예체능 등급이 입력되었습니다.", HttpStatus.BAD_REQUEST);
        });
    }

    private void validateAttendanceScore(List<BigDecimal> convertedAbsentDays, List<BigDecimal> convertedAttendanceDays) {
        if (convertedAbsentDays.size() != 3)
            throw new ExpectedException("결석일수 개수가 유효하지 않습니다.", HttpStatus.BAD_REQUEST);

        if (convertedAttendanceDays.size() != 9)
            throw new ExpectedException("출결일수 개수가 유효하지 않습니다.", HttpStatus.BAD_REQUEST);
    }

    private void validateVolunteerScore(List<BigDecimal> convertedVolunteerHours) {
        if (convertedVolunteerHours.size() != 3)
            throw new ExpectedException("봉사일수 개수가 유효하지 않습니다.", HttpStatus.BAD_REQUEST);
    }
}
