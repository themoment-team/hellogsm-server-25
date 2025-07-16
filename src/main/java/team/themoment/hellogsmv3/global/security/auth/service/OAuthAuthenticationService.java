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
import team.themoment.hellogsmv3.domain.member.repo.MemberRepository;
import team.themoment.hellogsmv3.global.exception.error.ExpectedException;
import team.themoment.hellogsmv3.global.thirdParty.feign.client.dto.response.GoogleTokenResponse;
import team.themoment.hellogsmv3.global.thirdParty.feign.client.dto.response.GoogleUserInfoResponse;
import team.themoment.hellogsmv3.global.thirdParty.feign.client.dto.response.KakaoTokenResponse;
import team.themoment.hellogsmv3.global.thirdParty.feign.client.dto.response.KakaoUserInfoResponse;
import team.themoment.hellogsmv3.global.thirdParty.feign.client.oauth.google.GoogleOAuth2Client;
import team.themoment.hellogsmv3.global.thirdParty.feign.client.oauth.google.GoogleUserInfoClient;
import team.themoment.hellogsmv3.global.thirdParty.feign.client.oauth.kakao.KakaoOAuth2Client;
import team.themoment.hellogsmv3.global.thirdParty.feign.client.oauth.kakao.KakaoUserInfoClient;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAuthAuthenticationService {

    private final ClientRegistrationRepository clientRegistrationRepository;
    private final GoogleOAuth2Client googleOAuth2Client;
    private final GoogleUserInfoClient googleUserInfoClient;
    private final KakaoOAuth2Client kakaoOAuth2Client;
    private final KakaoUserInfoClient kakaoUserInfoClient;
    private final MemberRepository memberRepository;

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
            log.error("OAuth 인증 실패 - 예상된 에러: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("OAuth 인증 처리 중 예상치 못한 오류 발생", e);
            throw new ExpectedException("OAuth 인증 처리 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void authenticateWithGoogle(String code, HttpServletRequest request) {
        ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId("google");

        String redirectUri = determineRedirectUri(request, clientRegistration.getRedirectUri());

        GoogleTokenResponse tokenResponse;
        try {
            tokenResponse = googleOAuth2Client.exchangeCodeForToken(
                    clientRegistration.getAuthorizationGrantType().getValue(),
                    clientRegistration.getClientId(),
                    clientRegistration.getClientSecret(),
                    code,
                    redirectUri
            );
        } catch (Exception e) {
            throw new ExpectedException("Google OAuth 토큰 교환에 실패했습니다: " + e.getMessage(), HttpStatus.UNAUTHORIZED);
        }
        GoogleUserInfoResponse userInfo;
        try {
            userInfo = googleUserInfoClient.getUserInfo("Bearer " + tokenResponse.accessToken());
            log.info("Google 사용자 정보 조회 성공: email={}, name={}", userInfo.email(), userInfo.name());
        } catch (Exception e) {
            log.error("Google 사용자 정보 조회 실패: {}", e.getMessage());
            throw new ExpectedException("Google 사용자 정보 조회에 실패했습니다: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        completeAuthentication("google", userInfo.email(), userInfo.name(), userInfo.picture(),
                AuthReferrerType.GOOGLE, request);
    }

    private void authenticateWithKakao(String code, HttpServletRequest request) {
        ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId("kakao");

        String redirectUri = determineRedirectUri(request, clientRegistration.getRedirectUri());

        KakaoTokenResponse tokenResponse;
        try {
            tokenResponse = kakaoOAuth2Client.exchangeCodeForToken(
                    clientRegistration.getAuthorizationGrantType().getValue(),
                    clientRegistration.getClientId(),
                    clientRegistration.getClientSecret(),
                    code,
                    redirectUri
            );
            log.info("Kakao 토큰 교환 성공!");
        } catch (Exception e) {
            log.error("Kakao 토큰 교환 실패: code={}, redirectUri={}, error={}",
                    code, redirectUri, e.getMessage());
            throw new ExpectedException("Kakao OAuth 토큰 교환에 실패했습니다: " + e.getMessage(), HttpStatus.UNAUTHORIZED);
        }

        KakaoUserInfoResponse userInfo = kakaoUserInfoClient.getUserInfo("Bearer " + tokenResponse.accessToken());
        log.info("Kakao 사용자 정보 조회 성공: email={}, nickname={}",
                userInfo.getEmail(), userInfo.getNickname());

        completeAuthentication("kakao", userInfo.getEmail(), userInfo.getNickname(),
                userInfo.kakaoAccount().profile().profileImageUrl(),
                AuthReferrerType.KAKAO, request);
    }

    private String determineRedirectUri(HttpServletRequest request, String defaultRedirectUri) {
        String origin = request.getHeader("Origin");
        String referer = request.getHeader("Referer");

        log.debug("Redirect URI 결정을 위한 정보 - Origin: {}, Referer: {}, Default: {}",
                origin, referer, defaultRedirectUri);

        if (origin != null && (origin.contains("localhost:3000") || origin.contains("localhost:8080"))) {
            return defaultRedirectUri;
        }

        return defaultRedirectUri;
    }

    private void completeAuthentication(String provider, String email, String name, String picture,
                                        AuthReferrerType authReferrerType, HttpServletRequest request) {

        // Member 엔티티 조회 또는 생성
        Member member = memberRepository.findByAuthReferrerTypeAndEmail(authReferrerType, email)
                .orElseGet(() -> memberRepository.save(Member.buildMemberWithOauthInfo(email, authReferrerType)));

        // OAuth2User 객체 생성
        OAuth2User oauth2User = createOAuth2User(email, name, picture, member, provider);

        // Authentication 객체 생성
        Authentication authentication = new OAuth2AuthenticationToken(
                oauth2User,
                oauth2User.getAuthorities(),
                provider
        );

        // SecurityContext에 저장
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 세션 설정
        setupSecuritySession(request, authentication);

        log.info("=== 인증 완료 ===");
        log.info("Provider: {}, User ID: {}, Member Role: {}, Email: {}",
                provider, member.getId(), member.getRole(), email);
    }

    private OAuth2User createOAuth2User(String email, String name, String picture, Member member, String provider) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", member.getId());
        attributes.put("email", email);
        attributes.put("name", name);
        attributes.put("picture", picture);
        attributes.put("role", member.getRole());
        attributes.put("provider", provider);
        attributes.put("provider_id", email);
        attributes.put("last_login_time", LocalDateTime.now());

        Collection<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority(member.getRole().name())
        );

        return new DefaultOAuth2User(authorities, attributes, "id");
    }

    private void setupSecuritySession(HttpServletRequest request, Authentication authentication) {
        HttpSession session = request.getSession(true);
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
    }
}
