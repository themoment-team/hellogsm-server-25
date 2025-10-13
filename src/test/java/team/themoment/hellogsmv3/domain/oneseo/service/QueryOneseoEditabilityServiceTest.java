package team.themoment.hellogsmv3.domain.oneseo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import team.themoment.hellogsmv3.domain.oneseo.dto.response.OneseoEditabilityResDto;
import team.themoment.hellogsmv3.domain.oneseo.repository.EntranceTestResultRepository;

@DisplayName("QueryOneseoEditabilityService 클래스의")
class QueryOneseoEditabilityServiceTest {

    @Mock
    private EntranceTestResultRepository entranceTestResultRepository;

    @InjectMocks
    private QueryOneseoEditabilityService queryOneseoEditabilityService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Nested
    @DisplayName("execute 메소드는")
    class Describe_execute {

        @Nested
        @DisplayName("1차 전형이 종료되지 않은 경우")
        class Context_when_first_test_not_finished {

            @BeforeEach
            void setUp() {
                given(entranceTestResultRepository.existsByFirstTestPassYnIsNotNull()).willReturn(false);
            }

            @Test
            @DisplayName("수정 가능 여부가 true인 OneseoEditabilityResDto를 반환한다")
            void it_returns_true_editability() {
                OneseoEditabilityResDto result = queryOneseoEditabilityService.execute();
                assertEquals(true, result.oneseoEditability());
            }
        }

        @Nested
        @DisplayName("1차 전형이 종료된 경우")
        class Context_when_first_test_finished {

            @BeforeEach
            void setUp() {
                given(entranceTestResultRepository.existsByFirstTestPassYnIsNotNull()).willReturn(true);
            }

            @Test
            @DisplayName("수정 가능 여부가 false인 OneseoEditabilityResDto를 반환한다")
            void it_returns_false_editability() {
                OneseoEditabilityResDto result = queryOneseoEditabilityService.execute();
                assertEquals(false, result.oneseoEditability());
            }
        }
    }
}
