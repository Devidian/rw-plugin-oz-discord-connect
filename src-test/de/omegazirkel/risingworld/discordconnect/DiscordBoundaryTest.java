package de.omegazirkel.risingworld.discordconnect;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.RecordComponent;
import java.util.Locale;

import org.junit.Test;

public class DiscordBoundaryTest {
    @Test
    public void commandSchemaUsesDiscordCompatibleLowercaseNames() {
        CommandRegistry.getRequiredCommands().forEach(command -> {
            assertTrue(command.name().matches("[a-z0-9_-]{1,32}"));
            command.options().forEach(option ->
                    assertTrue(option.getName().matches("[a-z0-9_-]{1,32}")));
        });
    }

    @Test
    public void serverThreadDtosContainNoJdaTypes() {
        assertContainsNoJdaTypes(DiscordChatMessage.class);
        assertContainsNoJdaTypes(DiscordCommandRequest.class);
        assertContainsNoJdaTypes(DiscordCommandResult.class);
    }

    private static void assertContainsNoJdaTypes(Class<?> recordType) {
        for (RecordComponent component : recordType.getRecordComponents()) {
            assertFalse(component.getType().getName().toLowerCase(Locale.ROOT).contains("jda"));
        }
    }
}
