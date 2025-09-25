package team.themoment.hellogsmv3.domain.oneseo.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.OneseoEditabilityResDto;
import team.themoment.hellogsmv3.domain.oneseo.repository.EntranceTestResultRepository;

@Service
@RequiredArgsConstructor
public class QueryOneseoEditabilityService {

    private final EntranceTestResultRepository entranceTestResultRepository;

    public OneseoEditabilityResDto execute() {
        boolean isFirstTestFinished = entranceTestResultRepository.existsByFirstTestPassYnIsNotNull();
        return new OneseoEditabilityResDto(!isFirstTestFinished);
    }
}
