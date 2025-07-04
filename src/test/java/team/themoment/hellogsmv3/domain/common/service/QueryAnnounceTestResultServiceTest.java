package team.themoment.hellogsmv3.domain.common.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import team.themoment.hellogsmv3.domain.common.operation.dto.response.AnnounceTestResultResDto;
import team.themoment.hellogsmv3.domain.common.operation.entity.OperationTestResult;
import team.themoment.hellogsmv3.domain.common.operation.repo.OperationTestResultRepository;
import team.themoment.hellogsmv3.domain.common.operation.service.QueryAnnounceTestResultService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static team.themoment.hellogsmv3.domain.oneseo.entity.type.YesNo.*;

@DisplayName("QueryAnnounceTestResultService 클래스의")
public class QueryAnnounceTestResultServiceTest {

    @Mock
    private OperationTestResultRepository operationTestResultRepository;

    @InjectMocks
    private QueryAnnounceTestResultService queryAnnounceTestResultService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Nested
    @DisplayName("execute 메소드는")
    class Describe_execute {

        OperationTestResult testResult;

        @BeforeEach
        void setup() {
            testResult = OperationTestResult.builder()
                    .firstTestResultAnnouncementYn(NO)
                    .secondTestResultAnnouncementYn(NO)
                    .build();

            given(operationTestResultRepository.findTestResult()).willReturn(testResult);
        }

        @Test
        @DisplayName("1차 테스트 결과 발표 여부와 2차 테스트 결과 발표 여부를 반환한다")
        void it_returns_first_and_second_test_result_announcement_yn() {
            AnnounceTestResultResDto result = queryAnnounceTestResultService.execute();

            assertEquals(testResult.getFirstTestResultAnnouncementYn(), result.getFirstTestResultAnnouncementYn());
            assertEquals(testResult.getSecondTestResultAnnouncementYn(), result.getSecondTestResultAnnouncementYn());
        }
    }
}
