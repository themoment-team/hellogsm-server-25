package team.themoment.hellogsmv3.global.thirdParty.feign.client.lambda;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.CalculatedScoreResDto;
import team.themoment.hellogsmv3.global.thirdParty.feign.client.dto.request.LambdaScoreCalculatorReqDto;

@FeignClient(name = "lambda-score-calculator-client", url = "https://gpkaawuftwwq5xvrwz3a6iux7m0tsouq.lambda-url.ap-northeast-2.on.aws")
public interface LambdaScoreCalculatorClient {
    @PostMapping
    CalculatedScoreResDto calculateScore(@RequestBody LambdaScoreCalculatorReqDto reqDto);
}