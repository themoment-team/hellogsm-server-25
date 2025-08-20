package team.themoment.hellogsmv3.domain.common.utility.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import team.themoment.hellogsmv3.global.common.response.CommonApiResponse;

@RestController
@RequestMapping
@Tag(name = "Utility API", description = "개발용 유틸리티 API입니다.")
@RequiredArgsConstructor
public class UtilityController {


    @Operation(summary = "원서 삭제", description = "입력된 접수 번호에 해당하는 원서를 삭제합니다.")
    public CommonApiResponse deleteOneseo(@RequestParam String submitCode) {
        return CommonApiResponse.success(
                "입력된 접수 번호에 해당하는 원서를 삭제했습니다. 접수 번호: " + submitCode
        );
    }
}
