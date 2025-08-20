package team.themoment.hellogsmv3.domain.common.utility.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import team.themoment.hellogsmv3.domain.oneseo.repository.OneseoRepository;
import team.themoment.hellogsmv3.global.exception.error.ExpectedException;

@Service
@RequiredArgsConstructor
public class DeleteOneseoService {

    private final OneseoRepository oneseoRepository;

    public void execute(String submitCode) {
        if (oneseoRepository.deleteByOneseoSubmitCode(submitCode)) {
            throw new ExpectedException("해당 접수 번호에 해당하는 원서가 존재하지 않습니다.", HttpStatus.NOT_FOUND);
        }
    }
}