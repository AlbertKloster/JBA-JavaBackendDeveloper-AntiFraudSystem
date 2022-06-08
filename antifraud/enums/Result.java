package antifraud.enums;

public enum Result {
    ALLOWED, MANUAL_PROCESSING, PROHIBITED;

    public static boolean isNotValidResult(String input) {
        for (Result result : Result.values())
            if (result.name().equals(input))
                return false;
        return true;
    }
}
