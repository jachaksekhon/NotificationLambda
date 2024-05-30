package dev.jsekhon;

public class PhoneNumberValidator {
    private static final String PHONE_NUMBER_PATTERN = "^\\d{10}$";

    public static boolean isValid(String phoneNumber) {
        return phoneNumber != null && phoneNumber.matches(PHONE_NUMBER_PATTERN);
    }
}
