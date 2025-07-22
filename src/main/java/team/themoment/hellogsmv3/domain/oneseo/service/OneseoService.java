package team.themoment.hellogsmv3.domain.oneseo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import team.themoment.hellogsmv3.domain.common.operation.entity.OperationTestResult;
import team.themoment.hellogsmv3.domain.common.operation.repo.OperationTestResultRepository;
import team.themoment.hellogsmv3.domain.member.entity.Member;
import team.themoment.hellogsmv3.domain.oneseo.dto.internal.MiddleSchoolAchievementCalcDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.request.MiddleSchoolAchievementReqDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.request.OneseoReqDto;
import team.themoment.hellogsmv3.domain.oneseo.entity.Oneseo;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.GraduationType;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.Screening;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.ScreeningCategory;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.YesNo;
import team.themoment.hellogsmv3.domain.oneseo.repository.EntranceTestResultRepository;
import team.themoment.hellogsmv3.domain.oneseo.repository.OneseoRepository;
import team.themoment.hellogsmv3.global.exception.error.ExpectedException;
import team.themoment.hellogsmv3.global.security.data.ScheduleEnvironment;

import java.time.LocalDateTime;
import java.util.List;

import static team.themoment.hellogsmv3.domain.oneseo.entity.type.GraduationType.CANDIDATE;
import static team.themoment.hellogsmv3.domain.oneseo.entity.type.YesNo.NO;

@Service
@RequiredArgsConstructor
public class OneseoService {

    public static final String ONESEO_CACHE_VALUE = "oneseo";
    private final ScheduleEnvironment scheduleEnv;
    private final OneseoRepository oneseoRepository;
    private final OperationTestResultRepository operationTestResultRepository;
    private final EntranceTestResultRepository entranceTestResultRepository;

    public void assignSubmitCode(Oneseo oneseo, Screening originalScreening) {
        if (oneseo.getWantedScreening() != originalScreening) {
            Integer maxSubmitCodeNumber = oneseoRepository.findMaxSubmitCodeByScreening(oneseo.getWantedScreening());
            int newSubmitCodeNumber = (maxSubmitCodeNumber != null ? maxSubmitCodeNumber : 0) + 1;

            String submitCode;
            ScreeningCategory screeningCategory = oneseo.getWantedScreening().getScreeningCategory();
            switch (screeningCategory) {
                case GENERAL -> submitCode = "A-" + newSubmitCodeNumber;
                case SPECIAL -> submitCode = "B-" + newSubmitCodeNumber;
                case EXTRA -> submitCode = "C-" + newSubmitCodeNumber;
                default -> throw new IllegalArgumentException("Unexpected value: " + screeningCategory);
            }

            oneseo.setOneseoSubmitCode(submitCode);
        }
    }

    public static MiddleSchoolAchievementCalcDto buildCalcDtoWithFillEmpty(MiddleSchoolAchievementReqDto dto,GraduationType graduationType){
        MiddleSchoolAchievementCalcDto.MiddleSchoolAchievementCalcDtoBuilder builder = MiddleSchoolAchievementCalcDto.builder();
        List<Integer> tmpAchievement1_2 = dto.achievement1_2();
        List<Integer> tmpAchievement2_1 = dto.achievement2_1();
        if(graduationType == GraduationType.GED) {
            builder
                    .gedTotalScore(dto.gedTotalScore());
            return builder.build();
        }
        // 졸업예정자 & 졸업자는 없는 성적을 복사하여 사용
        // 자유학기제는 1-1,1-2,2-1 에서만 시행 가능하므로 나머지는 성적이 있는것으로 간주
        if(graduationType == GraduationType.CANDIDATE && dto.achievement1_2() == null){
            if(dto.achievement1_1() == null){
                tmpAchievement1_2 = dto.achievement2_2();
            } else {
                tmpAchievement1_2 = dto.achievement1_1();
            }
        }
        if(tmpAchievement2_1 == null){
            tmpAchievement2_1 = dto.achievement2_2();
        }
        builder
                .achievement1_2(validationGeneralAchievement(tmpAchievement1_2))
                .achievement2_1(validationGeneralAchievement(tmpAchievement2_1))
                .achievement2_2(validationGeneralAchievement(dto.achievement2_2()))
                .achievement3_1(validationGeneralAchievement(dto.achievement3_1()))
                .achievement3_2(validationGeneralAchievement(dto.achievement3_2()))
                .artsPhysicalAchievement(validationArtsPhysicalAchievement(dto.artsPhysicalAchievement()))
                .absentDays(dto.absentDays())
                .attendanceDays(dto.attendanceDays())
                .volunteerTime(dto.volunteerTime())
                .liberalSystem(dto.liberalSystem())
                .freeSemester(dto.freeSemester())
                .gedTotalScore(dto.gedTotalScore());
        return builder.build();
    }
    private static List<Integer> validationArtsPhysicalAchievement(List<Integer> achievements)  {
        if (achievements == null) return null;

        achievements.forEach(achievement -> {
            if (achievement != 0 && (achievement > 5 || achievement < 3)) throw new ExpectedException("올바르지 않은 예체능 등급이 입력되었습니다.", HttpStatus.BAD_REQUEST);
        });

        return achievements;
    }
    private static List<Integer> validationGeneralAchievement(List<Integer> achievements)  {
        if (achievements == null) return null;

        achievements.forEach(achievement -> {
            if (achievement > 5 || achievement < 0) throw new ExpectedException("올바르지 않은 일반교과 등급이 입력되었습니다.", HttpStatus.BAD_REQUEST);
        });

        return achievements;
    }

