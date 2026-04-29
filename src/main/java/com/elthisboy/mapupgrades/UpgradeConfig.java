package com.elthisboy.mapupgrades;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

/**
 * Loads and holds per-upgrade configuration from config/mapupgrades.json.
 *
 * For each UpgradeType the file contains:
 *   "base_price"      – cost for level 1
 *   "price_factor"    – multiplier per level  (cost = base_price * factor^(level))
 *   "max_level"       – 0 means unlimited
 *   "value_per_level" – stat improvement per level
 *                       (speed types: ADD_MULTIPLIED_BASE added per level, e.g. 0.025 = +2.5%/level)
 *                       (max_health: flat HP added per level, e.g. 1.0 = +1 HP/level)
 */
public final class UpgradeConfig {

    public static final class UpgradeSettings {
        public final int    basePice;
        public final double priceFactor;
        public final int    maxLevel;
        public final double valuePerLevel;

        public UpgradeSettings(int basePice, double priceFactor, int maxLevel, double valuePerLevel) {
            this.basePice      = basePice;
            this.priceFactor   = priceFactor;
            this.maxLevel      = maxLevel;
            this.valuePerLevel = valuePerLevel;
        }

        public int costForNextLevel(int currentLevel) {
            return (int) Math.ceil(basePice * Math.pow(priceFactor, currentLevel));
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(Mapupgrades.MOD_ID);
    private static final Gson   GSON   = new GsonBuilder().setPrettyPrinting().create();

    private final Map<UpgradeType, UpgradeSettings> settings = new EnumMap<>(UpgradeType.class);

    private static UpgradeConfig instance;

    private UpgradeConfig() {}

    public static UpgradeConfig getInstance() {
        if (instance == null) throw new IllegalStateException("UpgradeConfig not loaded yet");
        return instance;
    }

    public static void load() {
        Path configDir  = FabricLoader.getInstance().getConfigDir();
        Path configFile = configDir.resolve("mapupgrades.json");

        UpgradeConfig cfg = new UpgradeConfig();

        if (Files.exists(configFile)) {
            try (Reader reader = Files.newBufferedReader(configFile)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                for (UpgradeType type : UpgradeType.values()) {
                    if (root.has(type.asString())) {
                        JsonObject entry = root.getAsJsonObject(type.asString());
                        int    base   = entry.has("base_price")      ? entry.get("base_price").getAsInt()         : defaults(type).basePice;
                        double factor = entry.has("price_factor")    ? entry.get("price_factor").getAsDouble()    : defaults(type).priceFactor;
                        int    max    = entry.has("max_level")       ? entry.get("max_level").getAsInt()          : defaults(type).maxLevel;
                        double value  = entry.has("value_per_level") ? entry.get("value_per_level").getAsDouble() : defaults(type).valuePerLevel;
                        cfg.settings.put(type, new UpgradeSettings(base, factor, max, value));
                    } else {
                        cfg.settings.put(type, defaults(type));
                    }
                }
                LOGGER.info("[MapUpgrades] Config loaded from {}", configFile);
            } catch (IOException e) {
                LOGGER.error("[MapUpgrades] Failed to read config, using defaults", e);
                cfg.loadDefaults();
            }
        } else {
            cfg.loadDefaults();
            cfg.save(configFile);
            LOGGER.info("[MapUpgrades] Config not found – created defaults at {}", configFile);
        }

        instance = cfg;
    }

    public UpgradeSettings get(UpgradeType type) {
        return settings.getOrDefault(type, defaults(type));
    }

    private void loadDefaults() {
        for (UpgradeType type : UpgradeType.values()) {
            settings.put(type, defaults(type));
        }
    }

    private void save(Path path) {
        JsonObject root = new JsonObject();
        for (UpgradeType type : UpgradeType.values()) {
            UpgradeSettings s = settings.get(type);
            JsonObject entry = new JsonObject();
            entry.addProperty("base_price",      s.basePice);
            entry.addProperty("price_factor",    s.priceFactor);
            entry.addProperty("max_level",       s.maxLevel);
            entry.addProperty("value_per_level", s.valuePerLevel);
            root.add(type.asString(), entry);
        }
        try (Writer writer = Files.newBufferedWriter(path)) {
            GSON.toJson(root, writer);
        } catch (IOException e) {
            LOGGER.error("[MapUpgrades] Failed to write default config", e);
        }
    }

    private static UpgradeSettings defaults(UpgradeType type) {
        return switch (type) {
            case WALK_SPEED  -> new UpgradeSettings(10, 1.5, 30, 0.025);
            case STONE_SPEED -> new UpgradeSettings(10, 1.5, 30, 0.025);
            case DIRT_SPEED  -> new UpgradeSettings(10, 1.5, 30, 0.025);
            case WOOD_SPEED  -> new UpgradeSettings(10, 1.5, 30, 0.025);
            case MAX_HEALTH  -> new UpgradeSettings(15, 1.8, 30, 1.0);
        };
    }
}
