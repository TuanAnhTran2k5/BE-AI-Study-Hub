package AiStudyHub.BE.constraint.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Documented
@Constraint(validatedBy = MaxFileSizeValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface MaxFileSize {
    String message() default "FILE_TOO_LARGE";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    long maxBytes() default 20 * 1024 * 1024; // Default to 20MB
}
