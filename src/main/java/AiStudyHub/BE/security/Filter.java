package AiStudyHub.BE.security;

import AiStudyHub.BE.config.SecurityProperties;
import AiStudyHub.BE.entity.User;
import AiStudyHub.BE.exception.GlobalException;
import AiStudyHub.BE.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.List;

@Component
@Slf4j
public class Filter extends OncePerRequestFilter {

    @Autowired
    SecurityProperties securityProperties;

    @Autowired
    TokenService tokenService;

    @Autowired
    @Qualifier("handlerExceptionResolver")
    HandlerExceptionResolver resolver;

    private final AntPathMatcher matcher = new AntPathMatcher();


    // check token
    private boolean isPublicEndpoint(String url) {
        List<String> publicEndpoint = securityProperties.getPublicEndpoints();

        if (publicEndpoint == null || publicEndpoint.isEmpty()) {
            return false;
        }

        return publicEndpoint.stream().anyMatch(pattern -> matcher.match(pattern, url));
    }

    // get token
    private String getToken(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            return null;
        }
        return token.substring(7);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        String token = getToken(request);

        if (isPublicEndpoint(path)) {
            filterChain.doFilter(request,response);
            return;
        }
        if (token != null) {
            try{
                User user = tokenService.verifyAccessToken(token);
                UsernamePasswordAuthenticationToken authentication
                        = new UsernamePasswordAuthenticationToken(user,null, user.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                // Authentication passed — SecurityConfig will no longer block with 403
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }catch (GlobalException ex){
                SecurityContextHolder.clearContext();
                resolver.resolveException(request, response,null, ex);
                return;
            }
        }

        filterChain.doFilter(request,response);
    }
}