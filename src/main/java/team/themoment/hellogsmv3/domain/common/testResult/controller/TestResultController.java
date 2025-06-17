package team.themoment.hellogsmv3.domain.common.testResult.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import team.themoment.hellogsmv3.domain.common.testResult.dto.request.FoundTestResultReqDto;
import team.themoment.hellogsmv3.domain.common.testResult.dto.response.FoundTestResultResDto;
import team.themoment.hellogsmv3.domain.common.testResult.service.QueryTestResultService;
import team.themoment.hellogsmv3.domain.common.testResult.service.impl.GenerateTestResultCodeServiceImpl;
import team.themoment.hellogsmv3.domain.member.dto.request.AuthenticateCodeReqDto;
import team.themoment.hellogsmv3.domain.member.dto.request.GenerateCodeReqDto;
import team.themoment.hellogsmv3.domain.member.service.AuthenticateCodeService;
import team.themoment.hellogsmv3.global.common.handler.annotation.AuthRequest;
import team.themoment.hellogsmv3.global.common.response.CommonApiResponse;

import static team.themoment.hellogsmv3.domain.common.testResult.type.TestType.*;
import static team.themoment.hellogsmv3.domain.member.entity.type.AuthCodeType.TEST_RESULT;

@RestController
@RequestMapping("/test-result/v3")
@RequiredArgsConstructor
public class TestResultController {

    private final QueryTestResultService queryTestResultService;
    private final AuthenticateCodeService authenticateCodeService;
    private final GenerateTestResultCodeServiceImpl generateTestResultCodeService;

    @Operation(summary = "인증코드 전송", description = "전화번호를 요청받아 인증코드를 전송합니다.")
    @PostMapping("/send-code")
    public CommonApiResponse sendCode(
            @AuthRequest Long memberId,
            @RequestBody @Valid GenerateCodeReqDto reqDto
    ) {
        generateTestResultCodeService.execute(memberId, reqDto);
        return CommonApiResponse.success("전송되었습니다.");
    }

    @Operation(summary = "인증코드 인증", description = "인증코드를 요청받아 인증을 진행합니다.")
    @PostMapping("/auth-code")
    public CommonApiResponse authCode(
            @AuthRequest Long memberId,
            @RequestBody @Valid AuthenticateCodeReqDto reqDto
    ) {
        authenticateCodeService.execute(memberId, reqDto, TEST_RESULT);
        return CommonApiResponse.success("인증되었습니다.");
    }

    @Operation(summary = "1차 전형 결과 조회", description = "학부모 or 부모님의 전화번호와 접수번호의 조합으로 1차 전형의 결과를 조회합니다.")
    @GetMapping("/first-test")
    public FoundTestResultResDto foundFirstTestResult(
            @AuthRequest Long memberId,
            @RequestParam("phoneNumber") @NotNull String phoneNumber,
            @RequestParam("code") @NotNull String code,
            @RequestParam("submitCode") @NotNull String submitCode
    ) {
        return queryTestResultService.execute(memberId, code, phoneNumber, submitCode, FIRST);
    }

    @Operation(summary = "2차 전형 결과 조회", description = "학부모 or 부모님의 전화번호와 수험번호의 조합으로 2차 전형의 결과를 조회합니다.")
    @GetMapping("/second-test")
    public FoundTestResultResDto foundSecondTestResult(
            @AuthRequest Long memberId,
            @RequestParam("phoneNumber") @NotNull String phoneNumber,
            @RequestParam("code") @NotNull String code,
            @RequestParam("examinationNumber") @NotNull String examinationNumber
    ) {
        return queryTestResultService.execute(memberId, code, phoneNumber, examinationNumber, SECOND);
    }

    @Operation(summary = "1차 전형 합불 여부 공개 조회", description = "1차 전형 합불 여부를 이름과 생년월일로 공개 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "1차 전형 합격자 조회 성공"),
            @ApiResponse(responseCode = "204", description = "1차 전형 결과가 아직 발표되지 않았습니다.", content = @Content())
    })
    @GetMapping("/public/first-test-pass")
    public FoundTestResultResDto firstTestResultByNameAndBirth(
            @RequestParam("name") @NotNull String name,
            @RequestParam("birth") @NotNull LocalDate birth
    ) {
        return queryFirstTestResultByNameAndBirthService.execute(name, birth);
    }
