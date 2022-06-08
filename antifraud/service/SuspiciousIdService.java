package antifraud.service;

import antifraud.entity.SuspiciousIp;
import antifraud.repository.SuspiciousIpRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class SuspiciousIdService {

    private final SuspiciousIpRepository repository;

    @Autowired
    public SuspiciousIdService(SuspiciousIpRepository repository) {
        this.repository = repository;
    }

    public Optional<SuspiciousIp> findSuspiciousIpByIp(String ip) {
        return repository.findSuspiciousIpByIp(ip);
    }

    public SuspiciousIp save(SuspiciousIp suspiciousIp) {
        String ip = suspiciousIp.getIp();
        if (isNotValidIp(ip))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        if (repository.findSuspiciousIpByIp(ip).isPresent())
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        return repository.save(suspiciousIp);
    }

    private boolean isNotValidIp(String ip) {
        String[] numbers = ip.split("\\.");
        if (numbers.length != 4)
            return true;
        for (String number : numbers) {
            if (isNotDigit(number))
                return true;
            if (isNotInRange(number))
                return true;
        }
        return false;
    }

    private boolean isNotDigit(String number) {
        return number.matches("\\D");
    }

    private boolean isNotInRange(String number) {
        int n = Integer.parseInt(number);
        return n < 0 || n > 255;
    }

    public Map<String, Object> delete(String ip) {
        if (isNotValidIp(ip))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        SuspiciousIp suspiciousIp = repository.findSuspiciousIpByIp(ip)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        repository.delete(suspiciousIp);
        return Map.of("status", String.format("IP %s successfully removed!", ip));
    }

    public List<SuspiciousIp> findAll() {
        return repository.findAll();
    }

}
