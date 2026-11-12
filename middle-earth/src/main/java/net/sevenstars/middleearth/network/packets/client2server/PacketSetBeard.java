package net.sevenstars.middleearth.network.packets.client2server;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.sevenstars.api.network.contexts.ServerPacketContext;
import net.sevenstars.api.network.packets.ClientToServerPacket;
import net.sevenstars.middleearth.MiddleEarth;
import net.sevenstars.middleearth.entity.beard.PlayerBeard;
import net.sevenstars.middleearth.item.ResourceItemsME;

/**
 * The beard chosen on the grooming screen: an empty style shaves it off. Only a dwarf holding the Dwarven
 * Grooming Kit may change it.
 */
public class PacketSetBeard extends ClientToServerPacket<PacketSetBeard> {
    public static final Id<PacketSetBeard> ID = new Id<>(MiddleEarth.id("packet_set_beard"));
    public static final PacketCodec<RegistryByteBuf, PacketSetBeard> CODEC = PacketCodec.tuple(
        PacketCodecs.STRING, p -> p.style,
        PacketCodecs.STRING, p -> p.colour,
        PacketCodecs.STRING, p -> p.ornament,
        PacketSetBeard::new);

    private final String style;
    private final String colour;
    private final String ornament;

    public PacketSetBeard(String style, String colour, String ornament) {
        this.style = style;
        this.colour = colour;
        this.ornament = ornament;
    }

    @Override
    public Id<PacketSetBeard> getId() {
        return ID;
    }

    @Override
    public PacketCodec<RegistryByteBuf, PacketSetBeard> streamCodec() {
        return CODEC;
    }

    @Override
    public void process(ServerPacketContext context) {
        ServerPlayerEntity player = context.player();
        player.getServer().execute(() -> {
            boolean holdsKit = player.getMainHandStack().isOf(ResourceItemsME.DWARVEN_GROOMING_KIT)
                || player.getOffHandStack().isOf(ResourceItemsME.DWARVEN_GROOMING_KIT);
            if (!holdsKit || !PlayerBeard.canGrow(player)) {
                return;
            }
            if (style.isEmpty()) {
                player.removeAttached(PlayerBeard.BEARD);
            } else {
                PlayerBeard beard = new PlayerBeard(style, colour, ornament);
                if (!beard.isValid()) {
                    return;
                }
                player.setAttached(PlayerBeard.BEARD, beard);
            }
            player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_SHEEP_SHEAR,
                SoundCategory.PLAYERS, 0.6F, 1.2F);
        });
    }
}
