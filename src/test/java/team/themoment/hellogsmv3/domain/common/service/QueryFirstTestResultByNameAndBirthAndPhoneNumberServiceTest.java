package team.themoment.hellogsmv3.domain.common.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import team.themoment.hellogsmv3.domain.common.testResult.dto.response.FoundTestResultResDto;
import team.themoment.hellogsmv3.domain.common.testResult.service.QueryFirstTestResultByNameAndBirthAndPhoneNumberService;
import team.themoment.hellogsmv3.domain.member.entity.Member;
import team.themoment.hellogsmv3.domain.oneseo.entity.EntranceTestResult;
import team.themoment.hellogsmv3.domain.oneseo.entity.Oneseo;
import team.themoment.hellogsmv3.domain.oneseo.repository.OneseoRepository;
import team.themoment.hellogsmv3.domain.oneseo.service.OneseoService;
import team.themoment.hellogsmv3.global.exception.error.ExpectedException;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static team.themoment.hellogsmv3.domain.oneseo.entity.type.YesNo.*;

@DisplayName("QueryFirstTestResultByNameAndBirthAndPhoneNumberService 클래스의")
public class QueryFirstTestResultByNameAndBirthAndPhoneNumberServiceTest {

    @Mock
    private OneseoService oneseoService;

    @Mock
    private OneseoRepository oneseoRepository;

    @InjectMocks
    private QueryFirstTestResultByNameAndBirthAndPhoneNumberService queryFirstTestResultByNameAndBirthAndPhoneNumberService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Nested
    @DisplayName("execute 메소드는")
    class Describe_execute {

        String name = "홍길동";
        String phoneNumber = "01012345678";
        String birth = "2010-01-01";

        Member member = Member.builder()
                .id(1L)
                .name(name)
                .phoneNumber(phoneNumber)
                .birth(LocalDate.parse(birth))
                .build();

        EntranceTestResult testResult = EntranceTestResult.builder()
                .firstTestPassYn(YES)
                .build();

        Oneseo oneseo = Oneseo.builder()
                .member(member)
                .entranceTestResult(testResult)
                .build();

        FoundTestResultResDto result = FoundTestResultResDto.builder()
                .name("홍*동")
                .firstTestPassYn(oneseo.getEntranceTestResult().getFirstTestPassYn())
                .build();

        @Nested
        @DisplayName("1차 전형 결과 발표 전이라면")
        class Context_with_before_first_test_result_announcement {

            @BeforeEach
            void setUp() {
                given(oneseoService.validateFirstTestResultAnnouncement()).willReturn(true);
            }

            @Test
            @DisplayName("예외를 던진다")
            void it_throws_exception() {
                ExpectedException exception = assertThrows(ExpectedException.class, () ->
                        queryFirstTestResultByNameAndBirthAndPhoneNumberService.execute(name, phoneNumber, birth));

                assertEquals("1차 전형 결과 발표 전 입니다.", exception.getMessage());
            }
        }

        @Nested
        @DisplayName("생년월일 형식이 잘못되었다면")
        class Context_with_invalid_birth_format {

            @BeforeEach
            void setUp() {
                given(oneseoService.validateFirstTestResultAnnouncement()).willReturn(false);
            }

            @Test
            @DisplayName("예외를 던진다")
            void it_throws_exception() {
                ExpectedException exception = assertThrows(ExpectedException.class, () ->
                        queryFirstTestResultByNameAndBirthAndPhoneNumberService.execute(name, phoneNumber, "2010-01"));

                assertEquals("생년월일 형식이 잘못되었습니다. 올바른 형식은 'YYYY-MM-DD'입니다.", exception.getMessage());
            }
        }

        @Nested
        @DisplayName("주어진 정보를 가진 원서를 찾을 수 없다면")
        class Context_with_no_matching_application {

            @BeforeEach
            void setUp() {
                given(oneseoService.validateFirstTestResultAnnouncement()).willReturn(false);
                given(oneseoRepository.findByMemberNameAndMemberBirthAndPhoneNumber(name, phoneNumber, LocalDate.parse(birth))).willReturn(Optional.empty());
            }

            @Test
            @DisplayName("예외를 던진다")
            void it_throws_exception() {
                ExpectedException exception = assertThrows(ExpectedException.class, () ->
                        queryFirstTestResultByNameAndBirthAndPhoneNumberService.execute(name, phoneNumber, birth));

                assertEquals("해당 이름, 전화번호, 생년월일의 정보를 가진 원서를 찾을 수 없습니다.", exception.getMessage());
            }
        }

        @Nested
        @DisplayName("주어진 정보로 원서를 찾을 수 있다면")
        class Context_with_matching_application {

            @BeforeEach
            void setUp() {
                given(oneseoService.validateFirstTestResultAnnouncement()).willReturn(false);
                given(oneseoRepository.findByMemberNameAndMemberBirthAndPhoneNumber(name, phoneNumber, LocalDate.parse(birth))).willReturn(Optional.of(oneseo));
            }

            @Test
            @DisplayName("1차 전형 결과를 반환한다.")
            void it_returns_first_test_result() {
                FoundTestResultResDto response = queryFirstTestResultByNameAndBirthAndPhoneNumberService.execute(name, phoneNumber, birth);

                assertEquals(result.name(), response.name());
                assertEquals(result.firstTestPassYn(), response.firstTestPassYn());
            }
        }
    }
}
