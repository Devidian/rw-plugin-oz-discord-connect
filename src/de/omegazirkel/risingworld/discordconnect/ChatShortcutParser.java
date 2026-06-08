package de.omegazirkel.risingworld.discordconnect;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ChatShortcutParser {
    private static final Pattern SHORTCUT_PATTERN = Pattern
            .compile("(^|\\s)(\\+(?:screennogui|screen|sng|s|tp|t))(?=\\s|$)");
    private static final String SCREENSHOT_ICON = "\uD83D\uDDBC\uFE0F";

    private ChatShortcutParser() {
    }

    public static Result parse(String text, String teleportCommand) {
        if (text == null || text.isEmpty()) {
            return new Result("", false, false);
        }
        Matcher matcher = SHORTCUT_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();
        boolean screenshotWithGui = false;
        boolean screenshotWithoutGui = false;

        while (matcher.find()) {
            String shortcut = matcher.group(2);
            String replacement = switch (shortcut) {
                case "+screennogui", "+sng" -> {
                    screenshotWithoutGui = true;
                    yield SCREENSHOT_ICON;
                }
                case "+screen", "+s" -> {
                    screenshotWithGui = true;
                    yield SCREENSHOT_ICON;
                }
                case "+tp", "+t" -> teleportCommand == null ? shortcut : teleportCommand;
                default -> shortcut;
            };
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(matcher.group(1) + replacement));
        }
        matcher.appendTail(buffer);
        return new Result(buffer.toString(), screenshotWithGui, screenshotWithoutGui);
    }

    public record Result(String message, boolean screenshotWithGui, boolean screenshotWithoutGui) {
        public boolean hasScreenshot() {
            return screenshotWithGui || screenshotWithoutGui;
        }
    }
}
