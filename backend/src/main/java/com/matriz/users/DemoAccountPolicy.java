package com.matriz.users;

import java.util.List;

/** Exact usernames reserved for local demonstration accounts. */
public final class DemoAccountPolicy {
    public static final List<String> RESERVED_USERNAMES = List.of("demo.alex", "demo.sam");

    private DemoAccountPolicy() {}

    public static boolean isReservedUsername(String username) {
        return RESERVED_USERNAMES.contains(username);
    }
}
