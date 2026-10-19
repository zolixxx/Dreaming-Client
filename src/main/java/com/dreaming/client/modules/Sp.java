package com.dreaming.client.modules;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;
import java.util.function.Consumer;

/** Singleplayer helpers: run code on the integrated server's copy of the player, and release held keys. */
public final class Sp {
    private Sp() {}

    public static void run(Consumer<ServerPlayerEntity> action) {
        MinecraftClient mc = MinecraftClient.getInstance();
        MinecraftServer server = mc.getServer();
        if (server == null || mc.player == null) return;
        UUID id = mc.player.getUuid();
        server.execute(() -> {
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(id);
            if (p != null) action.accept(p);
        });
    }

    /** Un-press a key binding unless the user is physically holding it. */
    public static void release(KeyBinding key) {
        MinecraftClient mc = MinecraftClient.getInstance();
        InputUtil.Key bound = KeyBindingHelper.getBoundKeyOf(key);
        boolean held = bound.getCategory() == InputUtil.Type.KEYSYM
                && InputUtil.isKeyPressed(mc.getWindow().getHandle(), bound.getCode());
        key.setPressed(held);
    }

    public static double dist2(net.minecraft.util.math.BlockPos p, net.minecraft.util.math.Vec3d v) {
        double dx = p.getX() + 0.5 - v.x, dy = p.getY() + 0.5 - v.y, dz = p.getZ() + 0.5 - v.z;
        return dx * dx + dy * dy + dz * dz;
    }
}
