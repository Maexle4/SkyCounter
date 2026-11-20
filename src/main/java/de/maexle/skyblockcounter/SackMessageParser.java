package de.maexle.skyblockcounter;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses Hypixel's {@code [Sacks]} chat messages to track collected items.
 *
 * <p>Hypixel sends the message as a component tree where the visible part only says
 * something like {@code [Sacks] +16 items}, while the real contents (for example
 * {@code +1 Corleonite (Crystal Hollows Sack)}) live inside a {@link HoverEvent.ShowText}.
 * Because the whole tree arrives at once, no actual mouse hovering is needed: the hover
 * payload can be read straight out of the component.
 */
public final class SackMessageParser {

    private static final Logger LOGGER = LoggerFactory.getLogger("skyblockcounter");

    /** Marker that identifies the message type in the visible text. */
    private static final String SACKS_MARKER = "[Sacks]";

    /**
     * Matches one collected-item line inside the tooltip, e.g.
     * {@code +1 Corleonite (Crystal Hollows Sack)}.
     * Group 1 is the amount, group 2 the item name.
     */
    private static final Pattern ITEM_LINE =
            Pattern.compile("\\+(\\d+)\\s+([A-Za-z][A-Za-z' \\-]*)");

    private SackMessageParser() {
    }

    /**
     * Extracts the Corleonite amount from a {@code [Sacks]} message.
     *
     * @return the collected Corleonite amount, or 0 when the message is unrelated to
     *         Corleonite or could not be parsed
     */
    public static int parseCorleonite(Component message) {
        if (message == null) {
            return 0;
        }

        String visible = message.getString();
        if (!visible.contains(SACKS_MARKER)) {
            return 0;
        }

        // Hypixel nests components so a parent and its child can both carry the same
        // HoverEvent. Collect the distinct tooltip components first, then parse each one
        // exactly once instead of blindly concatenating (which double counts).
        java.util.Set<String> seenTooltips = new java.util.LinkedHashSet<>();
        collectHoverTexts(message, seenTooltips);

        int total = 0;
        boolean matched = false;
        StringBuilder payloadDump = new StringBuilder();

        for (String tooltip : seenTooltips) {
            payloadDump.append(tooltip.replace("\n", " | ")).append("  ///  ");

            for (String line : tooltip.split("\\R")) {
                Matcher matcher = ITEM_LINE.matcher(line);
                while (matcher.find()) {
                    String itemName = matcher.group(2).trim();
                    if (!itemName.contains("Corleonite")) {
                        continue;
                    }
                    try {
                        total += Integer.parseInt(matcher.group(1));
                        matched = true;
                    } catch (NumberFormatException ignored) {
                        // Malformed amount, skip this line.
                    }
                }
            }
        }

        if (matched) {
            // Log the full hover payload once so the exact Hypixel format stays verifiable.
            LOGGER.info("[Sacks] tooltips(" + seenTooltips.size() + ") >>>" + payloadDump + "<<<");
            LOGGER.info("[Sacks] parsed Corleonite +" + total);
        }
        return matched ? total : 0;
    }

    /**
     * Walks the component tree and concatenates every {@link HoverEvent.ShowText} payload.
     * Each payload is appended on its own line so item lines stay separated.
     */
    /** Collects every distinct HoverEvent.ShowText payload found in the component tree. */
    private static void collectHoverTexts(Component component, java.util.Set<String> out) {
        Style style = component.getStyle();
        HoverEvent hover = style.getHoverEvent();
        if (hover instanceof HoverEvent.ShowText showText) {
            String text = showText.value().getString();
            if (!text.isBlank()) {
                out.add(text);
            }
        }

        for (Component sibling : component.getSiblings()) {
            collectHoverTexts(sibling, out);
        }
    }

}
