package org.pancakelab;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class MainTest {
    @Test
    public void GivenDemoUseCase_WhenRunning_ThenItCompletesWithoutErrors() {
        // setup
        PrintStream out = new PrintStream(new ByteArrayOutputStream());

        // exercise & verify
        assertDoesNotThrow(() -> Main.run(out));
    }
}
