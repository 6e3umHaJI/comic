package by.bsuir.springbootproject.repositories;

import by.bsuir.springbootproject.entities.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    @EntityGraph(attributePaths = {"type", "comic", "chapter", "translation", "translation.language", "translation.user", "actorUser"})
    Page<Notification> findByUser_Id(Integer userId, Pageable pageable);

    long countByUser_Id(Integer userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Notification n set n.isRead = true where n.user.id = :userId and n.isRead = false")
    void markAllAsReadByUserId(Integer userId);

    Optional<Notification> findByIdAndUser_Id(Integer id, Integer userId);
}
