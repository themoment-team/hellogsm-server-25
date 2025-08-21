package team.themoment.hellogsmv3.domain.oneseo.annotation;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import team.themoment.hellogsmv3.domain.oneseo.dto.request.MiddleSchoolAchievementReqDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.request.OneseoReqDto;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.GraduationType;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.Major;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.Screening;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static team.themoment.hellogsmv3.domain.oneseo.entity.type.GraduationType.CANDIDATE;
import static team.themoment.hellogsmv3.domain.oneseo.entity.type.Major.*;
import static team.themoment.hellogsmv3.domain.oneseo.entity.type.Screening.GENERAL;
import static org.mockito.Mockito.*;

@DisplayName("DuplicatePhoneNumberValidator 클래스의")
public class DuplicatePhoneNumberValidatorTest {

    @DisplayName("isValid 메소드는")
    @Nested
    class Describe_isValid {

        DuplicatePhoneNumberValidator validator;
        ConstraintValidatorContext context;
        OneseoReqDto oneseoReqDto;
        ValidDuplicatePhoneNumber annotation;

        @BeforeEach
        void setUp() {
            validator = new DuplicatePhoneNumberValidator();
            // 실제 어노테이션 인스턴스를 리플렉션으로 가져옴
            @ValidDuplicatePhoneNumber
            class Dummy {}
            annotation = Dummy.class.getAnnotation(ValidDuplicatePhoneNumber.class);
            validator.initialize(annotation);
            context = Mockito.mock(ConstraintValidatorContext.class);
            ConstraintValidatorContext.ConstraintViolationBuilder builder = Mockito.mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
            when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(builder);
            when(builder.addConstraintViolation()).thenReturn(context);
        }

        @DisplayName("보호자와 담임선생님의 전화번호가 중복되면")
        @Nested
        class Context_with_duplicate_phone_numbers {

            @BeforeEach
            void setUp() {
                oneseoReqDto = createFilledDtoBuilder()
                        .guardianPhoneNumber("01012345678")
                        .schoolTeacherPhoneNumber("01012345678")
                        .build();
            }
            @DisplayName("ConstraintViolation을 발생시킨다.")
            @Test
            void it_makes_constraint_violation(){
                boolean result = validator.isValid(oneseoReqDto, context);
                assertFalse(result);
                verify(context).buildConstraintViolationWithTemplate(annotation.message());
            }
        }

        @DisplayName("보호자와 담임선생님의 전화번호가 중복되지 않으면")
        @Nested
        class Context_with_different_phone_numbers {
            @BeforeEach
            void setUp() {
                oneseoReqDto = createFilledDtoBuilder()
                        .guardianPhoneNumber("01012345678")
                        .schoolTeacherPhoneNumber("01077777777")
                        .build();
            }
            @DisplayName("ConstraintViolation을 발생시키지 않는다.")
            @Test
            void it_doesnt_make_constraint_violation() {
                boolean result = validator.isValid(oneseoReqDto, context);
                assertTrue(result);
            }
        }
    }
    private OneseoReqDto.OneseoReqDtoBuilder createFilledDtoBuilder(){
        List<Integer> achievement = Arrays.asList(5, 5, 5, 5, 5, 5, 5, 5, 5);
        List<String> generalSubjects = Arrays.asList("국어", "도덕", "사회", "역사", "수학", "과학", "기술가정", "영어");
        List<String> newSubjects = Arrays.asList("프로그래밍");
        List<Integer> artsPhysicalAchievement = Arrays.asList(5, 5, 5, 5, 5, 5, 5, 5, 5);
        List<String> artsPhysicalSubjects = Arrays.asList("체육", "미술", "음악");
        List<Integer> absentDays = Arrays.asList(0, 0, 0);
        List<Integer> attendanceDays = Arrays.asList(0, 0, 0, 0, 0, 0, 0, 0, 0);
        List<Integer> volunteerTime = Arrays.asList(10, 0, 2);
        String liberalSystem = "자유학기제";
        String freeSemester = "1-1";

        MiddleSchoolAchievementReqDto middleSchoolAchievementReqDto = MiddleSchoolAchievementReqDto.builder()
                .achievement1_2(achievement)
                .achievement2_1(achievement)
                .achievement2_2(achievement)
                .achievement3_1(achievement)
                .generalSubjects(generalSubjects)
                .newSubjects(newSubjects)
                .artsPhysicalAchievement(artsPhysicalAchievement)
                .artsPhysicalSubjects(artsPhysicalSubjects)
                .absentDays(absentDays)
                .attendanceDays(attendanceDays)
                .volunteerTime(volunteerTime)
                .liberalSystem(liberalSystem)
                .freeSemester(freeSemester)
                .build();

        String guardianName = "김보호";
        String guardianPhoneNumber = "01000000001";
        String relationshipWithGuardian = "모";
        String profileImg = "https://abc";
        String address = "광주광역시 광산구 송정동 상무대로 312";
        String detailAddress = "101동 1404호";
        GraduationType graduationType = CANDIDATE;
        String schoolTeacherName = "김선생";
        String schoolTeacherPhoneNumber = "01000000002";
        Major firstDesiredMajor = SW;
        Major secondDesiredMajor = AI;
        Major thirdDesiredMajor = IOT;
        String schoolName = "금호중앙중학교";
        String schoolAddress = "광주 어딘가";
        Screening screening = GENERAL;
        String graduationDate = "2020-02";

        OneseoReqDto.OneseoReqDtoBuilder oneseoReqDtoBuilder = OneseoReqDto.builder()
                .guardianName(guardianName)
                .guardianPhoneNumber(guardianPhoneNumber)
                .relationshipWithGuardian(relationshipWithGuardian)
                .profileImg(profileImg)
                .address(address)
                .detailAddress(detailAddress)
                .graduationType(graduationType)
                .schoolTeacherName(schoolTeacherName)
                .schoolTeacherPhoneNumber(schoolTeacherPhoneNumber)
                .firstDesiredMajor(firstDesiredMajor)
                .secondDesiredMajor(secondDesiredMajor)
                .thirdDesiredMajor(thirdDesiredMajor)
                .middleSchoolAchievement(middleSchoolAchievementReqDto)
                .schoolName(schoolName)
                .schoolAddress(schoolAddress)
                .screening(screening)
                .graduationDate(graduationDate);
        return oneseoReqDtoBuilder;
    }
}
