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

    private final String TOKEN_PREFIX = "Bearer ";

    public void execute(String provider, String code, HttpServletRequest request) {
        String decodedCode = URLDecoder.decode(code, StandardCharsets.UTF_8);
        try {
            switch (provider.toLowerCase()) {
                case "google" -> authenticateWithGoogle(decodedCode, request);
                case "kakao" -> authenticateWithKakao(decodedCode, request);
                default ->
                        throw new ExpectedException("지원하지 않는 OAuth Provider입니다: " + provider, HttpStatus.BAD_REQUEST);
            }
        } catch (ExpectedException e) {
            throw e;
        } catch (Exception e) {
            throw new ExpectedException("OAuth 인증 처리 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void authenticateWithGoogle(String code, HttpServletRequest request) {
        ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId("google");

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

        completeAuthentication("google", userInfo.email(), userInfo.name(), AuthReferrerType.GOOGLE, request);
    }

    private void authenticateWithKakao(String code, HttpServletRequest request) {
        ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId("kakao");

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

        completeAuthentication("kakao", userInfo.getEmail(), userInfo.getNickname(),
                AuthReferrerType.KAKAO, request);
    }

    private void completeAuthentication(String provider, String email, String name,
                                        AuthReferrerType authReferrerType, HttpServletRequest request) {
        Member member = memberRepository.findByAuthReferrerTypeAndEmail(authReferrerType, email)
                .orElseGet(() -> memberRepository.save(Member.buildMemberWithOauthInfo(email, authReferrerType)));
        OAuth2User oauth2User = createOAuth2User(email, name, member, provider);

        Authentication authentication = new OAuth2AuthenticationToken(
                oauth2User,
                oauth2User.getAuthorities(),
                provider
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        setupSecuritySession(request, authentication);
    }

    private OAuth2User createOAuth2User(String email, String name, Member member, String provider) {
        String nameAttribute = "id";
        Long id = member.getId();
        String emailAttribute = "email";
        String nameAttr = "name";
        String roleAttribute = "role";
        Role role = member.getRole();
        String providerAttribute = "provider";
        String providerIdAttribute = "provider_id";
        String lastLoginTimeAttribute = "last_login_time";
        LocalDateTime lastLoginTime = LocalDateTime.now();

        Map<String, Object> attributes = new HashMap<>(Map.of(
                nameAttribute, id,
                emailAttribute, email,
                nameAttr, name,
                roleAttribute, role,
                providerAttribute, provider,
                providerIdAttribute, email,
                lastLoginTimeAttribute, lastLoginTime
        ));
        
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("OAUTH2_USER"));
        authorities.add(new SimpleGrantedAuthority("SCOPE_email"));
        authorities.add(new SimpleGrantedAuthority(role.name()));

        return new DefaultOAuth2User(authorities, attributes, nameAttribute);
    }

    private void setupSecuritySession(HttpServletRequest request, Authentication authentication) {
        HttpSession session = request.getSession(true);
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
    }
}
