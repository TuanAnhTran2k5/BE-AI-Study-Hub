package AiStudyHub.BE.constraint.validator;

import AiStudyHub.BE.constraint.MaxFileSize;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;

/**
 * Validator implementation for {@link MaxFileSize}.
 * Ensures the uploaded file's size does not exceed the allowed threshold.
 */
public class MaxFileSizeValidator implements ConstraintValidator<MaxFileSize, MultipartFile> {

    private long maxBytes;

    @Override
    public void initialize(MaxFileSize constraintAnnotation) {
        this.maxBytes = constraintAnnotation.maxBytes();
    }

    @Override
    public boolean isValid(MultipartFile file, ConstraintValidatorContext context) {
        if (file == null || file.isEmpty()) {
            return true; // Let presence annotations handle empty files
        }
        return file.getSize() <= maxBytes;
    }
}
