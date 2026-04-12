package by.bsuir.springbootproject.exceptions;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;

@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class GlobalControllerExceptionHandler {

    private boolean isAjax(HttpServletRequest request) {
        if (request.getDispatcherType() == DispatcherType.ERROR) {
            return false;
        }
        return "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));
    }

    private ModelAndView errorPage(int statusCode, String title, String message, HttpServletRequest request) {
        ModelAndView mv = new ModelAndView("error/error-page");
        mv.addObject("statusCode", statusCode);
        mv.addObject("title", title);
        mv.addObject("message", message);
        mv.addObject("path", request.getRequestURI());
        return mv;
    }

    private ResponseEntity<Map<String, Object>> errorJson(int statusCode, String title, String message, HttpServletRequest request) {
        return ResponseEntity.status(statusCode)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "success", false,
                        "statusCode", statusCode,
                        "title", title,
                        "message", message,
                        "path", request.getRequestURI()
                ));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public Object handleNoResourceFound(NoResourceFoundException e, HttpServletRequest request) {
        log.warn("404 Static resource not found: path={}", request.getRequestURI());

        if (isAjax(request)) {
            return errorJson(
                    HttpStatus.NOT_FOUND.value(),
                    "Ресурс не найден",
                    "Запрашиваемый файл или страница недоступны.",
                    request
            );
        }

        return errorPage(
                HttpStatus.NOT_FOUND.value(),
                "Ресурс не найден",
                "Запрашиваемый файл или страница недоступны.",
                request
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public Object handleAccessDenied(AccessDeniedException e, HttpServletRequest request) {
        log.warn("403 Access denied: path={}, message={}", request.getRequestURI(), e.getMessage());

        if (isAjax(request)) {
            return errorJson(
                    HttpStatus.FORBIDDEN.value(),
                    "Доступ запрещён",
                    "У вас нет прав для просмотра этой страницы.",
                    request
            );
        }

        return errorPage(
                HttpStatus.FORBIDDEN.value(),
                "Доступ запрещён",
                "У вас нет прав для просмотра этой страницы.",
                request
        );
    }

    @ExceptionHandler(AuthorizationException.class)
    public Object handleAuthorizationException(AuthorizationException e, HttpServletRequest request) {
        log.warn("401 Authorization error: path={}, message={}", request.getRequestURI(), e.getMessage());

        if (isAjax(request)) {
            return errorJson(
                    HttpStatus.UNAUTHORIZED.value(),
                    "Требуется авторизация",
                    "Чтобы продолжить, войдите в аккаунт.",
                    request
            );
        }

        return errorPage(
                HttpStatus.UNAUTHORIZED.value(),
                "Требуется авторизация",
                "Чтобы продолжить, войдите в аккаунт.",
                request
        );
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public Object handleUserAlreadyExistsException(UserAlreadyExistsException e, HttpServletRequest request) {
        log.warn("409 User already exists: {}", e.getMessage());

        if (isAjax(request)) {
            return errorJson(
                    HttpStatus.CONFLICT.value(),
                    "Пользователь уже существует",
                    "Аккаунт с такими данными уже зарегистрирован.",
                    request
            );
        }

        return errorPage(
                HttpStatus.CONFLICT.value(),
                "Пользователь уже существует",
                "Аккаунт с такими данными уже зарегистрирован.",
                request
        );
    }

    @ExceptionHandler({
            InsufficientFundsException.class,
            NoProductsInOrderException.class,
            NoProductInInventoryException.class,
            IllegalArgumentException.class,
            IllegalStateException.class
    })
    public Object handleBusinessExceptions(Exception e, HttpServletRequest request) {
        log.warn("Business error on path={}: {}", request.getRequestURI(), e.getMessage());

        if (isAjax(request)) {
            return errorJson(
                    HttpStatus.BAD_REQUEST.value(),
                    "Операция не выполнена",
                    e.getMessage(),
                    request
            );
        }

        return errorPage(
                HttpStatus.BAD_REQUEST.value(),
                "Операция не выполнена",
                e.getMessage(),
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public Object handleException(Exception e, HttpServletRequest request) {
        log.error("Unhandled exception on path={}", request.getRequestURI(), e);

        if (isAjax(request)) {
            return errorJson(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Что-то пошло не так",
                    "Мы уже зафиксировали проблему. Попробуйте обновить страницу или вернуться позже.",
                    request
            );
        }

        return errorPage(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Что-то пошло не так",
                "Мы уже зафиксировали проблему. Попробуйте обновить страницу или вернуться позже.",
                request
        );
    }
}
