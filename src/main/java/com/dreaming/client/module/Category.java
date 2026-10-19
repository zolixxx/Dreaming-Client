package com.dreaming.client.module;

public enum Category {
    COMBAT("Combat"),
    MOVEMENT("Movement"),
    PLAYER("Player"),
    RENDER("Render"),
    WORLD("World"),
    HUD("HUD"),
    CLIENT("Client");

    public final String label;
    Category(String label) { this.label = label; }
}
