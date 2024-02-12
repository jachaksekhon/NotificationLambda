package dev.jsekhon;

public class PhoneNumberValidator {
    // Regular expression pattern for a valid phone number
    private static final String PHONE_NUMBER_PATTERN = "^\\d{10}$";

    // Method to validate a phone number
    public static boolean isValid(String phoneNumber) {
        // Check if the phone number matches the pattern
        return phoneNumber != null && phoneNumber.matches(PHONE_NUMBER_PATTERN);
    }
}
