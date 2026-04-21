package by.bsuir.springbootproject.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AuthLoginValidationFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/auth/login";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !LOGIN_PATH.equals(request.getServletPath())
                || !"POST".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String login = request.getParameter("login");
        String password = request.getParameter("password");

        if (!AuthInputValidationUtils.isValidLogin(login)) {
            redirectWithError(request, response, AuthInputValidationUtils.getLoginValidationMessage(login));
            return;
        }

        if (!AuthInputValidationUtils.isValidPassword(password)) {
            redirectWithError(request, response, AuthInputValidationUtils.getPasswordValidationMessage());
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void redirectWithError(HttpServletRequest request,
                                   HttpServletResponse response,
                                   String message) throws IOException {
        HttpSession session = request.getSession(true);
        session.setAttribute("authStatusMessage", message);
        response.sendRedirect(request.getContextPath() + LOGIN_PATH);
    }
}
