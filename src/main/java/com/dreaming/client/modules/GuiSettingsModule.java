package com.dreaming.client.modules;

import com.dreaming.client.module.Category;
import com.dreaming.client.module.Module;
import com.dreaming.client.setting.BoolSetting;
import com.dreaming.client.setting.NumSetting;

public class GuiSettingsModule extends Module {
    public static GuiSettingsModule INSTANCE;

    public final NumSetting speed = add(new NumSetting("Animation speed", 1.0, 0.5, 2.5, 0.1));
    public final NumSetting hue = add(new NumSetting("Accent hue", 265, 0, 360, 5));
    public final BoolSetting particles = add(new BoolSetting("Particles", true));
    public final BoolSetting dim = add(new BoolSetting("Dim background", true));

    public GuiSettingsModule() {
        super("Interface", "Look and feel of this menu.", Category.CLIENT);
        settingsOnly = true;
        enabled = true;
        INSTANCE = this;
    }
}
