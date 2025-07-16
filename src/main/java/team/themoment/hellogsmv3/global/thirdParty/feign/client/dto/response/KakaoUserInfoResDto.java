package team.themoment.hellogsmv3.global.thirdParty.feign.client.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Kakao User Info Response DTO
 */
public record KakaoUserInfoResDto(
    @JsonProperty("id")
    Long id,
    
    @JsonProperty("connected_at")
    String connectedAt,
    
    @JsonProperty("kakao_account")
    KakaoAccount kakaoAccount
) {
    
    /**
     * Kakao Account 정보
     */
    public record KakaoAccount(
        @JsonProperty("profile_nickname_needs_agreement")
        Boolean profileNicknameNeedsAgreement,
        
        @JsonProperty("profile_image_needs_agreement")
        Boolean profileImageNeedsAgreement,
        
        @JsonProperty("profile")
        Profile profile,
        
        @JsonProperty("name_needs_agreement")
        Boolean nameNeedsAgreement,
        
        @JsonProperty("name")
        String name,
        
        @JsonProperty("email_needs_agreement")
        Boolean emailNeedsAgreement,
        
        @JsonProperty("is_email_valid")
        Boolean isEmailValid,
        
        @JsonProperty("is_email_verified")
        Boolean isEmailVerified,
        
        @JsonProperty("email")
        String email
    ) {
    }
    
    /**
     * Kakao Profile 정보
     */
    public record Profile(
        @JsonProperty("nickname")
        String nickname,
        
        @JsonProperty("thumbnail_image_url")
        String thumbnailImageUrl,
        
        @JsonProperty("profile_image_url")
        String profileImageUrl,
        
        @JsonProperty("is_default_image")
        Boolean isDefaultImage
    ) {
    }
    
    /**
     * 이메일 추출 헬퍼 메서드
     */
    public String getEmail() {
        return kakaoAccount != null ? kakaoAccount.email : null;
    }
    
    /**
     * 닉네임 추출 헬퍼 메서드
     */
    public String getNickname() {
        return kakaoAccount != null && kakaoAccount.profile != null 
            ? kakaoAccount.profile.nickname 
            : null;
    }
}
