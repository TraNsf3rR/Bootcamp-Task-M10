package com.accenture.externalapis.demo.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class ClientExceptionTest {

    @Test
    void keepsMessageAndCause() {
        IllegalStateException cause = new IllegalStateException("request failed");

        ClientException exception = new ClientException("External service failed", cause);

        assertEquals("External service failed", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    void acceptsMessageWithoutCause() {
        ClientException exception = new ClientException("External service failed");

        assertEquals("External service failed", exception.getMessage());
    }
}
