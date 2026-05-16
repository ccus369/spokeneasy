package com.spokeneasy.app.core.model;

/**
 * Shared data classes for the application.
 */
public final class Common {

    private Common() {}

    public static class UiResult<T> {
        private final T data;
        private final String errorMessage;
        private final boolean isSuccess;

        private UiResult(T data, String errorMessage, boolean isSuccess) {
            this.data = data;
            this.errorMessage = errorMessage;
            this.isSuccess = isSuccess;
        }

        public static <T> UiResult<T> success(T data) {
            return new UiResult<>(data, null, true);
        }

        public static <T> UiResult<T> error(String message) {
            return new UiResult<>(null, message, false);
        }

        public T getData() { return data; }
        public String getErrorMessage() { return errorMessage; }
        public boolean isSuccess() { return isSuccess; }
    }

    public static class ScoreResult {
        private final int score;
        private final String detail;

        public ScoreResult(int score, String detail) {
            this.score = score;
            this.detail = detail;
        }

        public int getScore() { return score; }
        public String getDetail() { return detail; }
    }
}
