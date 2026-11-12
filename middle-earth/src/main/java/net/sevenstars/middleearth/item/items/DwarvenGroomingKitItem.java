package net.sevenstars.middleearth.item.items;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.sevenstars.middleearth.MiddleEarth;
import net.sevenstars.middleearth.entity.beard.PlayerBeard;
import net.sevenstars.middleearth.network.packets.server2client.PacketOpenGroomingScreen;

/**
 * Comb, shears and beard rings in a leather roll: a dwarf uses it to choose their beard's style, colour and
 * ornaments (the beard screen). It is kept, not used up. Other races have no such beards to groom.
 */
public class DwarvenGroomingKitItem extends Item {
    public DwarvenGroomingKitItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }
        if (user instanceof ServerPlayerEntity player) {
            if (!PlayerBeard.canGrow(player)) {
                player.sendMessage(Text.translatable("item." + MiddleEarth.MOD_ID + ".dwarven_grooming_kit.not_dwarf"), true);
                return ActionResult.FAIL;
            }
            ServerPlayNetworking.send(player, PacketOpenGroomingScreen.INSTANCE);
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }
}
