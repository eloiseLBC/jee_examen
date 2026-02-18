package com.example.jee.examen.service;

import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class DiceService {

    private final Random random = new Random();

    public int[] rollAll() {
        int[] dice = new int[5];
        for (int i = 0; i < 5; i++) {
            dice[i] = rollDie();
        }
        return dice;
    }

    public int[] rerollUnlocked(int[] currentDice, boolean[] locked) {
        int[] next = currentDice.clone();
        for (int i = 0; i < 5; i++) {
            if (!locked[i]) {
                next[i] = rollDie();
            }
        }
        return next;
    }

    private int rollDie() {
        return random.nextInt(6) + 1;
    }
}
