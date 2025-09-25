package team.themoment.hellogsmv3.global.thirdParty.feign.client.oauth.google;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import team.themoment.hellogsmv3.global.thirdParty.feign.client.dto.response.GoogleTokenResDto;
import team.themoment.hellogsmv3.global.thirdParty.feign.config.FeignConfig;

@FeignClient(name = "google-oauth2-client", url = "https://oauth2.googleapis.com", configuration = FeignConfig.class)
public interface GoogleOAuth2Client {

    @PostMapping(value = "/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    GoogleTokenResDto exchangeCodeForToken(@RequestParam("grant_type") String grantType,
            @RequestParam("client_id") String clientId, @RequestParam("client_secret") String clientSecret,
            @RequestParam("code") String code, @RequestParam("redirect_uri") String redirectUri);
}
