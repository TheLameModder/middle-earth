package net.sevenstars.middleearth.entity.beard;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.Identifier;
import net.sevenstars.middleearth.MiddleEarth;
import net.sevenstars.middleearth.resources.datas.texture_presets.TexturePresetDataPool;
import net.sevenstars.middleearth.registries.AtlasesME;
import net.sevenstars.middleearth.registries.content.races.RaceRegistry;
import net.sevenstars.middleearth.resources.datas.races.Race;
import net.sevenstars.middleearth.resources.persistent_datas.PlayerDataService;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A dwarf player's beard, chosen with the Dwarven Grooming Kit: one of the dwarf NPCs' beard styles, in a hair
 * colour, with its rings or beads in an ornament. Drawn from the NPCs' own beard textures (character/hairs).
 * Kept on the player (saved, kept through death, synced to everyone who can see them); none when unset.
 */
public record PlayerBeard(String style, String colour, String ornament) {
    /**
     * The NPCs' beard styles (textures/character/hairs/beard_<style>.png), those in the character atlas. (There is
     * also a beard_very_thick_ornamented texture, but no NPC wears it and it is not in the atlas yet.)
     */
    public static final List<String> STYLES = List.of("short", "clean", "single", "semi_long", "broad", "very_broad",
        "large", "very_long", "chunky_braids", "very_large_mustache", "fancy_mustache_ornamented", "dual_ornamented",
        "dual_large_ornamented", "long_single_ornamented", "long_braids_ornamented", "unclean_ornamented");
    /** The hair colours of the hair palettes (character/hairs/color_palettes). */
    public static final List<String> COLOURS = List.of("black", "cold_black", "dark_brown", "brown", "dirty_brown",
        "ginger", "red", "straw", "blonde", "dirty_blonde", "greasy", "gray", "white");
    /** The ornament metals and beads of the hair palettes. */
    public static final List<String> ORNAMENTS = List.of("gold", "silver", "copper", "almandine", "beads");
    /** The one style drawn on the face alone, with no beard hanging below the chin. */
    private static final String NO_ADDON = "short";

    public static final Codec<PlayerBeard> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.fieldOf("style").forGetter(PlayerBeard::style),
        Codec.STRING.fieldOf("colour").forGetter(PlayerBeard::colour),
        Codec.STRING.fieldOf("ornament").forGetter(PlayerBeard::ornament)
    ).apply(instance, PlayerBeard::new));
    public static final PacketCodec<ByteBuf, PlayerBeard> PACKET_CODEC = PacketCodec.tuple(
        PacketCodecs.STRING, PlayerBeard::style,
        PacketCodecs.STRING, PlayerBeard::colour,
        PacketCodecs.STRING, PlayerBeard::ornament,
        PlayerBeard::new);

    public static final AttachmentType<PlayerBeard> BEARD = AttachmentRegistry.<PlayerBeard>builder()
        .persistent(CODEC)
        .copyOnDeath()
        .syncWith(PACKET_CODEC, AttachmentSyncPredicate.all())
        .buildAndRegister(Identifier.of(MiddleEarth.MOD_ID, "beard"));

    public static void register() {
        // Loads the class, which registers the attachment.
    }

    /** Whether the style, colour and ornament are all ones there are textures for. */
    public boolean isValid() {
        return STYLES.contains(style) && COLOURS.contains(colour) && ORNAMENTS.contains(ornament);
    }

    /** The player's beard, or null for none. */
    @Nullable
    public static PlayerBeard of(PlayerEntity player) {
        PlayerBeard beard = player.getAttached(BEARD);
        return beard != null && beard.isValid() ? beard : null;
    }

    /** Only dwarves grow beards like these (server side: the race is kept there). */
    public static boolean canGrow(PlayerEntity player) {
        Race race = PlayerDataService.getPlayerRace(player, player.getWorld());
        return race != null && RaceRegistry.DWARF.getValue().equals(race.getId());
    }

    /** The beard on the face and chin, drawn over the head like the NPCs' (a character atlas sprite). */
    public Identifier faceTexture() {
        return MiddleEarth.appendPrefix(TexturePresetDataPool.buildId(MiddleEarth.id("beard_" + style), material()),
            AtlasesME.HAIR_PREFIX);
    }

    /** The beard hanging below the chin over the chest (the hair model's), or null for a style without one. */
    @Nullable
    public Identifier hangingTexture() {
        if (NO_ADDON.equals(style)) {
            return null;
        }
        return MiddleEarth.appendPrefix(TexturePresetDataPool.buildAddonId(MiddleEarth.id("beard_" + style), material()),
            AtlasesME.HAIR_PREFIX);
    }

    private Identifier material() {
        return MiddleEarth.id(colour + "_and_" + ornament);
    }
}
