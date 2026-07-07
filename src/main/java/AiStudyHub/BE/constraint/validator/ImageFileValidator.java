package AiStudyHub.BE.constraint.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;


public class ImageFileValidator implements ConstraintValidator<ValidImageFile, MultipartFile> {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".webp", ".gif", ".avif",
            ".heic", ".heif", ".bmp", ".tiff", ".tif", ".svg",
            ".ico", ".jfif", ".pjpeg", ".pjp"
    );

    @Override
    public void initialize(ValidImageFile constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(MultipartFile file, ConstraintValidatorContext context) {
        if (file == null || file.isEmpty()) {
            return true; // Let presence annotations (e.g. @NotNull) handle empty files
        }

        String contentType = file.getContentType();
        if (contentType != null && contentType.toLowerCase().startsWith("image/")) {
            return true;
        }

        String filename = file.getOriginalFilename();
        if (filename != null) {
            String lowerFilename = filename.toLowerCase();
            for (String ext : ALLOWED_EXTENSIONS) {
                if (lowerFilename.endsWith(ext)) {
                    return true;
                }
            }
        }

        return false;
    }
}
