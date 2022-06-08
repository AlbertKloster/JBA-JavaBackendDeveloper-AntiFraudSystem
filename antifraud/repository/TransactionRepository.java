package antifraud.repository;

import antifraud.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findAllByNumberAndDateBetween(String number, LocalDateTime date1, LocalDateTime date2);
    List<Transaction> findAllByNumber(String number);
}
