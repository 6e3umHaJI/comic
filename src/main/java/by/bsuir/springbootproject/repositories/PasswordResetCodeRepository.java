package by.bsuir.springbootproject.repositories;

import by.bsuir.springbootproject.entities.PasswordResetCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetCodeRepository extends JpaRepository<PasswordResetCode, Integer> {

    @Query("""
        select prc
        from PasswordResetCode prc
        join fetch prc.user u
        where u.id = :userId and prc.used = false
        order by prc.createdAt desc
    """)
    Optional<PasswordResetCode> findTopActiveByUserId(Integer userId);

    @Modifying
    @Query("""
        update PasswordResetCode prc
        set prc.used = true
        where prc.user.id = :userId and prc.used = false
    """)
    void invalidateAllByUserId(Integer userId);
}