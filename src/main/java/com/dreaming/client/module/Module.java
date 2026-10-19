package com.dreaming.client.module;

import com.dreaming.client.setting.Setting;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {
    protected static final MinecraftClient mc = MinecraftClient.getInstance();

    public final String name;
    public final String description;
    public final Category category;
    public final List<Setting> settings = new ArrayList<>();

    public boolean enabled;
    public boolean settingsOnly; // pure settings card: no toggle, always expanded, hidden from HUD list
    public boolean expanded;   // settings dropdown in the GUI
    public int key = -1;       // GLFW key code, -1 = unbound

    protected Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    protected <T extends Setting> T add(T setting) {
        settings.add(setting);
        return setting;
    }

    public void toggle() {
        enabled = !enabled;
        if (enabled) onEnable(); else onDisable();
    }

    public void onEnable() {}
    public void onDisable() {}
    public void onTick() {}
    public void onWorldRender(WorldRenderContext ctx) {}
    public void onHud(DrawContext ctx) {}
}
