package com.elthisboy.mapupgrades;

import eu.pb4.placeholders.api.PlaceholderContext;
import eu.pb4.placeholders.api.PlaceholderResult;
import eu.pb4.placeholders.api.Placeholders;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Registers Text Placeholder API placeholders for use in HoloDisplays and other compatible mods.
 *
 * Available placeholders:
 *   %mapupgrades:level <type>%      → current level of that upgrade for the player
 *   %mapupgrades:next_cost <type>%  → coin cost of the next level
 *   %mapupgrades:max_level <type>%  → configured max level (or ∞)
 *
 * Example HoloDisplays line:
 *   <yellow>⛏ Stone Speed</yellow>\n<white>Level: <gold>%mapupgrades:level stone_speed%</gold></white>\n<white>Price: <green>%mapupgrades:next_cost stone_speed% coins</green></white>
 */
public final class UpgradePlaceholders {

    private UpgradePlaceholders() {}

    public static void register() {

        // %mapupgrades:level <type>%
        // Returns the player's current level for the given upgrade type.
        Placeholders.register(Identifier.of(Mapupgrades.MOD_ID, "level"), (ctx, arg) -> {
            if (!ctx.hasPlayer())
                return PlaceholderResult.invalid("No player");
            if (arg == null || arg.isBlank())
                return PlaceholderResult.invalid("No upgrade type argument");

            UpgradeType type = parseType(arg);
            if (type == null)
                return PlaceholderResult.invalid("Unknown upgrade type: " + arg);

            UpgradeDataHolder holder = (UpgradeDataHolder) ctx.player();
            int level = holder.mapupgrades$getUpgradeLevel(type);
            return PlaceholderResult.value(Text.literal(String.valueOf(level)));
        });

        // %mapupgrades:next_cost <type>%
        // Returns the coin cost to upgrade from the player's current level to the next.
        // Shows "MAX" if the player is already at the configured maximum level.
        Placeholders.register(Identifier.of(Mapupgrades.MOD_ID, "next_cost"), (ctx, arg) -> {
            if (!ctx.hasPlayer())
                return PlaceholderResult.invalid("No player");
            if (arg == null || arg.isBlank())
                return PlaceholderResult.invalid("No upgrade type argument");

            UpgradeType type = parseType(arg);
            if (type == null)
                return PlaceholderResult.invalid("Unknown upgrade type: " + arg);

            UpgradeDataHolder holder = (UpgradeDataHolder) ctx.player();
            UpgradeConfig.UpgradeSettings cfg = UpgradeConfig.getInstance().get(type);
            int currentLevel = holder.mapupgrades$getUpgradeLevel(type);

            if (cfg.maxLevel > 0 && currentLevel >= cfg.maxLevel)
                return PlaceholderResult.value(Text.literal("MAX"));

            int cost = cfg.costForNextLevel(currentLevel);
            return PlaceholderResult.value(Text.literal(String.valueOf(cost)));
        });

        // %mapupgrades:max_level <type>%
        // Returns the configured max level for the upgrade type, or ∞ if unlimited.
        Placeholders.register(Identifier.of(Mapupgrades.MOD_ID, "max_level"), (ctx, arg) -> {
            if (arg == null || arg.isBlank())
                return PlaceholderResult.invalid("No upgrade type argument");

            UpgradeType type = parseType(arg);
            if (type == null)
                return PlaceholderResult.invalid("Unknown upgrade type: " + arg);

            UpgradeConfig.UpgradeSettings cfg = UpgradeConfig.getInstance().get(type);
            String display = cfg.maxLevel > 0 ? String.valueOf(cfg.maxLevel) : "\u221e";
            return PlaceholderResult.value(Text.literal(display));
        });
    }

    // -------------------------------------------------------------------------

    private static UpgradeType parseType(String input) {
        String normalized = input.trim();
        for (UpgradeType type : UpgradeType.values()) {
            if (type.asString().equalsIgnoreCase(normalized)) return type;
        }
        try {
            return UpgradeType.valueOf(normalized.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
