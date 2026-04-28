package by.bsuir.springbootproject.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorViewResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.ModelAndView;

@Configuration
public class MvcErrorConfig {

    private static final String XML_HTTP_REQUEST = "XMLHttpRequest";

    @Bean
    public ErrorViewResolver customErrorViewResolver() {
        return (request, status, model) -> {
            if (isJsonRequest(request)) {
                return null;
            }

            String title;
            String message;

            if (status == HttpStatus.NOT_FOUND) {
                title = "Страница не найдена";
                message = "Похоже, такой страницы не существует или ссылка устарела.";
            } else if (status == HttpStatus.FORBIDDEN) {
                title = "Доступ запрещён";
                message = "У вас нет прав для просмотра этой страницы.";
            } else if (status == HttpStatus.UNAUTHORIZED) {
                title = "Требуется авторизация";
                message = "Чтобы продолжить, войдите в аккаунт.";
            } else if (status == HttpStatus.BAD_REQUEST) {
                title = "Некорректный запрос";
                message = "Проверьте параметры в адресной строке или вернитесь на предыдущую страницу.";
            } else if (status == HttpStatus.METHOD_NOT_ALLOWED) {
                title = "Метод не поддерживается";
                message = "Для этой страницы нельзя выполнить такой тип запроса.";
            } else {
                title = "Что-то пошло не так";
                message = "Мы уже зафиксировали проблему. Попробуйте обновить страницу или вернуться позже.";
            }

            ModelAndView mv = new ModelAndView("error/error-page");
            mv.setStatus(status);
            mv.addObject("statusCode", status.value());
            mv.addObject("title", title);
            mv.addObject("message", message);
            mv.addObject("path", request.getRequestURI());
            return mv;
        };
    }

    private boolean isJsonRequest(HttpServletRequest request) {
        String requestedWith = request.getHeader("X-Requested-With");
        String accept = request.getHeader(HttpHeaders.ACCEPT);

        return XML_HTTP_REQUEST.equals(requestedWith)
                || (accept != null && accept.contains(MediaType.APPLICATION_JSON_VALUE));
    }
}