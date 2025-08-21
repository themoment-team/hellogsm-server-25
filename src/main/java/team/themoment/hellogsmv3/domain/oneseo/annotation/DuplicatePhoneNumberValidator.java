package team.themoment.hellogsmv3.domain.oneseo.annotation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import team.themoment.hellogsmv3.domain.oneseo.dto.request.OneseoReqDto;

public class DuplicatePhoneNumberValidator implements ConstraintValidator<ValidDuplicatePhoneNumber, OneseoReqDto> {
    private ValidDuplicatePhoneNumber annotation;

    @Override
    public void initialize(ValidDuplicatePhoneNumber constraintAnnotation) {
        this.annotation = constraintAnnotation;
    }

    @Override
    public boolean isValid(OneseoReqDto dto, ConstraintValidatorContext context) {
        if (dto.guardianPhoneNumber().equals(dto.schoolTeacherPhoneNumber())) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(annotation.message()).addConstraintViolation();
            return false;
        }
        return true;
    }
}
