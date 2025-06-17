package team.themoment.hellogsmv3.domain.common.testResult.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.themoment.hellogsmv3.domain.common.testResult.dto.response.FoundTestResultResDto;
import team.themoment.hellogsmv3.domain.oneseo.entity.Oneseo;
import team.themoment.hellogsmv3.domain.oneseo.repository.OneseoRepository;
import team.themoment.hellogsmv3.domain.oneseo.service.OneseoService;
import team.themoment.hellogsmv3.global.exception.error.ExpectedException;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class QueryFirstTestResultByNameAndBirthService {

    private final OneseoService oneseoService;
    private final OneseoRepository oneseoRepository;

    @Transactional(readOnly = true)
    public FoundTestResultResDto execute(String name, LocalDate birth) {
        if(oneseoService.validateFirstTestResultAnnouncement()) {
            throw new ExpectedException("1차 전형 결과 발표 전 입니다.", HttpStatus.BAD_REQUEST);
        }
        Oneseo oneseo = findOneseo(name, birth);
        return FoundTestResultResDto.builder()
                .name(maskingName(oneseo.getMember().getName()))
                .firstTestPassYn(oneseo.getEntranceTestResult().getFirstTestPassYn())
                .build();
    }

    private Oneseo findOneseo(String name, LocalDate birth) {
        return oneseoRepository.findByMemberNameAndMemberBirth(name, birth)
                .orElseThrow(() -> new ExpectedException("해당 이름, 생년월일의 정보를 가진 원서를 찾을 수 없습니다.", HttpStatus.BAD_REQUEST));
    }

    private String maskingName(String name) {
        int length = name.length();

        if (length <= 1) {
            return name;
        } else if (length == 2) {
            return name.charAt(0) + "*";
        } else if (length == 3) {
            return name.charAt(0) + "*" + name.charAt(2);
        } else {
            return name.charAt(0) +
                    "*".repeat(length - 2) +
                    name.charAt(length - 1);
        }
    }
}
