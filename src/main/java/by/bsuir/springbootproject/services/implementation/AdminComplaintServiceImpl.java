package by.bsuir.springbootproject.services.implementation;

import by.bsuir.springbootproject.constants.Values;
import by.bsuir.springbootproject.dto.AdminComplaintItem;
import by.bsuir.springbootproject.entities.Comic;
import by.bsuir.springbootproject.entities.Complaint;
import by.bsuir.springbootproject.entities.ComplaintStatus;
import by.bsuir.springbootproject.entities.ComplaintType;
import by.bsuir.springbootproject.entities.Translation;
import by.bsuir.springbootproject.repositories.ComicRepository;
import by.bsuir.springbootproject.repositories.ComplaintRepository;
import by.bsuir.springbootproject.repositories.ComplaintStatusRepository;
import by.bsuir.springbootproject.repositories.ComplaintTypeRepository;
import by.bsuir.springbootproject.repositories.TranslationRepository;
import by.bsuir.springbootproject.services.AdminComplaintService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.ModelAndView;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminComplaintServiceImpl implements AdminComplaintService {

    private static final String SCOPE_COMIC = "COMIC";
    private static final String SCOPE_TRANSLATION = "TRANSLATION";
    private static final String STATUS_PENDING = "Ожидание";
    private static final String STATUS_IN_REVIEW = "На рассмотрении";

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final ComplaintRepository complaintRepository;
    private final ComplaintTypeRepository complaintTypeRepository;
    private final ComplaintStatusRepository complaintStatusRepository;
    private final ComicRepository comicRepository;
    private final TranslationRepository translationRepository;

    @Override
    public ModelAndView getComplaintsPage(String scope,
                                          String typeId,
                                          String q,
                                          String sortDirection,
                                          int page) {
        String actualScope = SCOPE_COMIC.equalsIgnoreCase(scope) ? SCOPE_COMIC : SCOPE_TRANSLATION;
        String actualSortDirection = "asc".equalsIgnoreCase(sortDirection) ? "asc" : "desc";
        String actualQuery = q == null ? "" : q.trim();
        int safePage = Math.max(page, 0);

        List<ComplaintType> complaintTypes = complaintTypeRepository.findByScopeOrderByIdAsc(actualScope);
        Integer selectedTypeId = parseTypeId(typeId, complaintTypes);

        Sort.Direction direction = "asc".equals(actualSortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Page<Complaint> complaintPage = SCOPE_COMIC.equals(actualScope)
                ? complaintRepository.findAdminComicComplaints(
                selectedTypeId,
                actualQuery,
                List.of(STATUS_PENDING, STATUS_IN_REVIEW),
                PageRequest.of(
                        safePage,
                        Values.COMPLAINTS_PAGE_SIZE,
                        Sort.by(direction, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"))
                )
        )
                : complaintRepository.findAdminTranslationComplaints(
                selectedTypeId,
                actualQuery,
                List.of(STATUS_PENDING, STATUS_IN_REVIEW),
                PageRequest.of(
                        safePage,
                        Values.COMPLAINTS_PAGE_SIZE,
                        Sort.by(direction, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"))
                )
        );

        List<AdminComplaintItem> items = complaintPage.getContent().stream()
                .map(this::toAdminComplaintItem)
                .toList();

        ModelAndView mv = new ModelAndView("admin/complaints-page");
        mv.addObject("scope", actualScope);
        mv.addObject("selectedTypeId", selectedTypeId);
        mv.addObject("q", actualQuery);
        mv.addObject("sortDirection", actualSortDirection);
        mv.addObject("complaintTypes", complaintTypes);
        mv.addObject("statusOptions", complaintStatusRepository.findAllByOrderByIdAsc());
        mv.addObject("complaints", items);

        applyPagination(mv, complaintPage);
        return mv;
    }

    @Override
    @Transactional
    public void updateComplaintStatus(Integer complaintId, Integer statusId) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new IllegalArgumentException("Жалоба не найдена."));

        ComplaintStatus status = complaintStatusRepository.findById(statusId)
                .orElseThrow(() -> new IllegalArgumentException("Статус не найден."));

        complaint.setStatus(status);
        complaintRepository.save(complaint);
    }

    private Integer parseTypeId(String typeId, List<ComplaintType> complaintTypes) {
        if (typeId == null || typeId.isBlank()) {
            return null;
        }

        try {
            Integer parsed = Integer.parseInt(typeId);
            boolean existsInScope = complaintTypes.stream().anyMatch(type -> type.getId().equals(parsed));
            return existsInScope ? parsed : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private AdminComplaintItem toAdminComplaintItem(Complaint complaint) {
        String scope = complaint.getType() != null ? complaint.getType().getScope() : SCOPE_TRANSLATION;
        String typeName = complaint.getType() != null ? complaint.getType().getName() : "Жалоба";
        String statusName = complaint.getStatus() != null ? complaint.getStatus().getName() : STATUS_PENDING;
        Integer statusId = complaint.getStatus() != null ? complaint.getStatus().getId() : null;

        String createdAtFormatted = complaint.getCreatedAt() != null
                ? complaint.getCreatedAt().format(DATE_TIME_FORMATTER)
                : "";

        String username = complaint.getUser() != null ? complaint.getUser().getUsername() : "Неизвестный пользователь";
        String email = complaint.getUser() != null ? complaint.getUser().getEmail() : "";

        if (SCOPE_COMIC.equalsIgnoreCase(scope)) {
            Comic comic = comicRepository.findById(complaint.getTargetId()).orElse(null);

            String targetTitle = comic != null ? comic.getTitle() : "Комикс был удалён";
            String targetSubtitle = comic != null
                    ? buildComicSubtitle(comic)
                    : "Объект недоступен";

            String targetUrl = comic != null ? "/comics/" + comic.getId() : null;
            String cover = comic != null ? comic.getCover() : null;

            return new AdminComplaintItem(
                    complaint.getId(),
                    scope,
                    typeName,
                    statusId,
                    statusName,
                    complaint.getDescription(),
                    createdAtFormatted,
                    targetUrl,
                    targetTitle,
                    targetSubtitle,
                    cover,
                    username,
                    email
            );
        }

        Translation translation = translationRepository.findReaderTranslationById(complaint.getTargetId()).orElse(null);

        String targetTitle = translation != null
                ? translation.getChapter().getComic().getTitle()
                : "Глава или перевод были удалены";

        String targetSubtitle = translation != null
                ? "Глава " + translation.getChapter().getChapterNumber() + " • " + translation.getLanguage().getName()
                : "Объект недоступен";

        String targetUrl = translation != null ? "/read/" + translation.getId() : null;
        String cover = translation != null ? translation.getChapter().getComic().getCover() : null;

        return new AdminComplaintItem(
                complaint.getId(),
                scope,
                typeName,
                statusId,
                statusName,
                complaint.getDescription(),
                createdAtFormatted,
                targetUrl,
                targetTitle,
                targetSubtitle,
                cover,
                username,
                email
        );
    }

    private String buildComicSubtitle(Comic comic) {
        StringBuilder subtitle = new StringBuilder();

        if (comic.getOriginalTitle() != null && !comic.getOriginalTitle().isBlank()) {
            subtitle.append(comic.getOriginalTitle());
        }

        if (comic.getReleaseYear() != null) {
            if (subtitle.length() > 0) {
                subtitle.append(" • ");
            }
            subtitle.append(comic.getReleaseYear());
        }

        return subtitle.length() > 0 ? subtitle.toString() : "Жалоба на тайтл";
    }

    private void applyPagination(ModelAndView mv, Page<?> pageData) {
        int totalPages = pageData.getTotalPages();
        int currentPage = totalPages == 0 ? 0 : pageData.getNumber() + 1;
        int visiblePages = 5;
        int beginPage = totalPages == 0 ? 0 : Math.max(1, currentPage - 2);
        int endPage = totalPages == 0 ? 0 : Math.min(beginPage + visiblePages - 1, totalPages);

        if (totalPages > 0 && endPage - beginPage < visiblePages - 1) {
            beginPage = Math.max(1, endPage - visiblePages + 1);
        }

        mv.addObject("currentPage", currentPage);
        mv.addObject("totalPages", totalPages);
        mv.addObject("beginPage", beginPage);
        mv.addObject("endPage", endPage);
        mv.addObject("showLeftDots", totalPages > 0 && beginPage > 2);
        mv.addObject("showRightDots", totalPages > 0 && endPage < totalPages - 1);
    }
}
