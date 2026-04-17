package by.bsuir.springbootproject.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorViewResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.ModelAndView;

@Configuration
public class MvcErrorConfig {

    @Bean
    public ErrorViewResolver customErrorViewResolver() {
        return (request, status, model) -> {
            HttpServletRequest httpRequest = request;

            if ("XMLHttpRequest".equals(httpRequest.getHeader("X-Requested-With"))) {
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
            } else {
                title = "Что-то пошло не так";
                message = "Мы уже зафиксировали проблему. Попробуйте обновить страницу или вернуться позже.";
            }

            ModelAndView mv = new ModelAndView("error/error-page");
            mv.addObject("statusCode", status.value());
            mv.addObject("title", title);
            mv.addObject("message", message);
            mv.addObject("path", httpRequest.getRequestURI());
            return mv;
        };
    }
}
