package com.example.jee.examen.service;

import com.example.jee.examen.dto.HallOfFameResponse;
import com.example.jee.examen.enums.PartieStatus;
import com.example.jee.examen.repository.ColonneScoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HallOfFameService {

    private final ColonneScoreRepository colonneScoreRepository;

    public HallOfFameResponse top(int limit) {
        int normalizedLimit = Math.max(1, Math.min(limit, 100));
        List<HallOfFameRow> rows = colonneScoreRepository.findTopByPartieStatus(
                PartieStatus.TERMINE,
                PageRequest.of(0, normalizedLimit)
        );

        List<HallOfFameResponse.Entry> entries = rows.stream()
                .map(r -> new HallOfFameResponse.Entry(r.getPartieId(), r.getPseudo(), r.getScore()))
                .toList();

        return new HallOfFameResponse(entries);
    }
}
