package team.themoment.hellogsmv3.domain.oneseo.service;

import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.AdmissionTicketsResDto;
import team.themoment.hellogsmv3.domain.oneseo.repository.custom.CustomOneseoRepository;

@Service
@RequiredArgsConstructor
public class QueryAdmissionTicketsService {

    private final CustomOneseoRepository oneseoRepository;

    public List<AdmissionTicketsResDto> execute() {
        return oneseoRepository.findAdmissionTickets();
    }
}
