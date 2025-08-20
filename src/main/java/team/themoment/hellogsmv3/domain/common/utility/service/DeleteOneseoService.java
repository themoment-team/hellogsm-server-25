package team.themoment.hellogsmv3.domain.common.utility.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.themoment.hellogsmv3.domain.oneseo.repository.OneseoRepository;
import team.themoment.hellogsmv3.domain.oneseo.service.OneseoService;
import team.themoment.hellogsmv3.global.exception.error.ExpectedException;

@Service
@RequiredArgsConstructor
public class DeleteOneseoService {

    private final OneseoRepository oneseoRepository;

    @Transactional(rollbackFor = Exception.class)
    public void execute(String submitCode) {
        Long memberId = oneseoRepository.findMemberIdByOneseoSubmitCode(submitCode)
                .orElseThrow(() -> new ExpectedException("해당 접수 번호에 해당하는 원서가 존재하지 않습니다.", HttpStatus.NOT_FOUND));
        if (oneseoRepository.deleteByOneseoSubmitCode(submitCode) == 0) {
            throw new ExpectedException("원서 삭제에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        evictOneseoCache(memberId);
    }

    // Redis 캐시에서 해당 memberId에 대한 원서 정보를 제거하기 위한 메서드입니다
    @CacheEvict(value = OneseoService.ONESEO_CACHE_VALUE, key = "#memberId")
    public void evictOneseoCache(Long memberId) {
    }
}