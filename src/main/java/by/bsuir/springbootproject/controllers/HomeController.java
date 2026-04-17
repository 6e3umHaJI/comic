package by.bsuir.springbootproject.controllers;

import by.bsuir.springbootproject.utils.SecurityContextUtils;
import by.bsuir.springbootproject.services.ComicService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final ComicService comicService;
    private final SecurityContextUtils securityContextUtils;

    @GetMapping({"/", "/home"})
    public ModelAndView openHomePage() {
        ModelAndView modelAndView = new ModelAndView("home/home");

        modelAndView.addObject("user", securityContextUtils.getUserFromContext().orElse(null));
        modelAndView.addObject("popularComics", comicService.getMostPopularComics());
        modelAndView.addObject("recentUpdates", comicService.getRecentUpdates());
        modelAndView.addObject("newComics", comicService.getNewestComics());

        return modelAndView;
    }
}