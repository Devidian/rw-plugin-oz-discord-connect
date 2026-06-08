package de.omegazirkel.risingworld.discordconnect;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ChatShortcutParserTest {
    @Test
    public void parsesScreenshotShortcutAtEndOfMessage() {
        ChatShortcutParser.Result result = ChatShortcutParser.parse("look here +s", "`goto 1 2 3`");

        assertEquals("look here \uD83D\uDDBC\uFE0F", result.message());
        assertTrue(result.screenshotWithGui());
        assertFalse(result.screenshotWithoutGui());
    }

    @Test
    public void parsesScreenshotNoGuiShortcutAtEndOfMessage() {
        ChatShortcutParser.Result result = ChatShortcutParser.parse("look here +sng", "`goto 1 2 3`");

        assertEquals("look here \uD83D\uDDBC\uFE0F", result.message());
        assertFalse(result.screenshotWithGui());
        assertTrue(result.screenshotWithoutGui());
    }

    @Test
    public void parsesTeleportShortcutAtEndOfMessage() {
        ChatShortcutParser.Result result = ChatShortcutParser.parse("come here +t", "`goto 1 2 3`");

        assertEquals("come here `goto 1 2 3`", result.message());
        assertFalse(result.hasScreenshot());
    }

    @Test
    public void ignoresShortcutPrefixInsideNormalText() {
        ChatShortcutParser.Result result = ChatShortcutParser.parse("keep +screenshot text", "`goto 1 2 3`");

        assertEquals("keep +screenshot text", result.message());
        assertFalse(result.hasScreenshot());
    }
}
