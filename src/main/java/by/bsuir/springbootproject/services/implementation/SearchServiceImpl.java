package by.bsuir.springbootproject.services.implementation;

import by.bsuir.springbootproject.dto.QuickSearchComicItem;
import by.bsuir.springbootproject.entities.Comic;
import by.bsuir.springbootproject.repositories.ComicRepository;
import by.bsuir.springbootproject.services.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private static final int QUICK_SEARCH_PAGE_SIZE = 20;

    private final ComicRepository comicRepository;

    @Override
    public Map<String, Object> quickSearch(String q, int page) {
        String query = q == null ? "" : q.trim();
        int safePage = Math.max(page, 0);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("query", query);
        response.put("page", safePage);
        response.put("pageSize", QUICK_SEARCH_PAGE_SIZE);

        if (!StringUtils.hasText(query)) {
            response.put("total", 0L);
            response.put("items", List.of());
            return response;
        }

        Pageable pageable = PageRequest.of(safePage, QUICK_SEARCH_PAGE_SIZE);
        Page<Comic> result = comicRepository.findQuickSearchResults(query, pageable);

        List<QuickSearchComicItem> items = result.getContent().stream()
                .map(comic -> new QuickSearchComicItem(
                        comic.getId(),
                        comic.getTitle(),
                        comic.getOriginalTitle(),
                        comic.getShortDescription(),
                        comic.getCover()
                ))
                .toList();

        response.put("total", result.getTotalElements());
        response.put("items", items);
        return response;
    }
}
