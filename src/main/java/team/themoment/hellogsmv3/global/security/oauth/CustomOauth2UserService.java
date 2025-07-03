package team.themoment.hellogsmv3.global.security.oauth;

import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import team.themoment.hellogsmv3.domain.member.entity.Member;
import team.themoment.hellogsmv3.domain.member.entity.type.AuthReferrerType;
import team.themoment.hellogsmv3.domain.member.entity.type.Role;
import team.themoment.hellogsmv3.domain.member.repo.MemberRepository;

import java.time.LocalDateTime;
import java.util.*;

import static team.themoment.hellogsmv3.domain.member.entity.type.AuthReferrerType.*;

@Service
public class CustomOauth2UserService implements OAuth2UserService {

    private final OAuth2UserService<OAuth2UserRequest, OAuth2User> delegateOauth2UserService;
    private final MemberRepository memberRepository;

    public CustomOauth2UserService(MemberRepository memberRepository) {
        this.delegateOauth2UserService = new DefaultOAuth2UserService();
        this.memberRepository = memberRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = delegateOauth2UserService.loadUser(userRequest);
        Map<String, Object> oAuthAttributes = oAuth2User.getAttributes();

        final String provider = userRequest.getClientRegistration().getRegistrationId();
        String providerId;
        AuthReferrerType authRefType;

        if (provider == null) throw new AuthenticationServiceException("oauth provider가 존재하지 않습니다.");

        switch (provider.toLowerCase()) {
            case "kakao" -> {
                Map<String, Object> kakaoAccount = (Map<String, Object>) oAuthAttributes.get("kakao_account");
                providerId = Optional.ofNullable(kakaoAccount)
                        .map(account -> account.get("email"))
                        .map(Object::toString)
                        .orElse(oAuthAttributes.get("id").toString());

                authRefType = KAKAO;
            }
            case "google" -> {
                providerId = oAuthAttributes.get("email").toString();
                authRefType = GOOGLE;
            }
            default -> throw new IllegalArgumentException("올바르지 않은 oauth provider 입니다.");
        }

        Member member = getUser(providerId, authRefType);

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
        Collection<GrantedAuthority> authorities = new ArrayList<>(oAuth2User.getAuthorities());
        authorities.add(new SimpleGrantedAuthority(role.name()));

        return new UserInfo(authorities, attributes, nameAttribute);
    }

    private Member getUser(String providerId, AuthReferrerType authRefType) {
        return memberRepository.findByAuthReferrerTypeAndEmail(authRefType, providerId)
                .orElseGet(() -> memberRepository.save(Member.buildMemberWithOauthInfo(providerId, authRefType)));
    }
}
