package by.bsuir.springbootproject.controllers;

import by.bsuir.springbootproject.constants.RoutePaths;
import by.bsuir.springbootproject.services.RatingService;
import by.bsuir.springbootproject.utils.SecurityContextUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping(RoutePaths.API_RATINGS)
public class RatingController {

    private final RatingService ratingService;

    @PostMapping("/{comicId}")
    @ResponseBody
    public ResponseEntity<?> rateComic(@PathVariable int comicId,
                                       @RequestParam int value) {
        var userOpt = SecurityContextUtils.getUser();
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "Авторизуйтесь, чтобы оценить"));
        }

        ratingService.rateComic(userOpt.get(), comicId, value);
        return ResponseEntity.ok(Map.of("status", "ok", "value", value));
    }

    @DeleteMapping("/{comicId}")
    @ResponseBody
    public ResponseEntity<?> removeRating(@PathVariable int comicId) {
        var userOpt = SecurityContextUtils.getUser();
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "Авторизация требуема"));
        }

        ratingService.removeRating(userOpt.get(), comicId);
        return ResponseEntity.ok(Map.of("status", "deleted"));
    }
}