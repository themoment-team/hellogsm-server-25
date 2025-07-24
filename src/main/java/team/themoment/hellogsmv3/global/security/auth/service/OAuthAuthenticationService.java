package team.themoment.hellogsmv3.global.security.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
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

    private static final String TOKEN_PREFIX = "Bearer ";
    private static final String GOOGLE_PROVIDER = "google";
    private static final String KAKAO_PROVIDER = "kakao";

    public void execute(String provider, String code, HttpServletRequest request) {
        String decodedCode = URLDecoder.decode(code, StandardCharsets.UTF_8);
        
        try {
            switch (provider.toLowerCase()) {
                case GOOGLE_PROVIDER -> authenticateWithGoogle(decodedCode, request);
                case KAKAO_PROVIDER -> authenticateWithKakao(decodedCode, request);
                default -> throw new ExpectedException("지원하지 않는 OAuth Provider입니다: " + provider, HttpStatus.BAD_REQUEST);
            }
        } catch (ExpectedException e) {
            throw e;
        } catch (Exception e) {
            throw new ExpectedException("OAuth 인증 처리 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void authenticateWithGoogle(String code, HttpServletRequest request) {
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
        } catch (Exception e) {
            throw new ExpectedException("Google OAuth 토큰 교환에 실패했습니다", HttpStatus.UNAUTHORIZED);
        }
        GoogleUserInfoResDto userInfo = googleUserInfoClient.getUserInfo(TOKEN_PREFIX + tokenResponse.accessToken());
        completeAuthentication(GOOGLE_PROVIDER, userInfo.email(), AuthReferrerType.GOOGLE, request);
    }

    private void authenticateWithKakao(String code, HttpServletRequest request) {
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
        } catch (Exception e) {
            throw new ExpectedException("Kakao OAuth 토큰 교환에 실패했습니다", HttpStatus.UNAUTHORIZED);
        }
        KakaoUserInfoResDto userInfo = kakaoUserInfoClient.getUserInfo(TOKEN_PREFIX + tokenResponse.accessToken());
        completeAuthentication(KAKAO_PROVIDER, userInfo.getEmail(), AuthReferrerType.KAKAO, request);
    }

    private void completeAuthentication(String provider, String providerId, 
                                        AuthReferrerType authReferrerType, HttpServletRequest request) {
        Member member = getUser(providerId, authReferrerType);
        OAuth2User oauth2User = createOAuth2User(member, provider, providerId);
        Authentication authentication = new OAuth2AuthenticationToken(
                oauth2User,
                oauth2User.getAuthorities(),
                provider
        );
        setSecurityContext(request, authentication);
    }

    private Member getUser(String providerId, AuthReferrerType authRefType) {
        return memberRepository.findByAuthReferrerTypeAndEmail(authRefType, providerId)
                .orElseGet(() -> memberRepository.save(Member.buildMemberWithOauthInfo(providerId, authRefType)));
    }

    private OAuth2User createOAuth2User(Member member, String provider, String providerId) {
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
        return new DefaultOAuth2User(authorities, attributes, nameAttribute);
    }

    private Collection<GrantedAuthority> createAuthorities(Role role) {
        Role userRole = role != null ? role : Role.UNAUTHENTICATED;
        return List.of(
                new SimpleGrantedAuthority("OAUTH2_USER"),
                new SimpleGrantedAuthority("SCOPE_email"),
                new SimpleGrantedAuthority(userRole.name())
        );
    }

    private void setSecurityContext(HttpServletRequest request, Authentication authentication) {
        SecurityContextHolder.getContext().setAuthentication(authentication);
        HttpSession session = request.getSession(true);
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
    }
}