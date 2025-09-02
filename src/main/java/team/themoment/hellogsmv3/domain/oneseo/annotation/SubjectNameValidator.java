package team.themoment.hellogsmv3.domain.oneseo.annotation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.http.HttpStatus;
import team.themoment.hellogsmv3.domain.oneseo.dto.request.MiddleSchoolAchievementReqDto;
import team.themoment.hellogsmv3.global.exception.error.ExpectedException;

import java.util.Collections;

public class SubjectNameValidator implements ConstraintValidator<ValidSubjectName, MiddleSchoolAchievementReqDto> {

    private ValidSubjectName annotation;

    @Override
    public void initialize(ValidSubjectName constraintAnnotation) {
        this.annotation = constraintAnnotation;
    }

    @Override
    public boolean isValid(MiddleSchoolAchievementReqDto middleSchoolAchievementReqDto, ConstraintValidatorContext context) {
        if(middleSchoolAchievementReqDto == null ||
                middleSchoolAchievementReqDto.generalSubjects() == null ||
                middleSchoolAchievementReqDto.artsPhysicalSubjects() == null
        ){
            throw new ExpectedException("middleSchoolAchievementReqDto 입력이 잘못되었습니다.", HttpStatus.BAD_REQUEST);
        }
        if(middleSchoolAchievementReqDto.newSubjects() == null){
            return true;
        }

        boolean isValid = Collections.disjoint(
                middleSchoolAchievementReqDto.generalSubjects(),
                middleSchoolAchievementReqDto.newSubjects()
        ) && Collections.disjoint(
                middleSchoolAchievementReqDto.artsPhysicalSubjects(),
                middleSchoolAchievementReqDto.newSubjects()
        ) && Collections.disjoint(
                middleSchoolAchievementReqDto.artsPhysicalSubjects(),
                middleSchoolAchievementReqDto.generalSubjects()
        );

        if(!isValid){
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(annotation.message()).addConstraintViolation();
            return false;
        }
        return true;
    }
}
