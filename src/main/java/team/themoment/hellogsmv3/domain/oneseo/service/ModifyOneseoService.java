package team.themoment.hellogsmv3.domain.oneseo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CachePut;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.themoment.hellogsmv3.domain.member.entity.Member;
import team.themoment.hellogsmv3.domain.member.service.MemberService;
import team.themoment.hellogsmv3.domain.oneseo.dto.internal.MiddleSchoolAchievementCalcDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.request.MiddleSchoolAchievementReqDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.request.OneseoReqDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.*;
import team.themoment.hellogsmv3.domain.oneseo.entity.*;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.DesiredMajors;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.GraduationType;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.Screening;
import team.themoment.hellogsmv3.domain.oneseo.repository.MiddleSchoolAchievementRepository;
import team.themoment.hellogsmv3.domain.oneseo.repository.OneseoPrivacyDetailRepository;
import team.themoment.hellogsmv3.domain.oneseo.repository.OneseoRepository;
import team.themoment.hellogsmv3.domain.oneseo.repository.ScreeningChangeHistoryRepository;
import team.themoment.hellogsmv3.global.exception.error.ExpectedException;

import java.util.List;
import java.util.Optional;

import static team.themoment.hellogsmv3.domain.oneseo.service.OneseoService.isValidMiddleSchoolInfo;

@Service
@RequiredArgsConstructor
public class ModifyOneseoService {

    private final OneseoRepository oneseoRepository;
    private final OneseoPrivacyDetailRepository oneseoPrivacyDetailRepository;
    private final MiddleSchoolAchievementRepository middleSchoolAchievementRepository;
    private final ScreeningChangeHistoryRepository screeningChangeHistoryRepository;
    private final OneseoService oneseoService;
    private final MemberService memberService;
    private final CalculateGradeService calculateGradeService;
    private final CalculateGedService calculateGedService;

    @Transactional
    @CachePut(value = OneseoService.ONESEO_CACHE_VALUE, key = "#memberId")
    public FoundOneseoResDto execute(OneseoReqDto reqDto, Long memberId) {

        isValidMiddleSchoolInfo(reqDto);

        Member currentMember = memberService.findByIdOrThrow(memberId);
        Oneseo currentOneseo = oneseoService.findByMemberOrThrow(currentMember);

        EntranceTestResult entranceTestResult = currentOneseo.getEntranceTestResult();
        OneseoService.isBeforeFirstTest(entranceTestResult.getFirstTestPassYn());

        OneseoPrivacyDetail oneseoPrivacyDetail = oneseoPrivacyDetailRepository.findByOneseo(currentOneseo);
        MiddleSchoolAchievement middleSchoolAchievement = middleSchoolAchievementRepository.findByOneseo(currentOneseo);

        Oneseo modifiedOneseo = buildOneseo(reqDto, currentOneseo, currentMember);
        oneseoService.assignSubmitCode(modifiedOneseo, currentOneseo.getWantedScreening());

        saveOneseoPrivacyDetail(reqDto, oneseoPrivacyDetail, modifiedOneseo);
        saveMiddleSchoolAchievement(reqDto, middleSchoolAchievement, modifiedOneseo);
        saveHistoryIfWantedScreeningChange(reqDto.screening(), currentOneseo.getWantedScreening(), modifiedOneseo);

        oneseoRepository.save(modifiedOneseo);

        CalculatedScoreResDto calculatedScoreResDto = calculateMiddleSchoolAchievement(oneseoPrivacyDetail.getGraduationType(), middleSchoolAchievement, currentOneseo);
        OneseoPrivacyDetailResDto oneseoPrivacyDetailResDto = buildOneseoPrivacyDetailResDto(currentMember, oneseoPrivacyDetail);
        MiddleSchoolAchievementResDto middleSchoolAchievementResDto = buildMiddleSchoolAchievementResDto(middleSchoolAchievement);

        return buildOneseoResDto(
                modifiedOneseo,
                oneseoPrivacyDetailResDto,
                middleSchoolAchievementResDto,
                calculatedScoreResDto
        );
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
                .gedTotalScore(middleSchoolAchievement.getGedTotalScore())
                .build();
    }

