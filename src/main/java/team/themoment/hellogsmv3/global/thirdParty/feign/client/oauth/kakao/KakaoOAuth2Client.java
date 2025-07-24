package team.themoment.hellogsmv3.global.thirdParty.feign.client.oauth.kakao;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import team.themoment.hellogsmv3.global.thirdParty.feign.client.dto.response.KakaoTokenResDto;
import team.themoment.hellogsmv3.global.thirdParty.feign.config.FeignConfig;

@FeignClient(name = "kakao-oauth2-client", url = "https://kauth.kakao.com", configuration = FeignConfig.class)
public interface KakaoOAuth2Client {

    @PostMapping(value = "/oauth/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    KakaoTokenResDto exchangeCodeForToken(
            @RequestParam("grant_type") String grantType,
            @RequestParam("client_id") String clientId,
            @RequestParam("client_secret") String clientSecret,
            @RequestParam("code") String code,
            @RequestParam("redirect_uri") String redirectUri
    );
}
