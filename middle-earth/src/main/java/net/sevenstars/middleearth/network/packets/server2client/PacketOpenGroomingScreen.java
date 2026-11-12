package net.sevenstars.middleearth.network.packets.server2client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.sevenstars.api.network.contexts.ClientPacketContext;
import net.sevenstars.api.network.packets.ServerToClientPacket;
import net.sevenstars.middleearth.MiddleEarth;
import net.sevenstars.middleearth.gui.grooming.GroomingScreen;

/** Opens the beard screen of the Dwarven Grooming Kit (the server has checked the player is a dwarf). */
public class PacketOpenGroomingScreen extends ServerToClientPacket<PacketOpenGroomingScreen> {
    public static final Id<PacketOpenGroomingScreen> ID = new Id<>(MiddleEarth.id("packet_open_grooming_screen"));
    /** The packet carries nothing, so there is just the one (a unit codec only encodes this very instance). */
    public static final PacketOpenGroomingScreen INSTANCE = new PacketOpenGroomingScreen();
    public static final PacketCodec<RegistryByteBuf, PacketOpenGroomingScreen> CODEC = PacketCodec.unit(INSTANCE);

    private PacketOpenGroomingScreen() {
    }

    @Override
    public Id<PacketOpenGroomingScreen> getId() {
        return ID;
    }

    @Override
    public PacketCodec<RegistryByteBuf, PacketOpenGroomingScreen> streamCodec() {
        return CODEC;
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void process(ClientPacketContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        client.execute(() -> client.setScreen(new GroomingScreen()));
    }
}
