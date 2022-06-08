package antifraud.repository;

import antifraud.entity.SuspiciousIp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SuspiciousIpRepository extends JpaRepository<SuspiciousIp, Long> {
    Optional<SuspiciousIp> findSuspiciousIpByIp(String ip);
}
