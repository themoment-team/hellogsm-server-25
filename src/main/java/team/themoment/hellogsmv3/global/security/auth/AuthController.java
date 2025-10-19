package team.themoment.hellogsmv3.global.security.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import team.themoment.hellogsmv3.global.common.response.CommonApiResponse;
import team.themoment.hellogsmv3.global.exception.error.ExpectedException;
import team.themoment.hellogsmv3.global.security.auth.dto.request.OAuthCodeReqDto;
import team.themoment.hellogsmv3.global.security.auth.service.OAuthAuthenticationService;

import java.io.IOException;

@Tag(name = "Auth API", description = "인증 관련 API입니다.")
@RestController
@RequestMapping("/auth/v3")
@RequiredArgsConstructor
public class AuthController {

    private final OAuthAuthenticationService oAuthAuthenticationService;
    private final ObjectMapper objectMapper;

    @Operation(summary = "OAuth 인증", description = "프론트엔드에서 받은 Authorization Code로 인증을 처리합니다.")
    @PostMapping("/auth/{provider}")
    public CommonApiResponse authenticateWithOAuth(@PathVariable String provider,
            @RequestBody @Valid OAuthCodeReqDto reqDto, HttpServletRequest request) {
        oAuthAuthenticationService.execute(provider, reqDto.code(), request);
        return CommonApiResponse.success("인증이 완료되었습니다.");
    }

    @Operation(summary = "로그아웃", description = "로그아웃을 진행합니다.")
    @GetMapping("/logout")
    public void logout(HttpServletRequest req, HttpServletResponse res) throws IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof OAuth2AuthenticationToken)) {
            throw new ExpectedException("인증 정보가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED);
        }
        CommonApiResponse response = CommonApiResponse.success("로그아웃 되었습니다.");
        res.setStatus(HttpServletResponse.SC_OK);
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        res.getWriter().write(objectMapper.writeValueAsString(response));
        res.flushBuffer();
        new SecurityContextLogoutHandler().logout(req, res, auth);
    }
}
