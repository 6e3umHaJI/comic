package by.bsuir.springbootproject.repositories;

import by.bsuir.springbootproject.entities.ApiMonthlyUsage;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApiMonthlyUsageRepository extends JpaRepository<ApiMonthlyUsage, Integer> {

    Optional<ApiMonthlyUsage> findByProviderAndMonthKey(String provider, String monthKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select usage
            from ApiMonthlyUsage usage
            where usage.provider = :provider
              and usage.monthKey = :monthKey
            """)
    Optional<ApiMonthlyUsage> findLockedByProviderAndMonthKey(@Param("provider") String provider,
                                                              @Param("monthKey") String monthKey);
}