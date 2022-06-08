package antifraud.service;

import antifraud.entity.AmountLimit;
import antifraud.repository.AmountLimitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AmountLimitService {
    private final AmountLimitRepository amountLimitRepository;

    @Autowired
    public AmountLimitService(AmountLimitRepository amountLimitRepository) {
        this.amountLimitRepository = amountLimitRepository;
    }

    public void save(AmountLimit amountLimit) {
        amountLimitRepository.save(amountLimit);
    }

    public Optional<AmountLimit> get() {
        return amountLimitRepository.findById(1L);
    }

}
