package antifraud.service;

import antifraud.entity.StolenCard;
import antifraud.repository.StolenCardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class StolenCardService {

    private final StolenCardRepository repository;

    @Autowired
    public StolenCardService(StolenCardRepository repository) {
        this.repository = repository;
    }

    public StolenCard save(StolenCard stolenCard) {
        String number = stolenCard.getNumber();

        if (repository.findStolenCardByNumber(number).isPresent())
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        return repository.save(stolenCard);
    }

    public Map<String, Object> delete(String number) {
        StolenCard stolenCard = repository.findStolenCardByNumber(number)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        repository.delete(stolenCard);
        return Map.of("status", String.format("Card %s successfully removed!", number));
    }

    public List<StolenCard> findAll() {
        return repository.findAll();
    }

    public Optional<StolenCard> findStolenCardByNumber(String number) {
        return repository.findStolenCardByNumber(number);
    }

}
