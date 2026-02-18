package com.example.jee.examen.dto;

import com.example.jee.examen.enums.Category;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ScoreRequest {
    @NotNull
    private Category category;
}
