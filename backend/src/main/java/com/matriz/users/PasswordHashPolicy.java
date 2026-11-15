package com.matriz.users;

import java.util.regex.Pattern;

/** Validates that a stored value is a BCrypt hash, rather than a disabled marker. */
public final class PasswordHashPolicy {
    private static final Pattern BCRYPT_HASH =
            Pattern.compile("\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}");

    private PasswordHashPolicy() {}

    public static boolean isUsableBcryptHash(String value) {
        return value != null && BCRYPT_HASH.matcher(value).matches();
    }
}
