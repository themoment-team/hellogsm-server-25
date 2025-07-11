package team.themoment.hellogsmv3.global.security.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record OAuthCodeReqDto(
    @Schema(description = "OAuth Provider로부터 받은 Authorization Code", example = "4/0AX4XfWh...")
    @NotBlank(message = "Authorization Code는 필수입니다.")
    String code
) {
}
