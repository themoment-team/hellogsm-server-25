package team.themoment.hellogsmv3.domain.oneseo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CachePut;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.themoment.hellogsmv3.domain.member.entity.Member;
import team.themoment.hellogsmv3.domain.member.service.MemberService;
import team.themoment.hellogsmv3.domain.oneseo.dto.request.MiddleSchoolAchievementReqDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.request.OneseoReqDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.*;
import team.themoment.hellogsmv3.domain.oneseo.entity.MiddleSchoolAchievement;
import team.themoment.hellogsmv3.domain.oneseo.entity.Oneseo;
import team.themoment.hellogsmv3.domain.oneseo.entity.OneseoPrivacyDetail;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.DesiredMajors;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.GraduationType;
import team.themoment.hellogsmv3.domain.oneseo.event.OneseoApplyEvent;
import team.themoment.hellogsmv3.domain.oneseo.repository.MiddleSchoolAchievementRepository;
import team.themoment.hellogsmv3.domain.oneseo.repository.OneseoPrivacyDetailRepository;
import team.themoment.hellogsmv3.domain.oneseo.repository.OneseoRepository;
import team.themoment.hellogsmv3.global.exception.error.ExpectedException;

import java.util.List;

import static team.themoment.hellogsmv3.domain.oneseo.entity.type.YesNo.*;
import static team.themoment.hellogsmv3.domain.oneseo.service.OneseoService.isValidMiddleSchoolInfo;

@Service
@RequiredArgsConstructor
public class CreateOneseoService {

    private final OneseoRepository oneseoRepository;
    private final OneseoPrivacyDetailRepository oneseoPrivacyDetailRepository;
    private final MiddleSchoolAchievementRepository middleSchoolAchievementRepository;
    private final MemberService memberService;
    private final CalculateGradeService calculateGradeService;
    private final CalculateGedService calculateGedService;
    private final OneseoService oneseoService;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    @CachePut(value = OneseoService.ONESEO_CACHE_VALUE, key = "#memberId")
    public FoundOneseoResDto execute(OneseoReqDto reqDto, Long memberId) {

        isValidMiddleSchoolInfo(reqDto);

        Member currentMember = memberService.findByIdForUpdateOrThrow(memberId);

        isExistOneseo(currentMember);

        Oneseo oneseo = buildOneseo(reqDto, currentMember);
        OneseoPrivacyDetail oneseoPrivacyDetail = buildOneseoPrivacyDetail(reqDto, oneseo);
        MiddleSchoolAchievement middleSchoolAchievement = buildMiddleSchoolAchievement(reqDto, oneseo);

        oneseoService.assignSubmitCode(oneseo, null);

        saveEntities(oneseo, oneseoPrivacyDetail, middleSchoolAchievement);

        CalculatedScoreResDto calculatedScoreResDto = calculateMiddleSchoolAchievement(oneseoPrivacyDetail.getGraduationType(), middleSchoolAchievement, oneseo);

        OneseoPrivacyDetailResDto oneseoPrivacyDetailResDto = buildOneseoPrivacyDetailResDto(currentMember, oneseoPrivacyDetail);
        MiddleSchoolAchievementResDto middleSchoolAchievementResDto = buildMiddleSchoolAchievementResDto(middleSchoolAchievement);

        sendOneseoApplyEvent(currentMember, oneseo, oneseoPrivacyDetail);

        return buildOneseoResDto(
                oneseo,
                oneseoPrivacyDetailResDto,
                middleSchoolAchievementResDto,
                calculatedScoreResDto
        );
    }

    private void sendOneseoApplyEvent(Member currentMember, Oneseo oneseo, OneseoPrivacyDetail oneseoPrivacyDetail) {
        OneseoApplyEvent oneseoApplyEvent = OneseoApplyEvent.builder()
                .name(currentMember.getName())
                .summitCode(oneseo.getOneseoSubmitCode())
                .graduationType(oneseoPrivacyDetail.getGraduationType())
                .screening(oneseo.getWantedScreening())
                .build();

        applicationEventPublisher.publishEvent(oneseoApplyEvent);
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
        MiddleSchoolAchievementReqDto data = MiddleSchoolAchievementReqDto.builder()
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

    private void saveEntities(Oneseo oneseo, OneseoPrivacyDetail oneseoPrivacyDetail, MiddleSchoolAchievement middleSchoolAchievement) {
        oneseoRepository.save(oneseo);
        oneseoPrivacyDetailRepository.save(oneseoPrivacyDetail);
        middleSchoolAchievementRepository.save(middleSchoolAchievement);
    }

    private Oneseo buildOneseo(OneseoReqDto reqDto, Member currentMember) {
        return Oneseo.builder()
                .member(currentMember)
                .desiredMajors(DesiredMajors.builder()
                        .firstDesiredMajor(reqDto.firstDesiredMajor())
                        .secondDesiredMajor(reqDto.secondDesiredMajor())
                        .thirdDesiredMajor(reqDto.thirdDesiredMajor())
                        .build())
                .realOneseoArrivedYn(NO)
                .wantedScreening(reqDto.screening())
                .build();
    }

    private OneseoPrivacyDetail buildOneseoPrivacyDetail(OneseoReqDto reqDto, Oneseo oneseo) {
        return OneseoPrivacyDetail.builder()
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
    }

    private MiddleSchoolAchievement buildMiddleSchoolAchievement(OneseoReqDto reqDto, Oneseo oneseo) {
        MiddleSchoolAchievementReqDto middleSchoolAchievement = reqDto.middleSchoolAchievement();

        return MiddleSchoolAchievement.builder()
                .oneseo(oneseo)
                .achievement1_2(validationGeneralAchievement(middleSchoolAchievement.achievement1_2()))
                .achievement2_1(validationGeneralAchievement(middleSchoolAchievement.achievement2_1()))
                .achievement2_2(validationGeneralAchievement(middleSchoolAchievement.achievement2_2()))
                .achievement3_1(validationGeneralAchievement(middleSchoolAchievement.achievement3_1()))
                .achievement3_2(validationGeneralAchievement(middleSchoolAchievement.achievement3_2()))
                .generalSubjects(middleSchoolAchievement.generalSubjects())
                .newSubjects(middleSchoolAchievement.newSubjects())
                .artsPhysicalAchievement(validationArtsPhysicalAchievement(middleSchoolAchievement.artsPhysicalAchievement()))
                .artsPhysicalSubjects(middleSchoolAchievement.artsPhysicalSubjects())
                .absentDays(middleSchoolAchievement.absentDays())
                .attendanceDays(middleSchoolAchievement.attendanceDays())
                .volunteerTime(middleSchoolAchievement.volunteerTime())
                .liberalSystem(middleSchoolAchievement.liberalSystem())
                .freeSemester(middleSchoolAchievement.freeSemester())
                .gedTotalScore(middleSchoolAchievement.gedTotalScore())
                .build();
    }

    private void isExistOneseo(Member currentMember) {
        if (oneseoRepository.existsByMember(currentMember)) {
            throw new ExpectedException("이미 원서가 존재합니다.", HttpStatus.BAD_REQUEST);
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
