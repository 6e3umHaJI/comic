package by.bsuir.springbootproject.exceptions;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.Map;

@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class GlobalControllerExceptionHandler {

    private static final String XML_HTTP_REQUEST = "XMLHttpRequest";

    private boolean isAjax(HttpServletRequest request) {
        if (request.getDispatcherType() == DispatcherType.ERROR) {
            return false;
        }

        String requestedWith = request.getHeader("X-Requested-With");
        String accept = request.getHeader(HttpHeaders.ACCEPT);

        return XML_HTTP_REQUEST.equals(requestedWith)
                || (accept != null && accept.contains(MediaType.APPLICATION_JSON_VALUE));
    }

    private ModelAndView errorPage(HttpStatus status, String title, String message, HttpServletRequest request) {
        ModelAndView mv = new ModelAndView("error/error-page");
        mv.setStatus(status);
        mv.addObject("statusCode", status.value());
        mv.addObject("title", title);
        mv.addObject("message", message);
        mv.addObject("path", request.getRequestURI());
        return mv;
    }

    private ResponseEntity<Map<String, Object>> errorJson(
            HttpStatus status,
            String title,
            String message,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "success", false,
                        "statusCode", status.value(),
                        "title", title,
                        "message", message,
                        "path", request.getRequestURI()
                ));
    }

    private Object errorResponse(
            HttpStatus status,
            String title,
            String message,
            HttpServletRequest request
    ) {
        if (isAjax(request)) {
            return errorJson(status, title, message, request);
        }

        return errorPage(status, title, message, request);
    }

    @ExceptionHandler({
            org.springframework.web.servlet.resource.NoResourceFoundException.class,
            NoHandlerFoundException.class
    })
    public Object handleNotFound(Exception e, HttpServletRequest request) {
        log.warn("404 Not found: path={}, message={}", request.getRequestURI(), e.getMessage());

        return errorResponse(
                HttpStatus.NOT_FOUND,
                "Страница не найдена",
                "Похоже, такой страницы не существует или ссылка устарела.",
                request
        );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Object handleMethodNotSupported(HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        log.warn(
                "405 Method not supported: path={}, method={}, message={}",
                request.getRequestURI(),
                request.getMethod(),
                e.getMessage()
        );

        return errorResponse(
                HttpStatus.METHOD_NOT_ALLOWED,
                "Метод не поддерживается",
                "Для этой страницы нельзя выполнить такой тип запроса.",
                request
        );
    }

    @ExceptionHandler({
            MethodArgumentTypeMismatchException.class,
            TypeMismatchException.class,
            MissingServletRequestParameterException.class,
            ServletRequestBindingException.class,
            BindException.class,
            HttpMessageNotReadableException.class
    })
    public Object handleBadRequest(Exception e, HttpServletRequest request) {
        log.warn("400 Bad request: path={}, message={}", request.getRequestURI(), e.getMessage());

        return errorResponse(
                HttpStatus.BAD_REQUEST,
                "Некорректный запрос",
                "Проверьте параметры в адресной строке или вернитесь на предыдущую страницу.",
                request
        );
    }

    @ExceptionHandler({
            MultipartException.class,
            MaxUploadSizeExceededException.class
    })
    public Object handleMultipart(Exception e, HttpServletRequest request) {
        log.warn("400 Upload error: path={}, message={}", request.getRequestURI(), e.getMessage());

        return errorResponse(
                HttpStatus.BAD_REQUEST,
                "Файл не загружен",
                "Проверьте формат и размер файла.",
                request
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public Object handleAccessDenied(AccessDeniedException e, HttpServletRequest request) {
        log.warn("403 Access denied: path={}, message={}", request.getRequestURI(), e.getMessage());

        return errorResponse(
                HttpStatus.FORBIDDEN,
                "Доступ запрещён",
                "У вас нет прав для просмотра этой страницы.",
                request
        );
    }

    @ExceptionHandler(AuthorizationException.class)
    public Object handleAuthorizationException(AuthorizationException e, HttpServletRequest request) {
        log.warn("401 Authorization error: path={}, message={}", request.getRequestURI(), e.getMessage());

        return errorResponse(
                HttpStatus.UNAUTHORIZED,
                "Требуется авторизация",
                "Чтобы продолжить, войдите в аккаунт.",
                request
        );
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public Object handleUserAlreadyExistsException(UserAlreadyExistsException e, HttpServletRequest request) {
        log.warn("409 User already exists: path={}, message={}", request.getRequestURI(), e.getMessage());

        return errorResponse(
                HttpStatus.CONFLICT,
                "Пользователь уже существует",
                "Аккаунт с такими данными уже зарегистрирован.",
                request
        );
    }

    @ExceptionHandler(ResponseStatusException.class)
    public Object handleResponseStatusException(ResponseStatusException e, HttpServletRequest request) {
        HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        log.warn(
                "{} ResponseStatusException: path={}, message={}",
                status.value(),
                request.getRequestURI(),
                e.getReason()
        );

        return errorResponse(
                status,
                titleForStatus(status),
                messageForStatus(status),
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
        log.warn("400 Business error: path={}, message={}", request.getRequestURI(), e.getMessage());

        return errorResponse(
                HttpStatus.BAD_REQUEST,
                "Операция не выполнена",
                safeMessage(e.getMessage(), "Запрос не удалось обработать."),
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public Object handleException(Exception e, HttpServletRequest request) {
        log.error("500 Unhandled exception: path={}", request.getRequestURI(), e);

        return errorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Что-то пошло не так",
                "Попробуйте обновить страницу или вернуться позже.",
                request
        );
    }

    private String safeMessage(String message, String fallback) {
        return message == null || message.isBlank() ? fallback : message;
    }

    private String titleForStatus(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> "Страница не найдена";
            case FORBIDDEN -> "Доступ запрещён";
            case UNAUTHORIZED -> "Требуется авторизация";
            case BAD_REQUEST -> "Некорректный запрос";
            case METHOD_NOT_ALLOWED -> "Метод не поддерживается";
            default -> status.is5xxServerError() ? "Что-то пошло не так" : "Операция не выполнена";
        };
    }

    private String messageForStatus(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> "Похоже, такой страницы не существует или ссылка устарела.";
            case FORBIDDEN -> "У вас нет прав для просмотра этой страницы.";
            case UNAUTHORIZED -> "Чтобы продолжить, войдите в аккаунт.";
            case BAD_REQUEST -> "Проверьте параметры в адресной строке или вернитесь на предыдущую страницу.";
            case METHOD_NOT_ALLOWED -> "Для этой страницы нельзя выполнить такой тип запроса.";
            default -> status.is5xxServerError()
                    ? "Попробуйте обновить страницу или вернуться позже."
                    : "Запрос не удалось обработать.";
        };
    }
}
