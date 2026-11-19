package com.matriz.ci;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class IntentionalFailureTest {

    @Test
    void shouldFail() {
        assertTrue(false, "Intentional CI failure check");
    }
}
