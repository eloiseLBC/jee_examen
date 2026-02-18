package com.example.jee.examen.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class HallOfFameResponse {
    private List<Entry> entries;

    @Data
    @AllArgsConstructor
    public static class Entry {
        private Long partieId;
        private String pseudo;
        private Integer score;
    }
}
