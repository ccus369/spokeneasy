package com.spokeneasy.app.core.scorer;

/**
 * Interface for pronunciation scoring.
 */
public interface Scorer {

    int MAX_SCORE = 100;
    int MIN_SCORE = 0;

    /**
     * Score the user's pronunciation against the expected text.
     *
     * @param expectedText the correct text
     * @param audioFilePath path to the user's recording
     * @return score 0-100
     */
    int score(String expectedText, String audioFilePath);

    /**
     * Get detailed feedback for the last scoring.
     *
     * @return detail string describing the result
     */
    String getDetail();
}
