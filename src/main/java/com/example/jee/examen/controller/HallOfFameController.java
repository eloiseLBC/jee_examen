package com.example.jee.examen.controller;

import com.example.jee.examen.dto.HallOfFameResponse;
import com.example.jee.examen.service.HallOfFameService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/halloffame")
@RequiredArgsConstructor
public class HallOfFameController {

    private final HallOfFameService hallOfFameService;

    @GetMapping
    public HallOfFameResponse hallOfFame(@RequestParam(defaultValue = "10") int limit) {
        return hallOfFameService.top(limit);
    }
}
