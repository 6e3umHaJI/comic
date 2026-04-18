package by.bsuir.springbootproject.controllers;

import by.bsuir.springbootproject.services.NotificationService;
import by.bsuir.springbootproject.utils.SecurityContextUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalViewModelAdvice {

    private final SecurityContextUtils securityContextUtils;
    private final NotificationService notificationService;

    @ModelAttribute
    public void addHeaderNotificationState(Model model) {
        long notificationCount = securityContextUtils.getUserFromContext()
                .map(user -> notificationService.getNotificationCount(user.getId()))
                .orElse(0L);

        long unreadNotificationCount = securityContextUtils.getUserFromContext()
                .map(user -> notificationService.getUnreadNotificationCount(user.getId()))
                .orElse(0L);

        model.addAttribute("notificationCount", notificationCount);
        model.addAttribute("notificationCountLabel", notificationCount > 99 ? "99+" : String.valueOf(notificationCount));
        model.addAttribute("unreadNotificationCount", unreadNotificationCount);
        model.addAttribute("hasUnreadNotifications", unreadNotificationCount > 0);
    }
}
