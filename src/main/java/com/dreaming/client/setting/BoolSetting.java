package com.dreaming.client.setting;

public class BoolSetting extends Setting {
    public boolean value;
    public BoolSetting(String name, boolean def) { super(name); this.value = def; }
}
