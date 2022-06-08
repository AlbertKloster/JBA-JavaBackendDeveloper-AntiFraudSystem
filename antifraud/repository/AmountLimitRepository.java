package antifraud.repository;

import antifraud.entity.AmountLimit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AmountLimitRepository extends JpaRepository<AmountLimit, Long> {
}
