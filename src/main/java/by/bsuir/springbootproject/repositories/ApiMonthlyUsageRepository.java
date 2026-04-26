package by.bsuir.springbootproject.repositories;

import by.bsuir.springbootproject.entities.ApiMonthlyUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApiMonthlyUsageRepository extends JpaRepository<ApiMonthlyUsage, Integer> {
    Optional<ApiMonthlyUsage> findByProviderAndMonthKey(String provider, String monthKey);
}
