package team.themoment.hellogsmv3.domain.common.testResult.service;

import static team.themoment.hellogsmv3.domain.common.testResult.type.TestType.*;
import static team.themoment.hellogsmv3.domain.member.entity.type.AuthCodeType.TEST_RESULT;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import team.themoment.hellogsmv3.domain.common.testResult.dto.response.FoundTestResultResDto;
import team.themoment.hellogsmv3.domain.common.testResult.type.TestType;
import team.themoment.hellogsmv3.domain.member.service.CommonCodeService;
import team.themoment.hellogsmv3.domain.oneseo.entity.Oneseo;
import team.themoment.hellogsmv3.domain.oneseo.repository.OneseoRepository;
import team.themoment.hellogsmv3.domain.oneseo.service.OneseoService;
import team.themoment.hellogsmv3.global.exception.error.ExpectedException;

@Service
@RequiredArgsConstructor
public class QueryTestResultService {

    private final OneseoService oneseoService;
    private final OneseoRepository oneseoRepository;
    private final CommonCodeService commonCodeService;

    public FoundTestResultResDto execute(Long memberId, String code, String phoneNumber, String oneseoCode,
            TestType testType) {
        validateTesResultAnnouncement(testType);

        Oneseo findOneseo = findOneseo(phoneNumber, oneseoCode, testType);
        commonCodeService.validateAndDelete(memberId, code, phoneNumber, TEST_RESULT);

        return getResDto(findOneseo, testType);
    }

    private void validateTesResultAnnouncement(TestType testType) {
        if (testType.equals(FIRST)) {
            if (oneseoService.validateFirstTestResultAnnouncement()) {
                throw new ExpectedException("1차 전형 결과 발표 전 입니다.", HttpStatus.BAD_REQUEST);
            }
        } else {
            if (oneseoService.validateSecondTestResultAnnouncement()) {
                throw new ExpectedException("2차 전형 결과 발표 전 입니다.", HttpStatus.BAD_REQUEST);
            }
        }
    }

    private Oneseo findOneseo(String phoneNumber, String oneseoCode, TestType testType) {
        // 1차 전형 결과 조회에는 접수번호, 2차 전형 결과 조회에는 수험번호로 조회

        return testType.equals(FIRST)
                ? oneseoRepository.findByGuardianOrTeacherPhoneNumberAndSubmitCode(phoneNumber, oneseoCode).orElseThrow(
                        () -> new ExpectedException("해당 전화번호, 접수번호로 작성된 원서를 찾을 수 없습니다.", HttpStatus.BAD_REQUEST))
                : oneseoRepository.findByGuardianOrTeacherPhoneNumberAndExaminationNumber(phoneNumber, oneseoCode)
                        .orElseThrow(() -> new ExpectedException("해당 전화번호, 수험번호로 작성된 원서를 찾을 수 없습니다.",
                                HttpStatus.BAD_REQUEST));
    }

    private FoundTestResultResDto getResDto(Oneseo findOneseo, TestType testType) {
        return testType.equals(FIRST)
                ? buildFirstTestResultResDto(findOneseo)
                : buildSecondTestResultResDto(findOneseo);
    }

    private FoundTestResultResDto buildFirstTestResultResDto(Oneseo findOneseo) {
        return FoundTestResultResDto.builder().name(findOneseo.getMember().getName())
                .firstTestPassYn(findOneseo.getEntranceTestResult().getFirstTestPassYn()).build();
    }

    private FoundTestResultResDto buildSecondTestResultResDto(Oneseo findOneseo) {
        return FoundTestResultResDto.builder().name(findOneseo.getMember().getName())
                .firstTestPassYn(findOneseo.getEntranceTestResult().getFirstTestPassYn())
                .secondTestPassYn(findOneseo.getEntranceTestResult().getSecondTestPassYn())
                .decidedMajor(findOneseo.getDecidedMajor()).build();
    }
}
