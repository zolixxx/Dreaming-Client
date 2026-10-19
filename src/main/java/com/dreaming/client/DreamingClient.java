package com.dreaming.client;

import com.dreaming.client.hud.Hud;
import com.dreaming.client.gui.ClickGui;
import com.dreaming.client.module.Module;
import com.dreaming.client.module.ModuleManager;
import com.dreaming.client.render.RenderUtil;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class DreamingClient implements ClientModInitializer {
    public static final String NAME = "Dreaming Client";
    public static final String VERSION = "0.1.0";
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private static KeyBinding guiKey;
    private static final boolean[] keyDown = new boolean[512];

    /** Safety gate: every module only runs in a singleplayer world. */
    public static boolean allowed() {
        return mc.world != null && mc.player != null && mc.isInSingleplayer();
    }

    @Override
    public void onInitializeClient() {
        ModuleManager.init();

        guiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.dreamingclient.gui", InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT, "category.dreamingclient"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (guiKey.wasPressed()) {
                if (client.currentScreen == null) client.setScreen(new ClickGui());
            }
            if (!allowed()) return;

            for (Module m : ModuleManager.all()) {
                if (m.key > 0 && m.key < keyDown.length && client.currentScreen == null) {
                    boolean down = InputUtil.isKeyPressed(client.getWindow().getHandle(), m.key);
                    if (down && !keyDown[m.key]) m.toggle();
                    keyDown[m.key] = down;
                }
                if (m.enabled) m.onTick();
            }
        });

        WorldRenderEvents.LAST.register(ctx -> {
            if (!allowed()) return;
            for (Module m : ModuleManager.all()) {
                if (m.enabled) m.onWorldRender(ctx);
            }
            RenderUtil.flush(ctx);
        });

        HudRenderCallback.EVENT.register(Hud::render);
    }
}
