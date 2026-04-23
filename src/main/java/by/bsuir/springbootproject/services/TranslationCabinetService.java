package by.bsuir.springbootproject.services;

import by.bsuir.springbootproject.entities.Translation;
import org.springframework.data.domain.Page;

public interface TranslationCabinetService {
    Page<Translation> getUserTranslationsPage(Integer userId, String q, String sortDirection, int page, int size);
}
