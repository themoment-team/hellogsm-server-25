package team.themoment.hellogsmv3.global.thirdParty.feign.client.lambda;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.CalculatedScoreResDto;
import team.themoment.hellogsmv3.global.thirdParty.feign.client.dto.request.LambdaScoreCalculatorReqDto;

import static team.themoment.hellogsmv3.global.security.data.HeaderConstant.X_HG_API_KEY;

@FeignClient(name = "lambda-score-calculator-client", url = "${lambda-score-calculator.url}")
public interface LambdaScoreCalculatorClient {
    @PostMapping
    CalculatedScoreResDto calculateScore(
            @RequestBody LambdaScoreCalculatorReqDto reqDto,
            @RequestHeader(X_HG_API_KEY) String apiKey
    );
}