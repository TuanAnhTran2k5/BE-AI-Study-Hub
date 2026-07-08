package AiStudyHub.BE.constraint.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;


public class FileTypeValidator implements ConstraintValidator<ValidFileType, MultipartFile> {

    private List<String> allowedTypes;

    @Override
    public void initialize(ValidFileType constraintAnnotation) {
        this.allowedTypes = Arrays.asList(constraintAnnotation.allowedTypes());
    }

    @Override
    public boolean isValid(MultipartFile file, ConstraintValidatorContext context) {
        if (file == null || file.isEmpty()) {
            return true; // Let validation on presence (e.g., @NotNull) handle empty files
        }
        String contentType = file.getContentType();
        return contentType != null && allowedTypes.contains(contentType);
    }
}
