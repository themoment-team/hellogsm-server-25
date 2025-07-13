package team.themoment.hellogsmv3.global.security.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationExchange;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponse;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import team.themoment.hellogsmv3.global.exception.error.ExpectedException;
import team.themoment.hellogsmv3.global.security.oauth.CustomOauth2UserService;

/**
 * 
 * PDF 플로우를 구현하면서도 Spring Security의 검증된 컴포넌트들을 활용합니다.
 * 서비스 계층은 비즈니스 로직만 처리하고, 컨트롤러에서 응답을 래핑합니다.
 * 
 * - OAuth2AccessTokenResponseClient: 안전한 토큰 교환
 * - CustomOauth2UserService: 기존 사용자 정보 처리 로직 재사용
 * - OAuth2AuthorizedClientService: 표준 방식의 클라이언트 정보 저장
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OAuthAuthenticationService {

    private final ClientRegistrationRepository clientRegistrationRepository;
    private final OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> tokenResponseClient;
    private final CustomOauth2UserService oauth2UserService; // 기존 서비스 재사용
    private final OAuth2AuthorizedClientService authorizedClientService;

    /**
     * 프론트엔드에서 받은 Authorization Code로 OAuth 인증을 처리합니다.
     * 
     * @param provider OAuth Provider (google, kakao)
     * @param code Authorization Code
     * @param request HTTP 요청
     * @param response HTTP 응답
     * @throws ExpectedException 인증 실패 시
     */
    public void authenticate(String provider, String code, 
                           HttpServletRequest request, HttpServletResponse response) {
        try {
            log.info("OAuth 인증 시작: provider={}", provider);
            
            // 1. ClientRegistration 조회 (Spring Security 표준 방식)
            ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId(provider);
            if (clientRegistration == null) {
                throw new ExpectedException("지원하지 않는 OAuth Provider입니다.", HttpStatus.BAD_REQUEST);
            }
            
            // 2. OAuth2AuthorizationCodeGrantRequest 생성 (표준 방식)
            OAuth2AuthorizationCodeGrantRequest grantRequest = createAuthorizationCodeGrantRequest(
                clientRegistration, code);
            
            // 3. Access Token 교환 (Spring Security 표준 TokenResponseClient 사용)
            OAuth2AccessTokenResponse tokenResponse = tokenResponseClient.getTokenResponse(grantRequest);
            
            // 4. 사용자 정보 조회 (기존 CustomOauth2UserService 재사용)
            OAuth2UserRequest userRequest = new OAuth2UserRequest(clientRegistration, tokenResponse.getAccessToken());
            OAuth2User oauth2User = oauth2UserService.loadUser(userRequest);
            
            // 5. Authentication 객체 생성 (Spring Security 표준 방식)
            Authentication authentication = createAuthentication(oauth2User, clientRegistration);
            
            // 6. SecurityContext에 저장
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // 7. 세션 설정 (Spring Security 표준 방식)
            setupSecuritySession(request, authentication);
            
            // 8. OAuth2AuthorizedClient 저장 (표준 방식)
            saveAuthorizedClient(clientRegistration, authentication, tokenResponse);
            
            log.info("OAuth 인증 완료: provider={}, userId={}", provider, authentication.getName());
            
        } catch (OAuth2AuthorizationException e) {
            log.error("OAuth 인증 오류: provider={}, error={}", provider, e.getError().getDescription());
            throw new ExpectedException("OAuth 인증 오류: " + e.getError().getDescription(), HttpStatus.UNAUTHORIZED);
        } catch (ExpectedException e) {
            // ExpectedException은 그대로 전파
            throw e;
        } catch (Exception e) {
            log.error("OAuth 인증 처리 중 오류 발생: provider={}", provider, e);
            throw new ExpectedException("OAuth 인증 처리 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Spring Security 간소화된 방식으로 OAuth2AuthorizationCodeGrantRequest 생성
     * 
     * PDF 플로우에서는 프론트엔드가 OAuth를 직접 처리하므로
     * state 검증을 간소화할 수 있습니다.
     */
    private OAuth2AuthorizationCodeGrantRequest createAuthorizationCodeGrantRequest(
            ClientRegistration clientRegistration, String code) {
        
        // 간소화된 OAuth2AuthorizationRequest 생성
        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                .clientId(clientRegistration.getClientId())
                .authorizationUri(clientRegistration.getProviderDetails().getAuthorizationUri())
                .redirectUri(clientRegistration.getRedirectUri())
                .scopes(clientRegistration.getScopes())
                .build();
        
        // OAuth2AuthorizationResponse 생성 (state 없이)
        OAuth2AuthorizationResponse authorizationResponse = OAuth2AuthorizationResponse.success(code)
                .redirectUri(clientRegistration.getRedirectUri())
                .build();
        
        // OAuth2AuthorizationExchange 생성
        OAuth2AuthorizationExchange authorizationExchange = new OAuth2AuthorizationExchange(
                authorizationRequest, authorizationResponse);
        
        return new OAuth2AuthorizationCodeGrantRequest(clientRegistration, authorizationExchange);
    }
    
    /**
     * Spring Security 표준 Authentication 객체 생성
     */
    private Authentication createAuthentication(OAuth2User oauth2User, ClientRegistration clientRegistration) {
        return new OAuth2AuthenticationToken(
                oauth2User, 
                oauth2User.getAuthorities(), 
                clientRegistration.getRegistrationId()
        );
    }
    
    /**
     * Spring Security 표준 세션 설정
     */
    private void setupSecuritySession(HttpServletRequest request, Authentication authentication) {
        HttpSession session = request.getSession(true);
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
    }
    
    /**
     * Spring Security 표준 OAuth2AuthorizedClient 저장
     */
    private void saveAuthorizedClient(ClientRegistration clientRegistration, 
                                     Authentication authentication, 
                                     OAuth2AccessTokenResponse tokenResponse) {
        OAuth2AuthorizedClient authorizedClient = new OAuth2AuthorizedClient(
                clientRegistration, 
                authentication.getName(), 
                tokenResponse.getAccessToken(),
                tokenResponse.getRefreshToken()
        );
        authorizedClientService.saveAuthorizedClient(authorizedClient, authentication);
    }

}
