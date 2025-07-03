package team.themoment.hellogsmv3.domain.oneseo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team.themoment.hellogsmv3.domain.oneseo.entity.EntranceTestResult;
import team.themoment.hellogsmv3.domain.oneseo.entity.Oneseo;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.YesNo;

import java.util.List;

public interface EntranceTestResultRepository extends JpaRepository<EntranceTestResult, Long> {
    EntranceTestResult findByOneseo(Oneseo oneseo);
    List<EntranceTestResult> findAllByFirstTestPassYnOrSecondTestPassYn(YesNo firstTestPassYn, YesNo secondTestPassYn);
    boolean existsByFirstTestPassYnIsNotNull();
    boolean existsByFirstTestPassYnIsNull();
    boolean existsBySecondTestPassYnIsNull();
}
