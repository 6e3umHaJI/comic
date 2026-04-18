package by.bsuir.springbootproject.services.implementation;

import by.bsuir.springbootproject.entities.Complaint;
import by.bsuir.springbootproject.entities.ComplaintStatus;
import by.bsuir.springbootproject.entities.ComplaintType;
import by.bsuir.springbootproject.repositories.ComicRepository;
import by.bsuir.springbootproject.repositories.ComplaintRepository;
import by.bsuir.springbootproject.repositories.ComplaintStatusRepository;
import by.bsuir.springbootproject.repositories.ComplaintTypeRepository;
import by.bsuir.springbootproject.repositories.TranslationRepository;
import by.bsuir.springbootproject.repositories.UserRepository;
import by.bsuir.springbootproject.services.ComplaintService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ComplaintServiceImpl implements ComplaintService {

    private static final String SCOPE_COMIC = "COMIC";
    private static final String SCOPE_TRANSLATION = "TRANSLATION";
    private static final String STATUS_PENDING = "Ожидание";
    private static final int MAX_ACTIVE_COMPLAINTS = 20;

    private final ComplaintRepository complaintRepository;
    private final ComplaintTypeRepository complaintTypeRepository;
    private final ComplaintStatusRepository complaintStatusRepository;
    private final UserRepository userRepository;
    private final ComicRepository comicRepository;
    private final TranslationRepository translationRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ComplaintType> getComplaintTypesForScope(String scope) {
        return complaintTypeRepository.findByScopeOrderByIdAsc(scope);
    }

    @Override
    public void submitComplaint(Integer userId,
                                Integer targetId,
                                Integer complaintTypeId,
                                String description) {
        String actualDescription = description == null ? "" : description.trim();

        if (targetId == null || targetId <= 0) {
            throw new IllegalArgumentException("Не найден объект жалобы.");
        }

        if (complaintTypeId == null || complaintTypeId <= 0) {
            throw new IllegalArgumentException("Выберите тип жалобы.");
        }

        if (actualDescription.isBlank()) {
            throw new IllegalArgumentException("Описание жалобы обязательно к заполнению.");
        }

        long activeComplaints = complaintRepository.countByUser_IdAndStatus_Name(userId, STATUS_PENDING);
        if (activeComplaints >= MAX_ACTIVE_COMPLAINTS) {
            throw new IllegalStateException("У вас уже есть 20 жалоб в ожидании. Дождитесь рассмотрения хотя бы одной.");
        }

        ComplaintType type = complaintTypeRepository.findById(complaintTypeId)
                .orElseThrow(() -> new IllegalArgumentException("Тип жалобы не найден."));

        String scope = type.getScope() == null ? "" : type.getScope().trim().toUpperCase();

        boolean targetExists = switch (scope) {
            case SCOPE_COMIC -> comicRepository.existsById(targetId);
            case SCOPE_TRANSLATION -> translationRepository.existsById(targetId);
            default -> throw new IllegalArgumentException("Некорректный тип жалобы.");
        };

        if (!targetExists) {
            throw new IllegalArgumentException("Объект жалобы не найден.");
        }

        ComplaintStatus pendingStatus = complaintStatusRepository.findByName(STATUS_PENDING)
                .orElseThrow(() -> new IllegalStateException("Статус 'Ожидание' не найден."));

        complaintRepository.save(
                Complaint.builder()
                        .user(userRepository.getReferenceById(userId))
                        .targetId(targetId)
                        .type(type)
                        .description(actualDescription)
                        .status(pendingStatus)
                        .createdAt(LocalDateTime.now())
                        .build()
        );
    }
}
