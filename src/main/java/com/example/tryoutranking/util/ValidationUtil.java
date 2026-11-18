package com.example.tryoutranking.util;

public final class ValidationUtil {
    private ValidationUtil() { }

    public static boolean hasName(String firstName, String lastName) {
        return (firstName != null && !firstName.isBlank()) || (lastName != null && !lastName.isBlank());
    }

    public static boolean isValidEmailOrBlank(String email) {
        return email == null || email.isBlank() || email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    }
}
