package team.themoment.hellogsmv3.domain.oneseo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import team.themoment.hellogsmv3.domain.member.entity.Member;
import team.themoment.hellogsmv3.domain.member.service.MemberService;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.*;
import team.themoment.hellogsmv3.domain.oneseo.entity.*;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.DesiredMajors;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.GraduationType;
import team.themoment.hellogsmv3.domain.oneseo.repository.MiddleSchoolAchievementRepository;
import team.themoment.hellogsmv3.domain.oneseo.repository.OneseoPrivacyDetailRepository;

import java.math.BigDecimal;
import java.util.List;

import static team.themoment.hellogsmv3.domain.oneseo.entity.type.GraduationType.*;
import static team.themoment.hellogsmv3.domain.oneseo.service.OneseoService.ONESEO_CACHE_VALUE;

@Service
@RequiredArgsConstructor
public class QueryOneseoByIdService {

    private final OneseoPrivacyDetailRepository oneseoPrivacyDetailRepository;
    private final MiddleSchoolAchievementRepository middleSchoolAchievementRepository;
    private final MemberService memberService;
    private final OneseoService oneseoService;

    @Cacheable(value = ONESEO_CACHE_VALUE, key = "#memberId")
    public FoundOneseoResDto execute(Long memberId) {
        Member member = memberService.findByIdOrThrow(memberId);
        Oneseo oneseo = oneseoService.findByMemberOrThrow(member);
        OneseoPrivacyDetail oneseoPrivacyDetail = oneseoPrivacyDetailRepository.findByOneseo(oneseo);
        MiddleSchoolAchievement middleSchoolAchievement = middleSchoolAchievementRepository.findByOneseo(oneseo);

        CalculatedScoreResDto calculatedScoreResDto = buildCalculatedScoreResDto(oneseo, oneseoPrivacyDetail.getGraduationType());

        OneseoPrivacyDetailResDto oneseoPrivacyDetailResDto = buildOneseoPrivacyDetailResDto(member, oneseoPrivacyDetail);
        MiddleSchoolAchievementResDto middleSchoolAchievementResDto = buildMiddleSchoolAchievementResDto(middleSchoolAchievement);

        return buildFoundOneseoResDto(
                oneseo,
                oneseoPrivacyDetailResDto,
                middleSchoolAchievementResDto,
                calculatedScoreResDto
        );
    }
    private CalculatedScoreResDto buildCalculatedScoreResDto(Oneseo oneseo, GraduationType graduationType) {
        EntranceTestResult entranceTestResult = oneseo.getEntranceTestResult();
        EntranceTestFactorsDetail entranceTestFactorsDetail = entranceTestResult.getEntranceTestFactorsDetail();

        GeneralSubjectsScoreDetailResDto generalSubjectsScoreDetailResDto = GeneralSubjectsScoreDetailResDto.builder()
                .score1_2(entranceTestFactorsDetail.getScore1_2())
                .score2_1(entranceTestFactorsDetail.getScore2_1())
                .score2_2(entranceTestFactorsDetail.getScore2_2())
                .score3_1(entranceTestFactorsDetail.getScore3_1())
                .score3_2(entranceTestFactorsDetail.getScore3_2())
                .build();

        return switch (graduationType) {
            case CANDIDATE, GRADUATE -> {
                MiddleSchoolAchievement middleSchoolAchievement = oneseo.getMiddleSchoolAchievement();
                BigDecimal score_1 = CalculateGradeService.calculateIndividualArtsPhysicalScore(middleSchoolAchievement.getArtsPhysicalAchievement(), 0, 3);
                BigDecimal score_2 = CalculateGradeService.calculateIndividualArtsPhysicalScore(middleSchoolAchievement.getArtsPhysicalAchievement(), 3, 6);
                BigDecimal score_3 = CalculateGradeService.calculateIndividualArtsPhysicalScore(middleSchoolAchievement.getArtsPhysicalAchievement(), 6, 9);
                BigDecimal score_4 = BigDecimal.ZERO;

                if (graduationType.equals(GRADUATE))
                    score_4 = CalculateGradeService.calculateIndividualArtsPhysicalScore(middleSchoolAchievement.getArtsPhysicalAchievement(), 9, 12);

                String freeSemesterKey = CalculateGradeService.getFreeSemesterKey(middleSchoolAchievement.getLiberalSystem(), middleSchoolAchievement.getFreeSemester());

                ArtsPhysicalSubjectsScoreDetailResDto artsPhysicalSubjectsScoreDetailResDto = ArtsPhysicalSubjectsScoreDetailResDto.builder()
                        .score1_2(CalculateGradeService.assignIndividualArtsPhysicalScore(freeSemesterKey, "1-2", graduationType, score_1, score_2, score_3, score_4))
                        .score2_1(CalculateGradeService.assignIndividualArtsPhysicalScore(freeSemesterKey, "2-1", graduationType, score_1, score_2, score_3, score_4))
                        .score2_2(CalculateGradeService.assignIndividualArtsPhysicalScore(freeSemesterKey, "2-2", graduationType, score_1, score_2, score_3, score_4))
                        .score3_1(CalculateGradeService.assignIndividualArtsPhysicalScore(freeSemesterKey, "3-1", graduationType, score_1, score_2, score_3, score_4))
                        .score3_2(CalculateGradeService.assignIndividualArtsPhysicalScore(freeSemesterKey, "3-2", graduationType, score_1, score_2, score_3, score_4))
                        .build();

                yield CalculatedScoreResDto.builder()
                        .generalSubjectsScore(entranceTestFactorsDetail.getGeneralSubjectsScore())
                        .artsPhysicalSubjectsScore(entranceTestFactorsDetail.getArtsPhysicalSubjectsScore())
                        .attendanceScore(entranceTestFactorsDetail.getAttendanceScore())
                        .volunteerScore(entranceTestFactorsDetail.getVolunteerScore())
                        .totalScore(entranceTestResult.getDocumentEvaluationScore())
                        .generalSubjectsScoreDetail(generalSubjectsScoreDetailResDto)
                        .artsPhysicalSubjectsScoreDetail(artsPhysicalSubjectsScoreDetailResDto)
                        .build();
            }
            case GED ->
                CalculatedScoreResDto.builder()
                        .totalSubjectsScore(entranceTestFactorsDetail.getTotalSubjectsScore())
                        .attendanceScore(entranceTestFactorsDetail.getAttendanceScore())
                        .volunteerScore(entranceTestFactorsDetail.getVolunteerScore())
                        .totalScore(entranceTestResult.getDocumentEvaluationScore())
                        .build();
        };
    }

