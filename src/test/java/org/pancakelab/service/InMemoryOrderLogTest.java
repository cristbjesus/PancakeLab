package org.pancakelab.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class InMemoryOrderLogTest {
    private final InMemoryOrderLog log = new InMemoryOrderLog();

    @Test
    public void GivenLoggedMessages_WhenReadingEntries_ThenMessagesReturnedInOrder() {
        // setup
        log.log("first");
        log.log("second");

        // exercise
        List<String> entries = log.entries();

        // verify
        assertEquals(List.of("first", "second"), entries);
    }

    @Test
    public void GivenLoggedMessage_WhenModifyingEntries_ThenUnsupportedOperationExceptionThrown() {
        // setup
        log.log("first");

        // exercise
        List<String> entries = log.entries();

        // verify
        assertThrows(UnsupportedOperationException.class, entries::clear);
    }
}
