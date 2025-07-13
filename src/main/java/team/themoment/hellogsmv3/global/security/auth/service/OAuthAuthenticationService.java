package team.themoment.hellogsmv3.global.security.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationExchange;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import team.themoment.hellogsmv3.domain.member.entity.Member;
import team.themoment.hellogsmv3.domain.member.entity.type.AuthReferrerType;
import team.themoment.hellogsmv3.domain.member.repo.MemberRepository;
import team.themoment.hellogsmv3.global.exception.error.ExpectedException;
import team.themoment.hellogsmv3.global.security.oauth.UserInfo;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

import static team.themoment.hellogsmv3.domain.member.entity.type.AuthReferrerType.GOOGLE;
import static team.themoment.hellogsmv3.domain.member.entity.type.AuthReferrerType.KAKAO;

/**
 * Spring Security OAuth2 기능을 활용한 커스텀 OAuth 인증 서비스
 * 
 * 기존 Spring Security OAuth2 플로우와 호환되도록 설계되었으며,
 * AuthorizationRequestRepository를 사용하여 state 및 nonce를 관리합니다.
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class OAuthAuthenticationService {

    private final ClientRegistrationRepository clientRegistrationRepository;
    private final MemberRepository memberRepository;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> tokenResponseClient = new DefaultAuthorizationCodeTokenResponseClient();
    private final OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService = new DefaultOAuth2UserService();
    
    // Spring Security에서 제공하는 AuthorizationRequestRepository 사용
    private final AuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository = new HttpSessionOAuth2AuthorizationRequestRepository();
    private static final SecureRandom secureRandom = new SecureRandom();
    

    public void authenticate(String provider, String code, HttpServletRequest request, HttpServletResponse response) {
        try {
            log.info("OAuth 인증 시작: provider={}", provider);
            
            ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId(provider);
            if (clientRegistration == null) {
                throw new ExpectedException("지원하지 않는 OAuth Provider입니다.", HttpStatus.BAD_REQUEST);
            }
            
            // Spring Security AuthorizationRequestRepository를 사용하여 state 검증
            String receivedState = request.getParameter("state");
            OAuth2AuthorizationRequest authorizationRequest = validateStateWithSpringSecurityRepository(request, receivedState);
            
            OAuth2AuthorizationCodeGrantRequest grantRequest = createGrantRequestWithSpringSecuritySupport(
                    clientRegistration, code, authorizationRequest, receivedState);
            OAuth2AccessTokenResponse tokenResponse = tokenResponseClient.getTokenResponse(grantRequest);
            OAuth2UserRequest userRequest = new OAuth2UserRequest(clientRegistration, tokenResponse.getAccessToken());
            OAuth2User oauth2User = oauth2UserService.loadUser(userRequest);
            
            Authentication authentication = processUserAuthentication(provider, oauth2User, clientRegistration);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // OAuth2AuthorizedClient 생성 및 저장 (Spring Security 표준 방식)
            OAuth2AuthorizedClient authorizedClient = new OAuth2AuthorizedClient(
                    clientRegistration, authentication.getName(), tokenResponse.getAccessToken());
            authorizedClientService.saveAuthorizedClient(authorizedClient, authentication);
            
            setupSessionCookie(request, response);
            
            log.info("OAuth 인증 완료: provider={}, userId={}", provider, authentication.getName());

        } catch (OAuth2AuthorizationException e) {
            log.error("OAuth 인증 오류: provider={}, error={}", provider, e.getError().getDescription());
            throw new ExpectedException("OAuth 인증 오류: " + e.getError().getDescription(), HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            log.error("OAuth 인증 처리 중 오류 발생: provider={}", provider, e);
            throw new ExpectedException("OAuth 인증 처리 중 오류가 발생했습니다: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Spring Security AuthorizationRequestRepository를 사용한 state 검증
     * 
     * 기존의 수동 세션 관리 대신 Spring Security의 표준 방식을 사용합니다.
     * 이는 더 안전하고 일관된 세션 관리를 제공합니다.
     */
    private OAuth2AuthorizationRequest validateStateWithSpringSecurityRepository(HttpServletRequest request, String receivedState) {
        if (!StringUtils.hasText(receivedState)) {
            log.error("State 파라미터가 누락되었습니다. CSRF 공격 가능성이 있습니다.");
            throw new ExpectedException("잘못된 요청입니다. 인증을 다시 시도해주세요.", HttpStatus.BAD_REQUEST);
        }
        
        // Spring Security AuthorizationRequestRepository에서 저장된 AuthorizationRequest 가져오기
        OAuth2AuthorizationRequest authorizationRequest = authorizationRequestRepository.removeAuthorizationRequest(request);
        
        if (authorizationRequest == null) {
            log.error("저장된 OAuth2AuthorizationRequest가 없습니다. 세션이 만료되었거나 잘못된 요청입니다.");
            throw new ExpectedException("세션이 만료되었습니다. 인증을 다시 시도해주세요.", HttpStatus.UNAUTHORIZED);
        }
        
        String storedState = authorizationRequest.getState();
        if (!StringUtils.hasText(storedState)) {
            log.error("AuthorizationRequest에 state가 없습니다. 잘못된 인증 플로우입니다.");
            throw new ExpectedException("잘못된 인증 요청입니다. 처음부터 다시 시도해주세요.", HttpStatus.BAD_REQUEST);
        }
        
        if (!storedState.equals(receivedState)) {
            log.error("State 파라미터가 일치하지 않습니다. CSRF 공격 가능성. stored={}, received={}", storedState, receivedState);
            throw new ExpectedException("보안 검증에 실패했습니다. 인증을 다시 시도해주세요.", HttpStatus.FORBIDDEN);
        }
        
        log.debug("Spring Security Repository를 사용한 State 파라미터 검증 완료: state={}", receivedState);
        return authorizationRequest;
    }
    
    /**
     * Spring Security의 OAuth2AuthorizationRequest를 활용한 GrantRequest 생성
     */
    private OAuth2AuthorizationCodeGrantRequest createGrantRequestWithSpringSecuritySupport(
            ClientRegistration clientRegistration, String code, 
            OAuth2AuthorizationRequest authorizationRequest, String state) {
        
        // OAuth2AuthorizationResponse 생성
        OAuth2AuthorizationResponse authorizationResponse = OAuth2AuthorizationResponse.success(code)
                .redirectUri(clientRegistration.getRedirectUri())
                .state(state)
                .build();
        
        // OAuth2AuthorizationExchange 생성
        OAuth2AuthorizationExchange authorizationExchange = new OAuth2AuthorizationExchange(
                authorizationRequest, authorizationResponse
        );
        
        return new OAuth2AuthorizationCodeGrantRequest(
                clientRegistration,
                authorizationExchange
        );
    }

    /**
     * State 파라미터를 검증하여 CSRF 공격을 방지합니다.
     * 
     * OAuth 2.0 RFC 6749에서 근리마다 CSRF 공격을 방지하기 위해 state 파라미터 사용을 강력히 권장합니다.
     * 
     * 보안 검증 실패 시나리오:
     * 1. State 파라미터 누락: CSRF 공격 가능성
     * 2. 세션 누락/만료: 비인가된 사용자 요청
     * 3. State 불일치: 중간자 공격(Man-in-the-Middle) 가능성
     * 4. State 재사용: Replay 공격 가능성
     * 
     * @param request HTTP 요청 객체
     * @param receivedState OAuth Provider로부터 받은 state 파라미터
     * @throws ExpectedException 보안 검증 실패 시 예외 발생
     */
    private void validateStateParameter(HttpServletRequest request, String receivedState) {
        if (!StringUtils.hasText(receivedState)) {
            log.error("State 파라미터가 누락되었습니다.");
            throw new ExpectedException("잘못된 요청입니다. 인증을 다시 시도해주세요.", HttpStatus.BAD_REQUEST);
        }
        
        HttpSession session = request.getSession(false);
        if (session == null) {
            log.error("세션이 존재하지 않습니다. 세션이 만료되었거나 잘못된 요청입니다.");
            throw new ExpectedException("세션이 만료되었습니다. 인증을 다시 시도해주세요.", HttpStatus.UNAUTHORIZED);
        }
        
        String storedState = (String) session.getAttribute(OAUTH_STATE_SESSION_KEY);
        if (!StringUtils.hasText(storedState)) {
            log.error("세션에 저장된 state가 없습니다. 잘못된 인증 플로우입니다.");
            throw new ExpectedException("잘못된 인증 요청입니다. 처음부터 다시 시도해주세요.", HttpStatus.BAD_REQUEST);
        }
        
        if (!storedState.equals(receivedState)) {
            log.error("State 파라미터가 일치하지 않습니다. stored={}, received={}", storedState, receivedState);
            throw new ExpectedException("보안 검증에 실패했습니다. 인증을 다시 시도해주세요.", HttpStatus.FORBIDDEN);
        }
        
        // 검증 완료 후 세션에서 state 제거 (재사용 방지)
        session.removeAttribute(OAUTH_STATE_SESSION_KEY);
        session.removeAttribute(OAUTH_NONCE_SESSION_KEY);
        
        log.debug("State 파라미터 검증 완료: state={}", receivedState);
    }

    private String generateSecureRandomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder result = new StringBuilder(length);
        
        for (int i = 0; i < length; i++) {
            result.append(chars.charAt(secureRandom.nextInt(chars.length())));
        }
        
        return result.toString();
    }
    
    /**
     * OAuth 인증을 위한 URL 생성 (프론트엔드에서 사용)
     */
    public String generateAuthorizationUrl(String provider, HttpServletRequest request) {
        ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId(provider);
        if (clientRegistration == null) {
            throw new ExpectedException("지원하지 않는 OAuth Provider입니다.", HttpStatus.BAD_REQUEST);
        }
        
        // 보안을 위한 state와 nonce 생성
        String state = generateSecureRandomString(32);
        String nonce = generateSecureRandomString(16);
        
        // 세션에 저장
        HttpSession session = request.getSession(true);
        session.setAttribute(OAUTH_STATE_SESSION_KEY, state);
        session.setAttribute(OAUTH_NONCE_SESSION_KEY, nonce);
        
        // Authorization URL 생성
        StringBuilder urlBuilder = new StringBuilder()
                .append(clientRegistration.getProviderDetails().getAuthorizationUri())
                .append("?response_type=code")
                .append("&client_id=").append(clientRegistration.getClientId())
                .append("&redirect_uri=").append(clientRegistration.getRedirectUri())
                .append("&state=").append(state);
        
        // Scope 추가
        if (!clientRegistration.getScopes().isEmpty()) {
            urlBuilder.append("&scope=").append(String.join("%20", clientRegistration.getScopes()));
        }
        
        // Provider별 추가 파라미터
        switch (provider.toLowerCase()) {
            case "google" -> {
                urlBuilder.append("&access_type=offline")
                          .append("&prompt=select_account")
                          .append("&nonce=").append(nonce);
            }
            case "kakao" -> {
                urlBuilder.append("&prompt=login");
            }
        }
        
        return urlBuilder.toString();
    }

    private Authentication processUserAuthentication(String provider, OAuth2User oauth2User, ClientRegistration clientRegistration) {
        Map<String, Object> attributes = oauth2User.getAttributes();
        
        String providerId;
        AuthReferrerType authRefType;
        switch (provider.toLowerCase()) {
            case "kakao" -> {
                Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
                providerId = Optional.ofNullable(kakaoAccount)
                        .map(account -> account.get("email"))
                        .map(Object::toString)
                        .orElse(attributes.get("id").toString());
                authRefType = KAKAO;
            }
            case "google" -> {
                providerId = attributes.get("email").toString();
                authRefType = GOOGLE;
            }
            default -> throw new ExpectedException("지원하지 않는 OAuth Provider입니다.", HttpStatus.BAD_REQUEST);
        }
        Member member = memberRepository.findByAuthReferrerTypeAndEmail(authRefType, providerId)
                .orElseGet(() -> memberRepository.save(Member.buildMemberWithOauthInfo(providerId, authRefType)));
        Map<String, Object> userInfoAttributes = new HashMap<>();
        userInfoAttributes.put("id", member.getId());
        userInfoAttributes.put("role", member.getRole());
        userInfoAttributes.put("provider", provider);
        userInfoAttributes.put("provider_id", providerId);
        userInfoAttributes.put("last_login_time", LocalDateTime.now());

        List<String> authorities = new ArrayList<>();
        authorities.add(member.getRole().name());

        UserInfo userInfo = new UserInfo(
                authorities.stream()
                        .map(SimpleGrantedAuthority::new)
                        .toList(),
                userInfoAttributes,
                "id"
        );

        return new OAuth2AuthenticationToken(
                userInfo, 
                userInfo.getAuthorities(), 
                provider
        );
    }

    private void setupSessionCookie(HttpServletRequest request, HttpServletResponse response) {
        // Spring Security가 자동으로 세션을 관리하므로 별도의 쿠키 설정은 필요 없음
        // 필요시 추가적인 세션 설정을 여기서 수행
        request.getSession(true); // 세션 생성 또는 기존 세션 사용
    }
}
