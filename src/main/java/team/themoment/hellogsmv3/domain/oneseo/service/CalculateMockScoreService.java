package team.themoment.hellogsmv3.domain.oneseo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import team.themoment.hellogsmv3.domain.oneseo.dto.request.MiddleSchoolAchievementReqDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.CalculatedScoreResDto;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.GraduationType;

@Service
@RequiredArgsConstructor
public class CalculateMockScoreService {

    private final CalculateGradeService calculateGradeService;
    private final CalculateGedService calculateGedService;

    public CalculatedScoreResDto execute(MiddleSchoolAchievementReqDto dto, GraduationType graduationType) {
        return switch (graduationType) {
            case CANDIDATE, GRADUATE -> calculateGradeService.execute(dto, null, graduationType);
            case GED -> calculateGedService.execute(dto, null, graduationType);
        };
    }

}