    public static Integer calcAbsentDaysCount(List<Integer> absentDays, List<Integer> attendanceDays) {
        if (absentDays == null || attendanceDays == null) {
            return null;
        }

        int totalAbsentDays = absentDays.stream().mapToInt(Integer::intValue).sum();
        int totalAttendanceDays = attendanceDays.stream().mapToInt(Integer::intValue).sum();

        return totalAbsentDays + (totalAttendanceDays / 3);
    }

    public static void isBeforeFirstTest(YesNo yn) {
        if (yn != null) {
            throw new ExpectedException("1차 전형 결과 산출 이후에는 작업을 진행할 수 없습니다.", HttpStatus.FORBIDDEN);
        }
    }

    public static void isBeforeSecondTest(YesNo yn) {
        if (yn != null) {
            throw new ExpectedException("2차 전형 결과 산출 이후에는 작업을 진행할 수 없습니다.", HttpStatus.FORBIDDEN);
        }
    }

    public boolean validateFirstTestResultAnnouncement() {
        OperationTestResult testResult = operationTestResultRepository.findTestResult();

        return testResult.getFirstTestResultAnnouncementYn().equals(NO) ||
                LocalDateTime.now().isBefore(scheduleEnv.firstResultsAnnouncement()) ||
                entranceTestResultRepository.existsByFirstTestPassYnIsNull();
    }

    public boolean validateSecondTestResultAnnouncement() {
        OperationTestResult testResult = operationTestResultRepository.findTestResult();

        return testResult.getSecondTestResultAnnouncementYn().equals(NO) ||
                LocalDateTime.now().isBefore(scheduleEnv.firstResultsAnnouncement()) ||
                entranceTestResultRepository.existsByFirstTestPassYnIsNull();
    }

    public static void isValidMiddleSchoolInfo(OneseoReqDto reqDto) {
        if (
                reqDto.graduationType().equals(CANDIDATE) && (
                        isBlankString(reqDto.schoolTeacherName()) ||
                        isBlankString(reqDto.schoolTeacherPhoneNumber()) ||
                        isBlankString(reqDto.schoolName()) ||
                        isBlankString(reqDto.schoolAddress())
                )
        ) {
            throw new ExpectedException("중학교 졸업예정인 지원자는 현재 재학 중인 중학교 정보를 필수로 입력해야 합니다.", HttpStatus.BAD_REQUEST);
        }
    }

    private static boolean isBlankString(String target) {
        return target == null || target.isBlank();
    }

    public Oneseo findByMemberOrThrow(Member member) {
        return oneseoRepository.findByMember(member)
                .orElseThrow(() -> new ExpectedException("해당 지원자의 원서를 찾을 수 없습니다. member ID: " + member.getId(), HttpStatus.NOT_FOUND));
    }
}
