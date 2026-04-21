package by.bsuir.springbootproject.repositories;

import by.bsuir.springbootproject.entities.UserSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserSectionRepository extends JpaRepository<UserSection, Integer> {

    Optional<UserSection> findByIdAndUserId(Integer id, Integer userId);

    boolean existsByUserIdAndNameIgnoreCase(Integer userId, String name);

    boolean existsByUserIdAndNameIgnoreCaseAndIdNot(Integer userId, String name, Integer id);

    long countByUserIdAndIsDefaultFalse(Integer userId);

    List<UserSection> findByUserIdOrderByIsDefaultDescNameAsc(Integer userId);

    @Query("""
    select us, coalesce(count(sc.id), 0)
    from UserSection us
    left join us.savedComics sc
    where us.user.id = :userId
    group by us
    order by us.isDefault desc, us.name asc
    """)
    List<Object[]> findAllWithCountsByUserId(@Param("userId") Integer userId);
}
