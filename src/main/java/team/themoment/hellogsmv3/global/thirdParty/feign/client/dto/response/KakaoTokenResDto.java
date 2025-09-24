package team.themoment.hellogsmv3.global.thirdParty.feign.client.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KakaoTokenResDto(
    @JsonProperty("access_token") String accessToken,
    @JsonProperty("token_type") String tokenType,
    @JsonProperty("refresh_token") String refreshToken,
    @JsonProperty("expires_in") Integer expiresIn,
    @JsonProperty("scope") String scope,
    @JsonProperty("refresh_token_expires_in") Integer refreshTokenExpiresIn) {}
