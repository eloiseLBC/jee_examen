package com.example.jee.examen.service;

import com.example.jee.examen.entity.ColonneScore;
import com.example.jee.examen.enums.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScoreServiceTest {

    private ScoreService scoreService;

    @BeforeEach
    void setUp() {
        scoreService = new ScoreService();
    }

    @Test
    void shouldScoreFull() {
        int[] dice = {2, 2, 3, 3, 3};
        assertEquals(25, scoreService.score(Category.FULL, dice));
    }

    @Test
    void shouldScoreSmallAndLargeStraight() {
        assertEquals(30, scoreService.score(Category.PETITE_SUITE, new int[]{1, 2, 3, 4, 6}));
        assertEquals(40, scoreService.score(Category.GRANDE_SUITE, new int[]{2, 3, 4, 5, 6}));
    }

    @Test
    void shouldScoreYam() {
        assertEquals(50, scoreService.score(Category.YAM, new int[]{5, 5, 5, 5, 5}));
    }

    @Test
    void shouldApplyNumbersBonusAt63() {
        ColonneScore sheet = ColonneScore.builder().build();
        sheet.setScore1(3);
        sheet.setScore2(6);
        sheet.setScore3(9);
        sheet.setScore4(12);
        sheet.setScore5(15);
        sheet.setScore6(18);

        scoreService.recomputeTotals(sheet, 0);

        assertEquals(63, sheet.getTotalNumbers());
        assertEquals(35, sheet.getTotalNumbersBonus());
        assertEquals(98, sheet.getScoreTotal());
    }
}
