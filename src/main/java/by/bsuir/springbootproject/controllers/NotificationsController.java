package by.bsuir.springbootproject.controllers;

import by.bsuir.springbootproject.constants.RoutePaths;
import by.bsuir.springbootproject.constants.ViewPaths;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping(RoutePaths.NOTIFICATIONS)
public class NotificationsController {

    @GetMapping
    public String page() {
        return ViewPaths.NOTIFICATIONS_PAGE;
    }
}
