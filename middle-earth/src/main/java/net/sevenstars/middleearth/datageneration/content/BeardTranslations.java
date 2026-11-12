package net.sevenstars.middleearth.datageneration.content;

import net.sevenstars.middleearth.MiddleEarth;
import net.sevenstars.middleearth.entity.beard.PlayerBeard;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * English text for dwarf players' beards: the Dwarven Grooming Kit's screen and its choices, added by the
 * LanguageProvider. The kit's own name comes from its id like every other Middle-earth item.
 */
public class BeardTranslations {
    private static final String SCREEN = "screen." + MiddleEarth.MOD_ID + ".grooming";

    public static final Map<String, String> ENTRIES = new LinkedHashMap<>();

    private static void put(String key, String value) {
        ENTRIES.put(key, value);
    }

    /** "very_long" -> "Very Long". */
    private static String words(String id) {
        StringBuilder name = new StringBuilder();
        for (String word : id.split("_")) {
            if (!name.isEmpty()) {
                name.append(' ');
            }
            name.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return name.toString();
    }

    static {
        put("item." + MiddleEarth.MOD_ID + ".dwarven_grooming_kit.not_dwarf", "Only a dwarf grows a beard worth such grooming");
        put(SCREEN, "Dwarven Grooming");
        put(SCREEN + ".style", "Beard");
        put(SCREEN + ".colour", "Color");
        put(SCREEN + ".ornament", "Rings and Beads");
        put(SCREEN + ".apply", "Apply");
        put(SCREEN + ".cancel", "Cancel");
        put(SCREEN + ".turn_left", "Turn left");
        put(SCREEN + ".turn_right", "Turn right");
        put(SCREEN + ".turn_reset", "Face forward");
        put(SCREEN + ".style.none", "Shaven");
        for (String style : PlayerBeard.STYLES) {
            put(SCREEN + ".style." + style, words(style.replace("_ornamented", "")));
        }
        // (too long for the selection bar)
        put(SCREEN + ".style.very_large_mustache", "Great Mustache");
        for (String colour : PlayerBeard.COLOURS) {
            put(SCREEN + ".colour." + colour, words(colour));
        }
        for (String ornament : PlayerBeard.ORNAMENTS) {
            put(SCREEN + ".ornament." + ornament, words(ornament));
        }
    }

    private BeardTranslations() {
    }
}
