package de.omegazirkel.risingworld.discordconnect;

import java.util.List;
import java.util.function.Function;
import java.util.function.ToLongFunction;

/** Deterministic saved-ID lookup and exact-name adoption. */
final class DiscordThreadSelector {
    private DiscordThreadSelector() {
    }

    static <T> T select(List<T> candidates, Long savedId, String topic,
            ToLongFunction<T> id, Function<T, String> name) {
        if (savedId != null) {
            for (T candidate : candidates) {
                if (id.applyAsLong(candidate) == savedId) return candidate;
            }
        }
        T match = null;
        for (T candidate : candidates) {
            if (!topic.equals(name.apply(candidate))) continue;
            if (match != null) throw new IllegalStateException("Ambiguous Discord threads named '" + topic + "'");
            match = candidate;
        }
        return match;
    }
}
