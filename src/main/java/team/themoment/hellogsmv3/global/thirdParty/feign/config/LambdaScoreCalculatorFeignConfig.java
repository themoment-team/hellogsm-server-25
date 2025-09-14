package team.themoment.hellogsmv3.global.thirdParty.feign.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static team.themoment.hellogsmv3.global.security.data.HeaderConstant.X_HG_API_KEY;

@Configuration
@RequiredArgsConstructor
public class LambdaScoreCalculatorFeignConfig {

    @Value("${lambda-score-calculator.api-key}")
    private String lambdaApiKey;

    @Bean
    public RequestInterceptor lambdaApiKeyInterceptor() {
        return template -> template.header(X_HG_API_KEY, lambdaApiKey);
    }
}
