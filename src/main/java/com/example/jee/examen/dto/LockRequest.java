package com.example.jee.examen.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class LockRequest {
    @NotNull
    private List<Integer> lockedIndexes;
}
