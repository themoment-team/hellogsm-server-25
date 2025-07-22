package team.themoment.hellogsmv3.global.security.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record OAuthTokenReqDto(
        @Schema(description = "OAuth Provider로부터 받은 OAuth Token")
        @NotBlank(message = "OAuth Token은 필수입니다.")
        String oauthToken
) {
}