    private OneseoPrivacyDetailResDto buildOneseoPrivacyDetailResDto(
            Member member,
            OneseoPrivacyDetail oneseoPrivacyDetail
    ) {
        return OneseoPrivacyDetailResDto.builder()
                .name(member.getName())
                .sex(member.getSex())
                .birth(member.getBirth())
                .phoneNumber(member.getPhoneNumber())
                .graduationType(oneseoPrivacyDetail.getGraduationType())
                .graduationDate(oneseoPrivacyDetail.getGraduationDate())
                .address(oneseoPrivacyDetail.getAddress())
                .detailAddress(oneseoPrivacyDetail.getDetailAddress())
                .guardianName(oneseoPrivacyDetail.getGuardianName())
                .guardianPhoneNumber(oneseoPrivacyDetail.getGuardianPhoneNumber())
                .relationshipWithGuardian(oneseoPrivacyDetail.getRelationshipWithGuardian())
                .schoolName(oneseoPrivacyDetail.getSchoolName())
                .schoolAddress(oneseoPrivacyDetail.getSchoolAddress())
                .schoolTeacherName(oneseoPrivacyDetail.getSchoolTeacherName())
                .schoolTeacherPhoneNumber(oneseoPrivacyDetail.getSchoolTeacherPhoneNumber())
                .profileImg(oneseoPrivacyDetail.getProfileImg())
                .build();
    }

    private MiddleSchoolAchievementResDto buildMiddleSchoolAchievementResDto(
            MiddleSchoolAchievement middleSchoolAchievement
    ) {

        List<Integer> absentDays = middleSchoolAchievement.getAbsentDays();
        List<Integer> attendanceDays = middleSchoolAchievement.getAttendanceDays();
        Integer absentDaysCount = OneseoService.calcAbsentDaysCount(absentDays, attendanceDays);

        return MiddleSchoolAchievementResDto.builder()
                .achievement1_2(middleSchoolAchievement.getAchievement1_2())
                .achievement2_1(middleSchoolAchievement.getAchievement2_1())
                .achievement2_2(middleSchoolAchievement.getAchievement2_2())
                .achievement3_1(middleSchoolAchievement.getAchievement3_1())
                .achievement3_2(middleSchoolAchievement.getAchievement3_2())
                .generalSubjects(middleSchoolAchievement.getGeneralSubjects())
                .newSubjects(middleSchoolAchievement.getNewSubjects())
                .artsPhysicalAchievement(middleSchoolAchievement.getArtsPhysicalAchievement())
                .artsPhysicalSubjects(middleSchoolAchievement.getArtsPhysicalSubjects())
                .absentDays(absentDays)
                .absentDaysCount(absentDaysCount)
                .attendanceDays(attendanceDays)
                .volunteerTime(middleSchoolAchievement.getVolunteerTime())
                .liberalSystem(middleSchoolAchievement.getLiberalSystem())
                .freeSemester(middleSchoolAchievement.getFreeSemester())
                .gedTotalScore(middleSchoolAchievement.getGedAvgScore())
                .build();
    }

    private FoundOneseoResDto buildFoundOneseoResDto(
            Oneseo oneseo,
            OneseoPrivacyDetailResDto oneseoPrivacyDetailResDto,
            MiddleSchoolAchievementResDto middleSchoolAchievementResDto,
            CalculatedScoreResDto calculatedScoreResDto
    ) {
        DesiredMajors desiredMajors = oneseo.getDesiredMajors();

        return FoundOneseoResDto.builder()
                .oneseoId(oneseo.getId())
                .submitCode(oneseo.getOneseoSubmitCode())
                .wantedScreening(oneseo.getWantedScreening())
                .desiredMajors(DesiredMajorsResDto.builder()
                        .firstDesiredMajor(desiredMajors.getFirstDesiredMajor())
                        .secondDesiredMajor(desiredMajors.getSecondDesiredMajor())
                        .thirdDesiredMajor(desiredMajors.getThirdDesiredMajor())
                        .build())
                .privacyDetail(oneseoPrivacyDetailResDto)
                .middleSchoolAchievement(middleSchoolAchievementResDto)
                .calculatedScore(calculatedScoreResDto)
                .build();
    }
}
