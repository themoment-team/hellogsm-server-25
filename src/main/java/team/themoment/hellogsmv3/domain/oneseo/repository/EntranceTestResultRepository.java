package team.themoment.hellogsmv3.domain.oneseo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team.themoment.hellogsmv3.domain.oneseo.entity.EntranceTestResult;
import team.themoment.hellogsmv3.domain.oneseo.entity.Oneseo;

public interface EntranceTestResultRepository extends JpaRepository<EntranceTestResult, Long> {
    EntranceTestResult findByOneseo(Oneseo oneseo);

    boolean existsByFirstTestPassYnIsNotNull();
    boolean existsByFirstTestPassYnIsNull();
    boolean existsBySecondTestPassYnIsNull();
}
