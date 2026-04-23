package by.bsuir.springbootproject.services.implementation;

import by.bsuir.springbootproject.entities.Translation;
import by.bsuir.springbootproject.repositories.TranslationRepository;
import by.bsuir.springbootproject.services.TranslationCabinetService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TranslationCabinetServiceImpl implements TranslationCabinetService {

    private static final int MAX_QUERY_LENGTH = 255;

    private final TranslationRepository translationRepository;

    @Override
    public Page<Translation> getUserTranslationsPage(Integer userId,
                                                     String q,
                                                     String sortDirection,
                                                     int page,
                                                     int size) {
        String normalizedQ = normalizeQuery(q);

        Sort sort = "asc".equalsIgnoreCase(sortDirection)
                ? Sort.by("createdAt").ascending().and(Sort.by("id").ascending())
                : Sort.by("createdAt").descending().and(Sort.by("id").descending());

        return translationRepository.findUserUploadedPage(
                userId,
                normalizedQ,
                PageRequest.of(Math.max(page, 0), size, sort)
        );
    }

    private String normalizeQuery(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim();
        return normalized.length() > MAX_QUERY_LENGTH
                ? normalized.substring(0, MAX_QUERY_LENGTH)
                : normalized;
    }
}
