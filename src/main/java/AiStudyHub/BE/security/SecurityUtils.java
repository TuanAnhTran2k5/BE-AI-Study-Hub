package AiStudyHub.BE.security;

import AiStudyHub.BE.constraint.ErrorCode;
import AiStudyHub.BE.entity.User;
import AiStudyHub.BE.exception.GlobalException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new GlobalException(ErrorCode.UNAUTHENTICATED);
        }

        if (authentication.getPrincipal() instanceof User user) {
            return user;
        }

        throw new GlobalException(ErrorCode.UNAUTHENTICATED);
    }
}
