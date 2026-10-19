package com.dreaming.client.module;

import com.dreaming.client.modules.*;

import java.util.ArrayList;
import java.util.List;

public final class ModuleManager {
    private static final List<Module> MODULES = new ArrayList<>();

    private ModuleManager() {}

    public static void init() {
        // Combat
        MODULES.add(new CombatModules.KillAura());
        MODULES.add(new CombatModules.TriggerBot());
        MODULES.add(new CombatModules.Reach());
        MODULES.add(new CombatModules.AutoTotem());
        MODULES.add(new CombatModules.AutoArmor());
        // Movement
        MODULES.add(new MovementModules.Sprint());
        MODULES.add(new MovementModules.Flight());
        MODULES.add(new MovementModules.Speed());
        MODULES.add(new MovementModules.Step());
        MODULES.add(new MovementModules.NoFall());
        MODULES.add(new MovementModules.Spider());
        MODULES.add(new MovementModules.Glide());
        MODULES.add(new MovementModules.Jesus());
        MODULES.add(new MovementModules.HighJump());
        MODULES.add(new MovementModules.Bhop());
        MODULES.add(new MovementModules.AirJump());
        MODULES.add(new MovementModules.AutoWalk());
        MODULES.add(new MovementModules.FastClimb());
        // Player
        MODULES.add(new PlayerModules.GodMode());
        MODULES.add(new PlayerModules.AutoRespawn());
        MODULES.add(new PlayerModules.AutoEat());
        MODULES.add(new PlayerModules.Scaffold());
        MODULES.add(new PlayerModules.ChestStealer());
        MODULES.add(new PlayerModules.AntiAFK());
        MODULES.add(new PlayerModules.AutoMine());
        // Render
        MODULES.add(new EspModule());
        MODULES.add(new StorageEspModule());
        MODULES.add(new NametagsModule());
        MODULES.add(new RenderModules.OreESP());
        MODULES.add(new RenderModules.Tracers());
        MODULES.add(new RenderModules.Hitboxes());
        MODULES.add(new RenderModules.Breadcrumbs());
        MODULES.add(new RenderModules.Fullbright());
        MODULES.add(new RenderModules.Zoom());
        MODULES.add(new RenderModules.FovChanger());
        // World
        MODULES.add(new WorldModules.TimeChanger());
        MODULES.add(new WorldModules.NoWeather());
        MODULES.add(new WorldModules.AutoTool());
        MODULES.add(new WorldModules.Nuker());
        // HUD
        MODULES.add(new HudModules.Info());
        MODULES.add(new HudModules.Keystrokes());
        MODULES.add(new HudModules.Potions());
        MODULES.add(new HudModules.ArmorHud());
        MODULES.add(new HudModules.Radar());
        // Client
        MODULES.add(new GuiSettingsModule());
    }

    public static List<Module> all() { return MODULES; }

    public static List<Module> in(Category c) {
        List<Module> out = new ArrayList<>();
        for (Module m : MODULES) if (m.category == c) out.add(m);
        return out;
    }
}
