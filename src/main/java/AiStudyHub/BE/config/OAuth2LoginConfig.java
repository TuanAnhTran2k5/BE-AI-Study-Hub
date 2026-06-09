package AiStudyHub.BE.config;

import AiStudyHub.BE.constraint.AuthProvider;
import AiStudyHub.BE.constraint.UserRole;
import AiStudyHub.BE.constraint.UserStatus;
import AiStudyHub.BE.entity.User;
import AiStudyHub.BE.repository.UserRepo;
import AiStudyHub.BE.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2LoginConfig implements AuthenticationSuccessHandler {

    @Autowired
    UserRepo userRepo;
    @Autowired
    TokenService tokenService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();

        String email = oauthUser.getAttribute("email");
        String fullName = oauthUser.getAttribute("name");
        String avatarUrl = oauthUser.getAttribute("picture");
        String googleId = oauthUser.getAttribute("sub");

        User user = userRepo.findByEmail(email)
                .map(existingUser -> {
                    existingUser.setGoogleId(googleId);

                    if (existingUser.getStatus() == UserStatus.PENDING) {
                        existingUser.setStatus(UserStatus.ACTIVE);
                    }

                    if (existingUser.getAvatarUrl() == null || existingUser.getAvatarUrl().isBlank()) {
                        existingUser.setAvatarUrl(avatarUrl);
                    }

                    return userRepo.save(existingUser);
                })
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .email(email)
                            .fullName(fullName)
                            .avatarUrl(avatarUrl)
                            .googleId(googleId)
                            .authProvider(AuthProvider.GOOGLE)
                            .role(UserRole.US)
                            .status(UserStatus.ACTIVE)
                            .build();

                    return userRepo.save(newUser);
                });

        String accessToken = tokenService.generateAccessToken(user);

        // String frontendUrl = "http://localhost:5173/oauth2/success?token=" + accessToken;
        //
        // response.sendRedirect(frontendUrl);

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        response.getWriter().write("""
        {
          "message": "Login Google successfully",
          "accessToken": "%s",
          "email": "%s",
          "fullName": "%s",
          "role": "%s",
          "status": "%s"
        }
        """.formatted(
                accessToken,
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getStatus()
        ));
    }

}
