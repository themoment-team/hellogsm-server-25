package team.themoment.hellogsmv3.global.security.auth.service.provider;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import team.themoment.hellogsmv3.domain.member.entity.type.AuthReferrerType;
import team.themoment.hellogsmv3.global.exception.error.ExpectedException;
import team.themoment.hellogsmv3.global.security.auth.dto.UserAuthInfo;
import team.themoment.hellogsmv3.global.thirdParty.feign.client.dto.response.KakaoTokenResDto;
import team.themoment.hellogsmv3.global.thirdParty.feign.client.dto.response.KakaoUserInfoResDto;
import team.themoment.hellogsmv3.global.thirdParty.feign.client.oauth.kakao.KakaoOAuth2Client;
import team.themoment.hellogsmv3.global.thirdParty.feign.client.oauth.kakao.KakaoUserInfoClient;

@Component
@RequiredArgsConstructor
public class KakaoOAuthProvider implements OAuthProvider {
    
    private static final String PROVIDER_NAME = "kakao";
    private static final String TOKEN_PREFIX = "Bearer ";
    
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final KakaoOAuth2Client kakaoOAuth2Client;
    private final KakaoUserInfoClient kakaoUserInfoClient;
    
    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }
    
    @Override
    public AuthReferrerType getAuthReferrerType() {
        return AuthReferrerType.KAKAO;
    }
    
    @Override
    public UserAuthInfo authenticate(String authorizationCode) {
        validateAuthorizationCode(authorizationCode);
        
        ClientRegistration clientRegistration = getClientRegistration();
        KakaoTokenResDto tokenResponse = exchangeCodeForToken(authorizationCode, clientRegistration);
        KakaoUserInfoResDto userInfo = getUserInfo(tokenResponse.accessToken());
        
        String providerId = extractUserEmail(userInfo);
        providerId = providerId == null ? userInfo.id().toString() : providerId;
        validateUserEmail(providerId);
        
        return new UserAuthInfo(providerId, PROVIDER_NAME, getAuthReferrerType());
    }
    
    private void validateAuthorizationCode(String code) {
        if (!StringUtils.hasText(code)) {
            throw new ExpectedException("Authorization code가 비어있습니다.", HttpStatus.BAD_REQUEST);
        }
    }
    
    private ClientRegistration getClientRegistration() {
        ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId(PROVIDER_NAME);
        if (clientRegistration == null) {
            throw new ExpectedException("Kakao OAuth 클라이언트 설정을 찾을 수 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return clientRegistration;
    }
    
    private KakaoTokenResDto exchangeCodeForToken(String code, ClientRegistration clientRegistration) {
        try {
            return kakaoOAuth2Client.exchangeCodeForToken(
                    clientRegistration.getAuthorizationGrantType().getValue(),
                    clientRegistration.getClientId(),
                    clientRegistration.getClientSecret(),
                    code,
                    clientRegistration.getRedirectUri()
            );
        } catch (Exception e) {
            throw new ExpectedException("Kakao OAuth 토큰 교환에 실패했습니다: " + e.getMessage(), HttpStatus.UNAUTHORIZED);
        }
    }
    
    private KakaoUserInfoResDto getUserInfo(String accessToken) {
        try {
            return kakaoUserInfoClient.getUserInfo(TOKEN_PREFIX + accessToken);
        } catch (Exception e) {
            throw new ExpectedException("Kakao 사용자 정보 조회에 실패했습니다: " + e.getMessage(), HttpStatus.UNAUTHORIZED);
        }
    }
    
    private String extractUserEmail(KakaoUserInfoResDto userInfo) {
        return userInfo.getEmail();
    }
    
    private void validateUserEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new ExpectedException("Kakao 사용자 이메일 정보를 가져올 수 없습니다.", HttpStatus.UNAUTHORIZED);
        }
    }
}