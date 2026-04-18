package by.bsuir.springbootproject.services;

import java.util.Map;

public interface SearchService {
    Map<String, Object> quickSearch(String q, int page);
}
