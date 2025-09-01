package team.themoment.hellogsmv3.domain.oneseo.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record SearchOneseosResDto(
        SearchOneseoPageInfoDto info,
        List<SearchOneseoResDto> oneseos
) {
}
