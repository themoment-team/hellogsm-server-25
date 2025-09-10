package team.themoment.hellogsmv3.domain.oneseo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import team.themoment.hellogsmv3.domain.oneseo.dto.request.MiddleSchoolAchievementReqDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.CalculatedScoreResDto;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.GraduationType;
import team.themoment.hellogsmv3.global.thirdParty.feign.client.dto.request.LambdaScoreCalculatorReqDto;
import team.themoment.hellogsmv3.global.thirdParty.feign.client.lambda.LambdaScoreCalculatorClient;

@Service
@RequiredArgsConstructor
public class CalculateMockScoreService {

    private final LambdaScoreCalculatorClient lambdaScoreCalculatorClient;

    public CalculatedScoreResDto execute(MiddleSchoolAchievementReqDto dto, GraduationType graduationType) {
        return lambdaScoreCalculatorClient.calculateScore(LambdaScoreCalculatorReqDto.from(dto, graduationType));
    }
}
