package team.themoment.hellogsmv3.global.security.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import team.themoment.hellogsmv3.domain.member.entity.Member;
import team.themoment.hellogsmv3.domain.member.entity.type.AuthReferrerType;
import team.themoment.hellogsmv3.domain.member.entity.type.Role;
import team.themoment.hellogsmv3.domain.member.repository.MemberRepository;
import team.themoment.hellogsmv3.global.security.auth.dto.UserAuthInfo;
import team.themoment.hellogsmv3.global.security.auth.service.provider.OAuthProvider;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAuthAuthenticationService {

    private final OAuthProviderFactory oAuthProviderFactory;
    private final MemberRepository memberRepository;

    public void execute(String provider, String code, HttpServletRequest request) {

        String decodedCode = URLDecoder.decode(code, StandardCharsets.UTF_8);
        OAuthProvider oAuthProvider = oAuthProviderFactory.getProvider(provider);

        UserAuthInfo userAuthInfo = oAuthProvider.authenticate(decodedCode);
        completeAuthentication(userAuthInfo, request);
    }

    private void completeAuthentication(UserAuthInfo userAuthInfo, HttpServletRequest request) {
        Member member = getOrCreateMember(userAuthInfo.email(), userAuthInfo.authReferrerType());
        OAuth2User oauth2User = createOAuth2User(member, userAuthInfo.provider(), userAuthInfo.email());
        Authentication authentication = new OAuth2AuthenticationToken(
                oauth2User,
                oauth2User.getAuthorities(),
                userAuthInfo.provider()
        );
        setSecurityContext(request, authentication);
    }

    private Member getOrCreateMember(String email, AuthReferrerType authReferrerType) {
        return memberRepository.findByAuthReferrerTypeAndEmail(authReferrerType, email)
                .orElseGet(() -> memberRepository.save(Member.buildMemberWithOauthInfo(email, authReferrerType)));
    }

    private OAuth2User createOAuth2User(Member member, String provider, String email) {
        Map<String, Object> attributes = createUserAttributes(member, provider, email);
        Collection<GrantedAuthority> authorities = createAuthorities(member.getRole());
        return new DefaultOAuth2User(authorities, attributes, "id");
    }

    private Map<String, Object> createUserAttributes(Member member, String provider, String email) {
        Role memberRole = Optional.ofNullable(member.getRole()).orElse(Role.UNAUTHENTICATED);
        return Map.of(
                "id", member.getId(),
                "role", memberRole,
                "provider", provider,
                "email", email,
                "last_login_time", LocalDateTime.now()
        );
    }

    private Collection<GrantedAuthority> createAuthorities(Role role) {
        Role userRole = Optional.ofNullable(role).orElse(Role.UNAUTHENTICATED);
        return List.of(
                new SimpleGrantedAuthority("OAUTH2_USER"),
                new SimpleGrantedAuthority("SCOPE_email"),
                new SimpleGrantedAuthority(userRole.name())
        );
    }

    private void setSecurityContext(HttpServletRequest request, Authentication authentication) {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);

        HttpSession oldSession = request.getSession(false);
        if (oldSession != null) {
            try {
                oldSession.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
                oldSession.getCreationTime();
                oldSession.getLastAccessedTime();
            } catch (IllegalStateException e) {
                try {
                    oldSession.invalidate();
                } catch (Exception ex) {
                    log.error("세션 무효화 중 예외 발생", ex);
                }
                oldSession = null;
            }
        }

        HttpSession session = (oldSession != null) ? oldSession : request.getSession(true);

        SecurityContextHolder.setContext(securityContext);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);

        session.setMaxInactiveInterval(3600);
    }
}