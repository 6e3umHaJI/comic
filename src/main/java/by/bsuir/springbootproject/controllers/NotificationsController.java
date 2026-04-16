package by.bsuir.springbootproject.controllers;

import by.bsuir.springbootproject.constants.RoutePaths;
import by.bsuir.springbootproject.constants.ViewPaths;
import by.bsuir.springbootproject.dto.NotificationToggleResult;
import by.bsuir.springbootproject.entities.User;
import by.bsuir.springbootproject.services.NotificationService;
import by.bsuir.springbootproject.utils.SecurityContextUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

@Controller
@RequestMapping(RoutePaths.NOTIFICATIONS)
@RequiredArgsConstructor
public class NotificationsController {

    private static final String XML_HTTP_REQUEST = "XMLHttpRequest";

    private final NotificationService notificationService;
    private final SecurityContextUtils securityContextUtils;

    @GetMapping
    public ModelAndView page(@RequestParam(defaultValue = "feed") String tab,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "") String q,
                             @RequestParam(defaultValue = "createdAt") String sortField,
                             @RequestParam(defaultValue = "desc") String sortDirection,
                             @RequestHeader(value = "X-Requested-With", required = false) String requestedWith) {
        User user = securityContextUtils.getUserFromContext()
                .orElseThrow(() -> new RuntimeException("Пользователь не авторизован"));

        ModelAndView mv = notificationService.getNotificationsPage(
                user.getId(),
                tab,
                page,
                q,
                sortField,
                sortDirection
        );

        mv.setViewName(XML_HTTP_REQUEST.equals(requestedWith)
                ? ViewPaths.NOTIFICATIONS_CONTENT
                : ViewPaths.NOTIFICATIONS_PAGE);

        return mv;
    }

    @PostMapping(RoutePaths.NOTIFICATIONS_TOGGLE)
    @ResponseBody
    public Map<String, Object> toggleComicNotifications(@RequestParam Integer comicId) {
        User user = securityContextUtils.getUserFromContext()
                .orElseThrow(() -> new RuntimeException("Пользователь не авторизован"));

        try {
            NotificationToggleResult result = notificationService.toggleComicSubscription(user.getId(), comicId);

            return Map.of(
                    "success", true,
                    "comicId", result.getComicId(),
                    "subscribed", result.isSubscribed(),
                    "message", result.isSubscribed() ? "Оповещения включены" : "Оповещения отключены"
            );
        } catch (Exception e) {
            return Map.of(
                    "success", false,
                    "message", e.getMessage() != null ? e.getMessage() : "Не удалось обновить оповещения"
            );
        }
    }

    @PostMapping(RoutePaths.NOTIFICATIONS_DELETE)
    @ResponseBody
    public Map<String, Object> deleteNotification(@RequestParam Integer notificationId) {
        User user = securityContextUtils.getUserFromContext()
                .orElseThrow(() -> new RuntimeException("Пользователь не авторизован"));

        boolean deleted = notificationService.deleteNotification(user.getId(), notificationId);
        long notificationCount = notificationService.getNotificationCount(user.getId());

        return Map.of(
                "success", deleted,
                "notificationCount", notificationCount,
                "hasNotifications", notificationCount > 0
        );
    }
}
