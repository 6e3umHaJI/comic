package by.bsuir.springbootproject.controllers;

import by.bsuir.springbootproject.constants.RoutePaths;
import by.bsuir.springbootproject.constants.ViewPaths;
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

    @GetMapping({RoutePaths.ROOT, RoutePaths.HOME})
    public ModelAndView openHomePage() {
        ModelAndView modelAndView = new ModelAndView(ViewPaths.HOME);

        modelAndView.addObject("user", SecurityContextUtils.getUser().orElse(null));
        modelAndView.addObject("popularComics", comicService.getMostPopularComics());
        modelAndView.addObject("recentUpdates", comicService.getRecentUpdates());
        modelAndView.addObject("newComics", comicService.getNewestComics());

        return modelAndView;
    }
}