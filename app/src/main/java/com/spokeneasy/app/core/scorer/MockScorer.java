package com.spokeneasy.app.core.scorer;

import java.util.Random;

/**
 * Mock implementation of Scorer for development and testing.
 * Returns a random score. Replace with XunfeiScorer for production.
 */
public class MockScorer implements Scorer {

    private final Random random = new Random();
    private String lastDetail;

    @Override
    public int score(String expectedText, String audioFilePath) {
        int score = random.nextInt(61) + 40; // 40-100

        if (score >= 90) {
            lastDetail = "Great pronunciation!";
        } else if (score >= 70) {
            lastDetail = "Good, but some sounds need improvement.";
        } else if (score >= 50) {
            lastDetail = "Keep practicing, several sounds need work.";
        } else {
            lastDetail = "Try again, focus on the pronunciation.";
        }

        return score;
    }

    @Override
    public String getDetail() {
        return lastDetail != null ? lastDetail : "No score yet.";
    }
}
