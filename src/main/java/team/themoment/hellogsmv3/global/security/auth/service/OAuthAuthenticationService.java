package team.themoment.hellogsmv3.global.security.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import team.themoment.hellogsmv3.domain.member.entity.Member;
import team.themoment.hellogsmv3.domain.member.entity.type.AuthReferrerType;
import team.themoment.hellogsmv3.domain.member.entity.type.Role;
import team.themoment.hellogsmv3.domain.member.repo.MemberRepository;
import team.themoment.hellogsmv3.global.exception.error.ExpectedException;
import team.themoment.hellogsmv3.global.thirdParty.feign.client.dto.response.GoogleTokenResDto;
import team.themoment.hellogsmv3.global.thirdParty.feign.client.dto.response.GoogleUserInfoResDto;
import team.themoment.hellogsmv3.global.thirdParty.feign.client.dto.response.KakaoTokenResDto;
import team.themoment.hellogsmv3.global.thirdParty.feign.client.dto.response.KakaoUserInfoResDto;
import team.themoment.hellogsmv3.global.thirdParty.feign.client.oauth.google.GoogleOAuth2Client;
import team.themoment.hellogsmv3.global.thirdParty.feign.client.oauth.google.GoogleUserInfoClient;
import team.themoment.hellogsmv3.global.thirdParty.feign.client.oauth.kakao.KakaoOAuth2Client;
import team.themoment.hellogsmv3.global.thirdParty.feign.client.oauth.kakao.KakaoUserInfoClient;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class OAuthAuthenticationService {

    private final ClientRegistrationRepository clientRegistrationRepository;
    private final GoogleOAuth2Client googleOAuth2Client;
    private final GoogleUserInfoClient googleUserInfoClient;
    private final KakaoOAuth2Client kakaoOAuth2Client;
    private final KakaoUserInfoClient kakaoUserInfoClient;
    private final MemberRepository memberRepository;

    // 수정: 상수로 정의하여 재사용성과 가독성 향상
    private static final String TOKEN_PREFIX = "Bearer ";
    private static final String GOOGLE_PROVIDER = "google";
    private static final String KAKAO_PROVIDER = "kakao";

    /**
     * OAuth 인증 처리 메인 메서드
     * 
     * @param provider OAuth Provider (google, kakao)
     * @param code Authorization Code
     * @param request HTTP 요청 객체
     */
    public void execute(String provider, String code, HttpServletRequest request) {
        String decodedCode = URLDecoder.decode(code, StandardCharsets.UTF_8);
        
        try {
            log.debug("OAuth 인증 시작 - provider: {}", provider);
            
            // 수정: switch 표현식으로 간소화
            switch (provider.toLowerCase()) {
                case GOOGLE_PROVIDER -> authenticateWithGoogle(decodedCode, request);
                case KAKAO_PROVIDER -> authenticateWithKakao(decodedCode, request);
                default -> throw new ExpectedException("지원하지 않는 OAuth Provider입니다: " + provider, HttpStatus.BAD_REQUEST);
            }
            
            log.debug("OAuth 인증 완료 - provider: {}", provider);
            
        } catch (ExpectedException e) {
            log.error("OAuth 인증 실패 - provider: {}, 에러: {}", provider, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("OAuth 인증 처리 중 예상치 못한 오류 발생 - provider: {}, 에러: {}", provider, e.getMessage(), e);
            throw new ExpectedException("OAuth 인증 처리 중 오류가 발생했습니다: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Google OAuth 인증 처리
     */
    private void authenticateWithGoogle(String code, HttpServletRequest request) {
        log.debug("Google OAuth 토큰 교환 시작");
        
        ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId(GOOGLE_PROVIDER);

        GoogleTokenResDto tokenResponse;
        try {
            tokenResponse = googleOAuth2Client.exchangeCodeForToken(
                    clientRegistration.getAuthorizationGrantType().getValue(),
                    clientRegistration.getClientId(),
                    clientRegistration.getClientSecret(),
                    code,
                    clientRegistration.getRedirectUri()
            );
            log.debug("Google OAuth 토큰 교환 성공");
        } catch (Exception e) {
            log.error("Google OAuth 토큰 교환 실패", e);
            throw new ExpectedException("Google OAuth 토큰 교환에 실패했습니다", HttpStatus.UNAUTHORIZED);
        }
        
        log.debug("Google 사용자 정보 조회 시작");
        GoogleUserInfoResDto userInfo = googleUserInfoClient.getUserInfo(TOKEN_PREFIX + tokenResponse.accessToken());
        log.debug("Google 사용자 정보 조회 성공 - email: {}", userInfo.email());
        
        completeAuthentication(GOOGLE_PROVIDER, userInfo.email(), AuthReferrerType.GOOGLE, request);
    }

    /**
     * Kakao OAuth 인증 처리
     */
    private void authenticateWithKakao(String code, HttpServletRequest request) {
        log.debug("Kakao OAuth 토큰 교환 시작");
        
        ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId(KAKAO_PROVIDER);

        KakaoTokenResDto tokenResponse;
        try {
            tokenResponse = kakaoOAuth2Client.exchangeCodeForToken(
                    clientRegistration.getAuthorizationGrantType().getValue(),
                    clientRegistration.getClientId(),
                    clientRegistration.getClientSecret(),
                    code,
                    clientRegistration.getRedirectUri()
            );
            log.debug("Kakao OAuth 토큰 교환 성공");
        } catch (Exception e) {
            log.error("Kakao OAuth 토큰 교환 실패", e);
            throw new ExpectedException("Kakao OAuth 토큰 교환에 실패했습니다", HttpStatus.UNAUTHORIZED);
        }

        log.debug("Kakao 사용자 정보 조회 시작");
        KakaoUserInfoResDto userInfo = kakaoUserInfoClient.getUserInfo(TOKEN_PREFIX + tokenResponse.accessToken());
        log.debug("Kakao 사용자 정보 조회 성공 - email: {}", userInfo.getEmail());
        
        completeAuthentication(KAKAO_PROVIDER, userInfo.getEmail(), AuthReferrerType.KAKAO, request);
    }

    /**
     * 인증 완료 처리
     * 사용자 정보 저장 및 Spring Security 컨텍스트 설정
     */
    private void completeAuthentication(String provider, String providerId, 
                                        AuthReferrerType authReferrerType, HttpServletRequest request) {
        try {
            log.debug("인증 완료 처리 시작 - providerId: {}, provider: {}", providerId, provider);
            
            // 수정: 기존 코드와 동일한 방식으로 사용자 조회/생성
            Member member = getUser(providerId, authReferrerType);
            log.debug("Member 조회/생성 완료 - memberId: {}, role: {}", member.getId(), member.getRole());
            
            OAuth2User oauth2User = createOAuth2User(member, provider, providerId);
            log.debug("OAuth2User 생성 완료");

            Authentication authentication = new OAuth2AuthenticationToken(
                    oauth2User,
                    oauth2User.getAuthorities(),
                    provider
            );
            log.debug("Authentication 객체 생성 완료");
            
            // 수정: 보안 컨텍스트 설정을 별도 메서드로 분리
            setSecurityContext(request, authentication);
            log.debug("SecurityContext 설정 완료");
            
            log.debug("인증 완료 처리 성공");
        } catch (Exception e) {
            log.error("인증 완료 처리 중 오류 발생 - providerId: {}, provider: {}", providerId, provider, e);
            throw e;
        }
    }

    /**
     * 수정: 기존 코드와 동일한 방식으로 사용자 조회/생성
     */
    private Member getUser(String providerId, AuthReferrerType authRefType) {
        try {
            return memberRepository.findByAuthReferrerTypeAndEmail(authRefType, providerId)
                    .orElseGet(() -> {
                        log.debug("새로운 사용자 생성 - providerId: {}, authRefType: {}", providerId, authRefType);
                        return memberRepository.save(Member.buildMemberWithOauthInfo(providerId, authRefType));
                    });
        } catch (Exception e) {
            log.error("Member 조회/생성 중 오류 발생 - providerId: {}, authRefType: {}", providerId, authRefType, e);
            throw e;
        }
    }

    /**
     * 수정: 기존 코드와 동일한 구조로 OAuth2User 생성
     */
    private OAuth2User createOAuth2User(Member member, String provider, String providerId) {
        try {
            log.debug("OAuth2User 생성 시작 - memberId: {}, provider: {}, providerId: {}", 
                     member.getId(), provider, providerId);
            
            // 수정: 기존 코드와 동일한 속성 구조
            String nameAttribute = "id";
            Long id = member.getId();
            String roleAttribute = "role";
            Role role = member.getRole();
            String providerAttribute = "provider";
            String providerIdAttribute = "provider_id";
            String lastLoginTimeIdAttribute = "last_login_time";
            LocalDateTime lastLoginTime = LocalDateTime.now();

            Map<String, Object> attributes = new HashMap<>(Map.of(
                    nameAttribute, id,
                    roleAttribute, role,
                    providerAttribute, provider,
                    providerIdAttribute, providerId,
                    lastLoginTimeIdAttribute, lastLoginTime
            ));
            
            Collection<GrantedAuthority> authorities = createAuthorities(role);
            
            log.debug("OAuth2User 생성 완료 - attributes: {}", attributes);
            return new DefaultOAuth2User(authorities, attributes, nameAttribute);
        } catch (Exception e) {
            log.error("OAuth2User 생성 중 오류 발생 - memberId: {}, provider: {}, providerId: {}", 
                     member.getId(), provider, providerId, e);
            throw e;
        }
    }

    /**
     * 수정: 권한 생성 로직을 별도 메서드로 분리하여 재사용성 향상
     */
    private Collection<GrantedAuthority> createAuthorities(Role role) {
        try {
            // 수정: null 체크 추가
            Role userRole = role != null ? role : Role.UNAUTHENTICATED;
            
            return List.of(
                    new SimpleGrantedAuthority("OAUTH2_USER"),
                    new SimpleGrantedAuthority("SCOPE_email"),
                    new SimpleGrantedAuthority(userRole.name())
            );
        } catch (Exception e) {
            log.error("권한 생성 중 오류 발생 - role: {}", role, e);
            throw e;
        }
    }

    /**
     * 수정: 보안 컨텍스트 설정을 별도 메서드로 분리하고 메서드명 개선
     */
    private void setSecurityContext(HttpServletRequest request, Authentication authentication) {
        try {
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            HttpSession session = request.getSession(true);
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(authentication);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
            
            log.debug("SecurityContext 설정 완료 - sessionId: {}", session.getId());
        } catch (Exception e) {
            log.error("SecurityContext 설정 중 오류 발생", e);
            throw e;
        }
    }
}
