package team.themoment.hellogsmv3.domain.common.operation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.themoment.hellogsmv3.domain.common.operation.dto.response.AnnounceTestResultResDto;
import team.themoment.hellogsmv3.domain.common.operation.entity.OperationTestResult;
import team.themoment.hellogsmv3.domain.common.operation.repo.OperationTestResultRepository;

@Service
@RequiredArgsConstructor
public class QueryAnnounceTestResultService {

    private final OperationTestResultRepository operationTestResultRepository;

    @Transactional(readOnly = true)
    public AnnounceTestResultResDto execute() {
        OperationTestResult testResult = operationTestResultRepository.findTestResult();

        return AnnounceTestResultResDto.builder()
                .firstTestResultAnnouncementYn(testResult.getFirstTestResultAnnouncementYn())
                .secondTestResultAnnouncementYn(testResult.getSecondTestResultAnnouncementYn())
                .build();
    }
}
