package antifraud.service;

import antifraud.entity.Transaction;
import antifraud.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository repository;

    @Autowired
    public TransactionService(TransactionRepository repository) {
        this.repository = repository;
    }

    public Transaction save(Transaction transaction) {
        return repository.save(transaction);
    }

    public List<Transaction> findAllByNumberAndDateBetween(String number, LocalDateTime date) {
        return repository.findAllByNumberAndDateBetween(number, date.minusMinutes(60), date);
    }

    public List<Transaction> findAll() {
        return repository.findAll();
    }

    public Transaction findById(Long id) {
        return repository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public List<Transaction> findAllByNumber(String number) {
        List<Transaction> transactions = repository.findAllByNumber(number);
        if (transactions.isEmpty())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return transactions;
    }
}
