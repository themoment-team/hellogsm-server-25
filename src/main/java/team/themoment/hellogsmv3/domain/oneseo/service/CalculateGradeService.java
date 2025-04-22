package team.themoment.hellogsmv3.domain.oneseo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import team.themoment.hellogsmv3.domain.oneseo.dto.request.MiddleSchoolAchievementReqDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.ArtsPhysicalSubjectsScoreDetailResDto;
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

    public CalculatedScoreResDto execute(MiddleSchoolAchievementReqDto dto, Oneseo oneseo, GraduationType graduationType) {

        validGraduationType(graduationType);
        validFreeSemester(dto.liberalSystem(), dto.freeSemester());

        String liberalSystem = dto.liberalSystem();
        // freeSemester가 NULL이라면 "" 공백으로 변경, "" 공백이라면 "1-1"로 변경
        String freeSemester = dto.freeSemester() == null
                ? ""
                : dto.freeSemester().equals("")
                ? "1-1"
                : dto.freeSemester();

        GeneralSubjectsSemesterScoreCalcDto generalSubjectsSemesterScore = calcGeneralSubjectsSemesterScore(dto, graduationType, liberalSystem, freeSemester);

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

            validateArtPhysicalAchievement(graduationType, dto.artsPhysicalAchievement());

            BigDecimal score_1 = calculateIndividualArtsPhysicalScore(dto.artsPhysicalAchievement(), 0, 3);
            BigDecimal score_2 = calculateIndividualArtsPhysicalScore(dto.artsPhysicalAchievement(), 3, 6);
            BigDecimal score_3 = calculateIndividualArtsPhysicalScore(dto.artsPhysicalAchievement(), 6, 9);
            BigDecimal score_4 = BigDecimal.ZERO;

            if (graduationType.equals(GRADUATE))
                score_4 = calculateIndividualArtsPhysicalScore(dto.artsPhysicalAchievement(), 9, 12);

            String freeSemesterKey = getFreeSemesterKey(liberalSystem, freeSemester);

            ArtsPhysicalSubjectsScoreDetailResDto artsPhysicalSubjectsScoreDetailResDto = ArtsPhysicalSubjectsScoreDetailResDto.builder()
                    .score1_2(assignIndividualArtsPhysicalScore(freeSemesterKey, "1-2", graduationType, score_1, score_2, score_3, score_4))
                    .score2_1(assignIndividualArtsPhysicalScore(freeSemesterKey, "2-1", graduationType, score_1, score_2, score_3, score_4))
                    .score2_2(assignIndividualArtsPhysicalScore(freeSemesterKey, "2-2", graduationType, score_1, score_2, score_3, score_4))
                    .score3_1(assignIndividualArtsPhysicalScore(freeSemesterKey, "3-1", graduationType, score_1, score_2, score_3, score_4))
                    .score3_2(assignIndividualArtsPhysicalScore(freeSemesterKey, "3-2", graduationType, score_1, score_2, score_3, score_4))
                    .build();

            return CalculatedScoreResDto.builder()
                    .generalSubjectsScore(generalSubjectsScore)
                    .artsPhysicalSubjectsScore(artsPhysicalSubjectsScore)
                    .attendanceScore(attendanceScore)
                    .volunteerScore(volunteerScore)
                    .totalScore(totalScore)
                    .generalSubjectsScoreDetail(generalSubjectsScoreDetailResDto)
                    .artsPhysicalSubjectsScoreDetail(artsPhysicalSubjectsScoreDetailResDto)
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

    private static void validateArtPhysicalAchievement(GraduationType graduationType, List<Integer> achievementList) {
        // 졸업예정자는 예체능 점수를 9개, 졸업자는 예체능 점수를 12개를 보내야 함
        int size = graduationType.equals(CANDIDATE) ? 9 : 12;
        if (achievementList.size() != size) {
            throw new ExpectedException("예체능 성취도 개수가 유효하지 않습니다. " + achievementList.size(), HttpStatus.BAD_REQUEST);
        }
    }

    public static String getFreeSemesterKey(String liberalSystem, String freeSemester) {
        if (liberalSystem.equals("자유학년제") || freeSemester.equals("1-1") || freeSemester.equals("1-2")) {
            return "free";
        }
        return freeSemester;
    }

    public static BigDecimal assignIndividualArtsPhysicalScore(
            String freeSemesterKey, String currentSemester, GraduationType graduationType,
            BigDecimal score_1, BigDecimal score_2, BigDecimal score_3, BigDecimal score_4
    ) {
        switch (freeSemesterKey) {
            case "free", "1-1", "1-2" -> {
                if (currentSemester.equals("2-1")) return score_1;
                if (currentSemester.equals("2-2")) return score_2;
                if (currentSemester.equals("3-1")) return score_3;
                if (currentSemester.equals("3-2") && graduationType.equals(GRADUATE)) return score_4;
            }
            case "2-1" -> {
                if (currentSemester.equals("1-2")) return score_1;
                if (currentSemester.equals("2-2")) return score_2;
                if (currentSemester.equals("3-1")) return score_3;
                if (currentSemester.equals("3-2") && graduationType.equals(GRADUATE)) return score_4;
            }
            case "2-2" -> {
                if (currentSemester.equals("1-2")) return score_1;
                if (currentSemester.equals("2-1")) return score_2;
                if (currentSemester.equals("3-1")) return score_3;
                if (currentSemester.equals("3-2") && graduationType.equals(GRADUATE)) return score_4;
            }
            case "3-1" -> {
                if (currentSemester.equals("1-2")) return score_1;
                if (currentSemester.equals("2-1")) return score_2;
                if (currentSemester.equals("2-2")) return score_3;
                if (currentSemester.equals("3-2") && graduationType.equals(GRADUATE)) return score_4;
            }
            case "3-2" -> {
                if (currentSemester.equals("1-2")) return score_1;
                if (currentSemester.equals("2-1")) return score_2;
                if (currentSemester.equals("2-2")) return score_3;
                if (currentSemester.equals("3-1") && graduationType.equals(GRADUATE)) return score_4;
            }
        }
        return null;
    }

    public static BigDecimal calculateIndividualArtsPhysicalScore(List<Integer> achievementList, int start, int end) {
        List<Integer> subList = achievementList.subList(start, end);

        int sum = subList.stream()
                .reduce(0, Integer::sum);

        int achievementCount = (int) subList.stream()
                .filter(achievement -> achievement != 0)
                .count();

        int allAchievementCount = (int) achievementList.stream()
                .filter(achievement -> achievement != 0)
                .count();

        if (achievementCount == 0) {
            return BigDecimal.ZERO;
        }

        // 개별 학기별 예체능 점수 배점은 60 * (해당 학기의 성적이 있는 예체능 교과 수 / 성적이 있는 총 예체능 교과 수)으로 계산
        BigDecimal allocation = BigDecimal.valueOf(60)
                .multiply(
                        BigDecimal.valueOf(achievementCount).divide(BigDecimal.valueOf(allAchievementCount), 10, RoundingMode.HALF_UP)
                );

        return BigDecimal.valueOf(sum)
                .divide(BigDecimal.valueOf(achievementCount).multiply(BigDecimal.valueOf(5)), 10, RoundingMode.HALF_UP)
                .multiply(allocation)
                .setScale(3, RoundingMode.HALF_UP);
    }

    private GeneralSubjectsSemesterScoreCalcDto calcGeneralSubjectsSemesterScore(MiddleSchoolAchievementReqDto dto, GraduationType graduationType, String liberalSystem, String freeSemester) {

        GeneralSubjectsSemesterScoreCalcDto.GeneralSubjectsSemesterScoreCalcDtoBuilder builder = GeneralSubjectsSemesterScoreCalcDto.builder();

        switch (graduationType) {
            case CANDIDATE -> builder
                        .score1_2(calcGeneralSubjectsScore(
                                dto.achievement1_2(),
                                BigDecimal.valueOf((liberalSystem.equals("자유학년제") || freeSemester.equals("1-2") || freeSemester.equals("1-1")) ? 0 : 54)
                        ))
                        .score2_1(calcGeneralSubjectsScore(
                                dto.achievement2_1(),
                                BigDecimal.valueOf(freeSemester.equals("2-1") ? 0 : 54)
                        ))
                        .score2_2(calcGeneralSubjectsScore(
                                dto.achievement2_2(),
                                BigDecimal.valueOf(freeSemester.equals("2-2") ? 0 : (freeSemester.equals("3-1") ? 72 : 54))
                        ))
                        .score3_1(calcGeneralSubjectsScore(
                                dto.achievement3_1(),
                                BigDecimal.valueOf(freeSemester.equals("3-1") ? 0 : 72)
                        ))
                        .score3_2(BigDecimal.ZERO);
            case GRADUATE -> builder
                        .score1_2(calcGeneralSubjectsScore(
                                dto.achievement1_2(),
                                BigDecimal.valueOf((liberalSystem.equals("자유학년제") || freeSemester.equals("1-2")) ? 0 : 36)
                        ))
                        .score2_1(calcGeneralSubjectsScore(
                                dto.achievement2_1(),
                                BigDecimal.valueOf(freeSemester.equals("2-1") ? 0 : 36)
                        ))
                        .score2_2(calcGeneralSubjectsScore(
                                dto.achievement2_2(),
                                BigDecimal.valueOf(freeSemester.equals("2-2") || freeSemester.equals("3-1") || freeSemester.equals("3-2") || freeSemester.equals("1-1") ? 0 : 36)
                        ))
                        .score3_1(calcGeneralSubjectsScore(
                                dto.achievement3_1(),
                                BigDecimal.valueOf(freeSemester.equals("3-1") ? 0 : 54)
                        ))
                        .score3_2(calcGeneralSubjectsScore(
                                dto.achievement3_2(),
                                BigDecimal.valueOf(freeSemester.equals("3-2") || freeSemester.equals("1-1") ? 0 : 54)
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