    private FoundOneseoResDto buildOneseoResDto(
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

    private CalculatedScoreResDto calculateMiddleSchoolAchievement(GraduationType graduationType, MiddleSchoolAchievement middleSchoolAchievement, Oneseo oneseo) {
        MiddleSchoolAchievementCalcDto data = MiddleSchoolAchievementCalcDto.builder()
                .achievement1_2(middleSchoolAchievement.getAchievement1_2())
                .achievement2_1(middleSchoolAchievement.getAchievement2_1())
                .achievement2_2(middleSchoolAchievement.getAchievement2_2())
                .achievement3_1(middleSchoolAchievement.getAchievement3_1())
                .achievement3_2(middleSchoolAchievement.getAchievement3_2())
                .artsPhysicalAchievement(middleSchoolAchievement.getArtsPhysicalAchievement())
                .absentDays(middleSchoolAchievement.getAbsentDays())
                .attendanceDays(middleSchoolAchievement.getAttendanceDays())
                .volunteerTime(middleSchoolAchievement.getVolunteerTime())
                .liberalSystem(middleSchoolAchievement.getLiberalSystem())
                .freeSemester(middleSchoolAchievement.getFreeSemester())
                .gedTotalScore(middleSchoolAchievement.getGedTotalScore())
                .build();

        return switch (graduationType) {
            case CANDIDATE, GRADUATE -> calculateGradeService.execute(data, oneseo, graduationType);
            case GED -> calculateGedService.execute(data, oneseo, graduationType);
        };
    }

    private Oneseo buildOneseo(OneseoReqDto reqDto, Oneseo oneseo, Member currentMember) {
        return Oneseo.builder()
                .id(oneseo.getId())
                .member(currentMember)
                .desiredMajors(DesiredMajors.builder()
                        .firstDesiredMajor(reqDto.firstDesiredMajor())
                        .secondDesiredMajor(reqDto.secondDesiredMajor())
                        .thirdDesiredMajor(reqDto.thirdDesiredMajor())
                        .build())
                .middleSchoolAchievement(oneseo.getMiddleSchoolAchievement())
                .oneseoPrivacyDetail(oneseo.getOneseoPrivacyDetail())
                .entranceTestResult(oneseo.getEntranceTestResult())
                .wantedScreeningChangeHistory(oneseo.getWantedScreeningChangeHistory())
                .realOneseoArrivedYn(oneseo.getRealOneseoArrivedYn())
                .wantedScreening(reqDto.screening())
                .passYn(oneseo.getPassYn())
                .decidedMajor(oneseo.getDecidedMajor())
                .entranceIntentionYn(oneseo.getEntranceIntentionYn())
                .oneseoSubmitCode(oneseo.getOneseoSubmitCode())
                .build();
    }

    private void saveOneseoPrivacyDetail(OneseoReqDto reqDto, OneseoPrivacyDetail oneseoPrivacyDetail, Oneseo oneseo) {
        OneseoPrivacyDetail modifiedOneseoPrivacyDetail = OneseoPrivacyDetail.builder()
                .id(oneseoPrivacyDetail.getId())
                .oneseo(oneseo)
                .graduationType(reqDto.graduationType())
                .graduationDate(reqDto.graduationDate())
                .address(reqDto.address())
                .detailAddress(reqDto.detailAddress())
                .profileImg(reqDto.profileImg())
                .guardianName(reqDto.guardianName())
                .guardianPhoneNumber(reqDto.guardianPhoneNumber())
                .relationshipWithGuardian(reqDto.relationshipWithGuardian())
                .schoolAddress(reqDto.schoolAddress())
                .schoolName(reqDto.schoolName())
                .schoolTeacherName(reqDto.schoolTeacherName())
                .schoolTeacherPhoneNumber(reqDto.schoolTeacherPhoneNumber())
                .build();

        oneseo.modifyOneseoPrivacyDetail(modifiedOneseoPrivacyDetail);
        oneseoPrivacyDetailRepository.save(modifiedOneseoPrivacyDetail);
    }

    private void saveMiddleSchoolAchievement(OneseoReqDto reqDto, MiddleSchoolAchievement middleSchoolAchievement, Oneseo oneseo) {
        MiddleSchoolAchievementReqDto updatedMiddleSchoolAchievement = reqDto.middleSchoolAchievement();

        MiddleSchoolAchievement modifiedMiddleSchoolAchievement = MiddleSchoolAchievement.builder()
                .id(middleSchoolAchievement.getId())
                .oneseo(oneseo)
                .achievement1_2(validationGeneralAchievement(updatedMiddleSchoolAchievement.achievement1_2()))
                .achievement2_1(validationGeneralAchievement(updatedMiddleSchoolAchievement.achievement2_1()))
                .achievement2_2(validationGeneralAchievement(updatedMiddleSchoolAchievement.achievement2_2()))
                .achievement3_1(validationGeneralAchievement(updatedMiddleSchoolAchievement.achievement3_1()))
                .achievement3_2(validationGeneralAchievement(updatedMiddleSchoolAchievement.achievement3_2()))
                .generalSubjects(updatedMiddleSchoolAchievement.generalSubjects())
                .newSubjects(updatedMiddleSchoolAchievement.newSubjects())
                .artsPhysicalAchievement(validationArtsPhysicalAchievement(updatedMiddleSchoolAchievement.artsPhysicalAchievement()))
                .artsPhysicalSubjects(updatedMiddleSchoolAchievement.artsPhysicalSubjects())
                .absentDays(updatedMiddleSchoolAchievement.absentDays())
                .attendanceDays(updatedMiddleSchoolAchievement.attendanceDays())
                .volunteerTime(updatedMiddleSchoolAchievement.volunteerTime())
                .liberalSystem(updatedMiddleSchoolAchievement.liberalSystem())
                .freeSemester(updatedMiddleSchoolAchievement.freeSemester())
                .gedTotalScore(updatedMiddleSchoolAchievement.gedTotalScore())
                .build();

        oneseo.modifyMiddleSchoolAchievement(modifiedMiddleSchoolAchievement);
        middleSchoolAchievementRepository.save(modifiedMiddleSchoolAchievement);
    }

    private void saveHistoryIfWantedScreeningChange(Screening afterScreening, Screening beforeScreening, Oneseo oneseo) {
        if (beforeScreening != afterScreening) {
            WantedScreeningChangeHistory screeningChangeHistory = WantedScreeningChangeHistory.builder()
                    .beforeScreening(beforeScreening)
                    .afterScreening(afterScreening)
                    .oneseo(oneseo).build();

            oneseo.addWantedScreeningChangeHistory(screeningChangeHistory);
            screeningChangeHistoryRepository.save(screeningChangeHistory);
        }
    }

    private List<Integer> validationGeneralAchievement(List<Integer> achievements)  {
        if (achievements == null) return null;

        achievements.forEach(achievement -> {
            if (achievement > 5 || achievement < 0) throw new ExpectedException("올바르지 않은 일반교과 등급이 입력되었습니다.", HttpStatus.BAD_REQUEST);
        });

        return achievements;
    }

    private List<Integer> validationArtsPhysicalAchievement(List<Integer> achievements)  {
        if (achievements == null) return null;

        achievements.forEach(achievement -> {
            if (achievement != 0 && (achievement > 5 || achievement < 3)) throw new ExpectedException("올바르지 않은 예체능 등급이 입력되었습니다.", HttpStatus.BAD_REQUEST);
        });

        return achievements;
    }

}
