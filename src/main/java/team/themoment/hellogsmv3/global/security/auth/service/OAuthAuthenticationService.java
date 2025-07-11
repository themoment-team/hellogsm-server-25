package team.themoment.hellogsmv3.global.security.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationExchange;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import team.themoment.hellogsmv3.domain.member.entity.Member;
import team.themoment.hellogsmv3.domain.member.entity.type.AuthReferrerType;
import team.themoment.hellogsmv3.domain.member.repo.MemberRepository;
import team.themoment.hellogsmv3.global.exception.error.ExpectedException;
import team.themoment.hellogsmv3.global.security.oauth.UserInfo;

import java.time.LocalDateTime;
import java.util.*;

import static team.themoment.hellogsmv3.domain.member.entity.type.AuthReferrerType.GOOGLE;
import static team.themoment.hellogsmv3.domain.member.entity.type.AuthReferrerType.KAKAO;

@Service
@RequiredArgsConstructor
public class OAuthAuthenticationService {

    private final ClientRegistrationRepository clientRegistrationRepository;
    private final MemberRepository memberRepository;
    private final OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> tokenResponseClient = new DefaultAuthorizationCodeTokenResponseClient();
    private final OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService = new DefaultOAuth2UserService();
    

    public void authenticate(String provider, String code, HttpServletRequest request, HttpServletResponse response) {
        try {
            ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId(provider);
            if (clientRegistration == null) {
                throw new ExpectedException("지원하지 않는 OAuth Provider입니다.", HttpStatus.BAD_REQUEST);
            }
            OAuth2AuthorizationCodeGrantRequest grantRequest = createGrantRequest(clientRegistration, code);
            OAuth2AccessTokenResponse tokenResponse = tokenResponseClient.getTokenResponse(grantRequest);
            OAuth2UserRequest userRequest = new OAuth2UserRequest(clientRegistration, tokenResponse.getAccessToken());
            OAuth2User oauth2User = oauth2UserService.loadUser(userRequest);
            Authentication authentication = processUserAuthentication(provider, oauth2User, clientRegistration);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            setupSessionCookie(request, response);

        } catch (OAuth2AuthorizationException e) {
            throw new ExpectedException("OAuth 인증 오류: " + e.getError().getDescription(), HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            throw new ExpectedException("OAuth 인증 처리 중 오류가 발생했습니다: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private OAuth2AuthorizationCodeGrantRequest createGrantRequest(ClientRegistration clientRegistration, String code) {
        // OAuth2AuthorizationRequest 생성 (간단한 형태)
        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                .clientId(clientRegistration.getClientId())
                .authorizationUri(clientRegistration.getProviderDetails().getAuthorizationUri())
                .redirectUri(clientRegistration.getRedirectUri())
                .scopes(clientRegistration.getScopes())
                .state("state") // 간단한 state
                .build();
        
        // OAuth2AuthorizationResponse 생성
        OAuth2AuthorizationResponse authorizationResponse = OAuth2AuthorizationResponse.success(code)
                .redirectUri(clientRegistration.getRedirectUri())
                .state("state")
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
