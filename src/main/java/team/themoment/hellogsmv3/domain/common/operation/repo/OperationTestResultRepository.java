package team.themoment.hellogsmv3.domain.common.operation.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import team.themoment.hellogsmv3.domain.common.operation.entity.OperationTestResult;

import java.util.Optional;

public interface OperationTestResultRepository extends JpaRepository<OperationTestResult, Long> {
    @Query("SELECT o FROM OperationTestResult o WHERE o.id = 1")
    Optional<OperationTestResult> findTestResult();
}
