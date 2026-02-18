package com.example.jee.examen.service;

import com.example.jee.examen.entity.ColonneScore;
import com.example.jee.examen.enums.Category;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ScoreService {

    public Map<Category, Integer> possibleScores(int[] dice, ColonneScore sheet) {
        Map<Category, Integer> result = new EnumMap<>(Category.class);
        for (Category category : Category.values()) {
            if (!isFilled(sheet, category)) {
                result.put(category, score(category, dice));
            }
        }
        return result;
    }

    public int score(Category cat, int[] dice) {
        int sum = Arrays.stream(dice).sum();
        int[] counts = counts(dice);

        return switch (cat) {
            case ONE -> counts[1] * 1;
            case TWO -> counts[2] * 2;
            case THREE -> counts[3] * 3;
            case FOUR -> counts[4] * 4;
            case FIVE -> counts[5] * 5;
            case SIX -> counts[6] * 6;
            case BRELAN -> hasAtLeastNOfAKind(counts, 3) ? sum : 0;
            case CARRE -> hasAtLeastNOfAKind(counts, 4) ? sum : 0;
            case FULL -> isFull(counts) ? 25 : 0;
            case PETITE_SUITE -> isSmallStraight(counts) ? 30 : 0;
            case GRANDE_SUITE -> isLargeStraight(counts) ? 40 : 0;
            case YAM -> hasAtLeastNOfAKind(counts, 5) ? 50 : 0;
            case CHANCE -> sum;
        };
    }

    public int[] counts(int[] dice) {
        int[] counts = new int[7];
        for (int die : dice) {
            if (die >= 1 && die <= 6) {
                counts[die]++;
            }
        }
        return counts;
    }

    public boolean isFull(int[] counts) {
        boolean has3 = false;
        boolean has2 = false;
        for (int i = 1; i <= 6; i++) {
            if (counts[i] == 3) has3 = true;
            if (counts[i] == 2) has2 = true;
        }
        return has3 && has2;
    }

    public boolean isSmallStraight(int[] counts) {
        return hasRun(counts, 1, 4) || hasRun(counts, 2, 4) || hasRun(counts, 3, 4);
    }

    public boolean isLargeStraight(int[] counts) {
        return hasRun(counts, 1, 5) || hasRun(counts, 2, 5);
    }

    private boolean hasAtLeastNOfAKind(int[] counts, int n) {
        for (int i = 1; i <= 6; i++) {
            if (counts[i] >= n) return true;
        }
        return false;
    }

    private boolean hasRun(int[] counts, int start, int length) {
        for (int i = start; i < start + length; i++) {
            if (counts[i] == 0) return false;
        }
        return true;
    }

    public boolean isFilled(ColonneScore sheet, Category category) {
        return switch (category) {
            case ONE -> sheet.getScore1() != null;
            case TWO -> sheet.getScore2() != null;
            case THREE -> sheet.getScore3() != null;
            case FOUR -> sheet.getScore4() != null;
            case FIVE -> sheet.getScore5() != null;
            case SIX -> sheet.getScore6() != null;
            case BRELAN -> sheet.getScoreBrelan() != null;
            case CARRE -> sheet.getScoreCarre() != null;
            case FULL -> sheet.getScoreFull() != null;
            case PETITE_SUITE -> sheet.getScorePetiteSuite() != null;
            case GRANDE_SUITE -> sheet.getScoreGrandeSuite() != null;
            case YAM -> sheet.getScoreYam() != null;
            case CHANCE -> sheet.getScoreChance() != null;
        };
    }

    public void setCategoryScore(ColonneScore sheet, Category category, int value) {
        switch (category) {
            case ONE -> sheet.setScore1(value);
            case TWO -> sheet.setScore2(value);
            case THREE -> sheet.setScore3(value);
            case FOUR -> sheet.setScore4(value);
            case FIVE -> sheet.setScore5(value);
            case SIX -> sheet.setScore6(value);
            case BRELAN -> sheet.setScoreBrelan(value);
            case CARRE -> sheet.setScoreCarre(value);
            case FULL -> sheet.setScoreFull(value);
            case PETITE_SUITE -> sheet.setScorePetiteSuite(value);
            case GRANDE_SUITE -> sheet.setScoreGrandeSuite(value);
            case YAM -> sheet.setScoreYam(value);
            case CHANCE -> sheet.setScoreChance(value);
        }
    }

    public boolean allCategoriesFilled(ColonneScore sheet) {
        for (Category category : Category.values()) {
            if (!isFilled(sheet, category)) {
                return false;
            }
        }
        return true;
    }

    public Category firstUnfilledCategory(ColonneScore sheet) {
        List<Category> order = List.of(
                Category.ONE, Category.TWO, Category.THREE, Category.FOUR, Category.FIVE, Category.SIX,
                Category.BRELAN, Category.CARRE, Category.FULL, Category.PETITE_SUITE,
                Category.GRANDE_SUITE, Category.YAM, Category.CHANCE
        );
        for (Category category : order) {
            if (!isFilled(sheet, category)) {
                return category;
            }
        }
        return null;
    }

    public void recomputeTotals(ColonneScore sheet, int extraYamCount) {
        int n1 = orZero(sheet.getScore1());
        int n2 = orZero(sheet.getScore2());
        int n3 = orZero(sheet.getScore3());
        int n4 = orZero(sheet.getScore4());
        int n5 = orZero(sheet.getScore5());
        int n6 = orZero(sheet.getScore6());

        int totalNumbers = n1 + n2 + n3 + n4 + n5 + n6;
        int bonus = totalNumbers >= 63 ? 35 : 0;

        int total = totalNumbers + bonus
                + orZero(sheet.getScoreBrelan())
                + orZero(sheet.getScoreCarre())
                + orZero(sheet.getScoreFull())
                + orZero(sheet.getScorePetiteSuite())
                + orZero(sheet.getScoreGrandeSuite())
                + orZero(sheet.getScoreYam())
                + orZero(sheet.getScoreChance())
                + (Math.max(extraYamCount, 0) * 100);

        sheet.setTotalNumbers(totalNumbers);
        sheet.setTotalNumbersBonus(bonus);
        sheet.setScoreTotal(total);
    }

    private int orZero(Integer value) {
        return value == null ? 0 : value;
    }
}
