package com.elthisboy.mapupgrades;

import net.minecraft.text.Text;
import net.minecraft.util.StringIdentifiable;

public enum UpgradeType implements StringIdentifiable {
    WALK_SPEED("walk_speed"),
    STONE_SPEED("stone_speed"),
    DIRT_SPEED("dirt_speed"),
    WOOD_SPEED("wood_speed"),
    MAX_HEALTH("max_health");

    private final String id;

    UpgradeType(String id) {
        this.id = id;
    }

    @Override
    public String asString() {
        return this.id;
    }

    public String nbtKey() {
        return switch (this) {
            case WALK_SPEED  -> "WalkSpeed";
            case STONE_SPEED -> "StoneSpeed";
            case DIRT_SPEED  -> "DirtSpeed";
            case WOOD_SPEED  -> "WoodSpeed";
            case MAX_HEALTH  -> "MaxHealth";
        };
    }

    public Text getDisplayName() {
        return Text.translatable("upgrade.mapupgrades." + this.id);
    }
}
