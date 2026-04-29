package com.elthisboy.mapupgrades;

import net.minecraft.block.BlockState;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ShovelItem;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public final class UpgradeManager {
    public static final String MOD_ID          = "mapupgrades";
    public static final String MONEY_OBJECTIVE = "money";

    private static final Identifier WALK_MODIFIER_ID   = Identifier.of(MOD_ID, "walk_speed_upgrade");
    private static final Identifier HEALTH_MODIFIER_ID = Identifier.of(MOD_ID, "max_health_upgrade");

    private UpgradeManager() {}

    public static void applyPersistentUpgrades(ServerPlayerEntity player) {
        UpgradeDataHolder holder = (UpgradeDataHolder) player;
        UpgradeConfig cfg = UpgradeConfig.getInstance();

        EntityAttributeInstance movement = player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (movement != null) {
            movement.removeModifier(WALK_MODIFIER_ID);
            int level = holder.mapupgrades$getUpgradeLevel(UpgradeType.WALK_SPEED);
            if (level > 0) {
                double valuePerLevel = cfg.get(UpgradeType.WALK_SPEED).valuePerLevel;
                movement.addPersistentModifier(new EntityAttributeModifier(
                        WALK_MODIFIER_ID,
                        level * valuePerLevel,
                        EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE
                ));
            }
        }

        EntityAttributeInstance maxHealth = player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.removeModifier(HEALTH_MODIFIER_ID);
            int level = holder.mapupgrades$getUpgradeLevel(UpgradeType.MAX_HEALTH);
            if (level > 0) {
                double valuePerLevel = cfg.get(UpgradeType.MAX_HEALTH).valuePerLevel;
                maxHealth.addPersistentModifier(new EntityAttributeModifier(
                        HEALTH_MODIFIER_ID,
                        level * valuePerLevel,
                        EntityAttributeModifier.Operation.ADD_VALUE
                ));
            }
        }

        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    public static float getBreakSpeedMultiplier(PlayerEntity player, BlockState state) {
        UpgradeDataHolder holder = (UpgradeDataHolder) player;
        UpgradeConfig cfg = UpgradeConfig.getInstance();
        ItemStack held = player.getMainHandStack();

        // ── MADERA: cualquier hacha sobre cualquier bloque de madera/derivados ──
        // Incluye: troncos, madera, stripped, hojas, tablas, bambú, etc.
        if (held.getItem() instanceof AxeItem && isWoodBlock(state)) {
            int level = holder.mapupgrades$getUpgradeLevel(UpgradeType.WOOD_SPEED);
            if (level == 0) return 1.0F;
            double valuePerLevel = cfg.get(UpgradeType.WOOD_SPEED).valuePerLevel;
            return 1.0F + (float) (level * valuePerLevel);
        }

        // ── TIERRA: cualquier pala sobre tierra y todos sus derivados ──
        // Incluye: dirt, grass, podzol, mycelium, mud, sand, gravel, soul sand, etc.
        if (held.getItem() instanceof ShovelItem && isDirtBlock(state)) {
            int level = holder.mapupgrades$getUpgradeLevel(UpgradeType.DIRT_SPEED);
            if (level == 0) return 1.0F;
            double valuePerLevel = cfg.get(UpgradeType.DIRT_SPEED).valuePerLevel;
            return 1.0F + (float) (level * valuePerLevel);
        }

        // ── PIEDRA: cualquier pico sobre piedra y todos sus derivados ──
        // Incluye: stone, cobblestone, ores, deepslate, andesite, granite,
        //          diorite, tuff, calcite, obsidian, netherrack, basalt, etc.
        if (held.getItem() instanceof PickaxeItem && isStoneBlock(state)) {
            int level = holder.mapupgrades$getUpgradeLevel(UpgradeType.STONE_SPEED);
            if (level == 0) return 1.0F;
            double valuePerLevel = cfg.get(UpgradeType.STONE_SPEED).valuePerLevel;
            return 1.0F + (float) (level * valuePerLevel);
        }

        return 1.0F;
    }

    // -------------------------------------------------------------------------
    // Block category helpers
    // -------------------------------------------------------------------------

    /**
     * Returns true for any block that logically belongs to the "wood" category:
     * logs, stripped logs, wood blocks, stripped wood, leaves, planks, bamboo,
     * bamboo blocks, mushroom stems, mangrove roots, etc.
     */
    private static boolean isWoodBlock(BlockState state) {
        return state.isIn(BlockTags.LOGS)           // all log variants (oak, spruce, birch, jungle, acacia, dark oak, mangrove, cherry, pale oak, bamboo, crimson, warped)
            || state.isIn(BlockTags.LEAVES)         // all leaf types
            || state.isIn(BlockTags.PLANKS)         // all plank types
            || state.isIn(BlockTags.WOODEN_SLABS)
            || state.isIn(BlockTags.WOODEN_STAIRS)
            || state.isIn(BlockTags.WOODEN_FENCES)
            || state.isIn(BlockTags.FENCE_GATES)
            || state.isIn(BlockTags.WOODEN_DOORS)
            || state.isIn(BlockTags.WOODEN_TRAPDOORS)
            || state.isIn(BlockTags.WOODEN_PRESSURE_PLATES)
            || state.isIn(BlockTags.WOODEN_BUTTONS)
            || state.isIn(BlockTags.BAMBOO_BLOCKS)  // bamboo block & stripped bamboo block
            || matchesBlock(state,
                // Mangrove
                "minecraft:mangrove_roots",
                "minecraft:muddy_mangrove_roots",
                // Mushroom blocks
                "minecraft:brown_mushroom_block",
                "minecraft:red_mushroom_block",
                "minecraft:mushroom_stem",
                // Misc wooden structures
                "minecraft:bookshelf",
                "minecraft:chiseled_bookshelf",
                "minecraft:crafting_table",
                "minecraft:fletching_table",
                "minecraft:cartography_table",
                "minecraft:smithing_table",
                "minecraft:loom",
                "minecraft:barrel",
                "minecraft:chest",
                "minecraft:trapped_chest",
                "minecraft:beehive",
                "minecraft:bee_nest",
                "minecraft:composter",
                "minecraft:ladder",
                "minecraft:scaffolding",
                "minecraft:bamboo",
                "minecraft:dead_bush"
            );
    }

    /**
     * Returns true for any block that logically belongs to the "dirt/soil/loose"
     * category: dirt variants, grass, mud, sand, gravel, soul sand/soil, clay, etc.
     */
    private static boolean isDirtBlock(BlockState state) {
        return state.isIn(BlockTags.DIRT)           // dirt, grass_block, podzol, mycelium, rooted_dirt, coarse_dirt
            || state.isIn(BlockTags.SAND)           // sand, red_sand
            || state.isIn(BlockTags.SOUL_SPEED_BLOCKS) // soul sand, soul soil
            || matchesBlock(state,
                // Mud family
                "minecraft:mud",
                "minecraft:packed_mud",
                "minecraft:mud_bricks",
                "minecraft:mud_brick_slab",
                "minecraft:mud_brick_stairs",
                "minecraft:mud_brick_wall",
                // Gravel
                "minecraft:gravel",
                // Clay
                "minecraft:clay",
                // Snow / ice (shovel)
                "minecraft:snow",
                "minecraft:snow_block",
                "minecraft:powder_snow",
                // Concrete powder (pre-hardened, shovel-mineable)
                "minecraft:white_concrete_powder",
                "minecraft:orange_concrete_powder",
                "minecraft:magenta_concrete_powder",
                "minecraft:light_blue_concrete_powder",
                "minecraft:yellow_concrete_powder",
                "minecraft:lime_concrete_powder",
                "minecraft:pink_concrete_powder",
                "minecraft:gray_concrete_powder",
                "minecraft:light_gray_concrete_powder",
                "minecraft:cyan_concrete_powder",
                "minecraft:purple_concrete_powder",
                "minecraft:blue_concrete_powder",
                "minecraft:brown_concrete_powder",
                "minecraft:green_concrete_powder",
                "minecraft:red_concrete_powder",
                "minecraft:black_concrete_powder",
                // Path / farmland
                "minecraft:dirt_path",
                "minecraft:farmland"
            );
    }

    /**
     * Returns true for any block that logically belongs to the "stone/mineral"
     * category: stone variants, ores, deepslate, nether and end stone materials,
     * metals (pickaxe-mineable hardened blocks), etc.
     */
    private static boolean isStoneBlock(BlockState state) {
        return state.isIn(BlockTags.STONE_ORE_REPLACEABLES)  // stone, deepslate (ore generation targets)
            || state.isIn(BlockTags.BASE_STONE_OVERWORLD)    // stone, granite, diorite, andesite, deepslate, tuff
            || state.isIn(BlockTags.BASE_STONE_NETHER)       // netherrack, basalt, blackstone
            || state.isIn(BlockTags.COAL_ORES)
            || state.isIn(BlockTags.IRON_ORES)
            || state.isIn(BlockTags.COPPER_ORES)
            || state.isIn(BlockTags.GOLD_ORES)
            || state.isIn(BlockTags.REDSTONE_ORES)
            || state.isIn(BlockTags.LAPIS_ORES)
            || state.isIn(BlockTags.DIAMOND_ORES)
            || state.isIn(BlockTags.EMERALD_ORES)
            || state.isIn(BlockTags.NEEDS_STONE_TOOL)        // iron/copper blocks, etc.
            || matchesBlock(state,
                // Cobblestone family
                "minecraft:cobblestone",
                "minecraft:mossy_cobblestone",
                "minecraft:cobblestone_slab",
                "minecraft:cobblestone_stairs",
                "minecraft:cobblestone_wall",
                "minecraft:mossy_cobblestone_slab",
                "minecraft:mossy_cobblestone_stairs",
                "minecraft:mossy_cobblestone_wall",
                // Stone bricks
                "minecraft:stone_bricks",
                "minecraft:mossy_stone_bricks",
                "minecraft:cracked_stone_bricks",
                "minecraft:chiseled_stone_bricks",
                "minecraft:stone_brick_slab",
                "minecraft:stone_brick_stairs",
                "minecraft:stone_brick_wall",
                "minecraft:mossy_stone_brick_slab",
                "minecraft:mossy_stone_brick_stairs",
                "minecraft:mossy_stone_brick_wall",
                // Deepslate
                "minecraft:deepslate",
                "minecraft:cobbled_deepslate",
                "minecraft:polished_deepslate",
                "minecraft:deepslate_bricks",
                "minecraft:deepslate_tiles",
                "minecraft:chiseled_deepslate",
                "minecraft:cracked_deepslate_bricks",
                "minecraft:cracked_deepslate_tiles",
                "minecraft:cobbled_deepslate_slab",
                "minecraft:cobbled_deepslate_stairs",
                "minecraft:cobbled_deepslate_wall",
                // Andesite / Granite / Diorite polished
                "minecraft:polished_andesite",
                "minecraft:polished_andesite_slab",
                "minecraft:polished_andesite_stairs",
                "minecraft:polished_granite",
                "minecraft:polished_granite_slab",
                "minecraft:polished_granite_stairs",
                "minecraft:polished_diorite",
                "minecraft:polished_diorite_slab",
                "minecraft:polished_diorite_stairs",
                // Tuff / Calcite
                "minecraft:tuff",
                "minecraft:tuff_bricks",
                "minecraft:polished_tuff",
                "minecraft:chiseled_tuff",
                "minecraft:chiseled_tuff_bricks",
                "minecraft:calcite",
                // Obsidian
                "minecraft:obsidian",
                "minecraft:crying_obsidian",
                "minecraft:reinforced_deepslate",
                // Nether
                "minecraft:netherrack",
                "minecraft:nether_bricks",
                "minecraft:cracked_nether_bricks",
                "minecraft:chiseled_nether_bricks",
                "minecraft:nether_brick_slab",
                "minecraft:nether_brick_stairs",
                "minecraft:nether_brick_fence",
                "minecraft:nether_brick_wall",
                "minecraft:basalt",
                "minecraft:polished_basalt",
                "minecraft:smooth_basalt",
                "minecraft:blackstone",
                "minecraft:polished_blackstone",
                "minecraft:polished_blackstone_bricks",
                "minecraft:chiseled_polished_blackstone",
                "minecraft:cracked_polished_blackstone_bricks",
                "minecraft:gilded_blackstone",
                "minecraft:nether_quartz_ore",
                "minecraft:nether_gold_ore",
                "minecraft:ancient_debris",
                // End
                "minecraft:end_stone",
                "minecraft:end_stone_bricks",
                "minecraft:end_stone_brick_slab",
                "minecraft:end_stone_brick_stairs",
                "minecraft:end_stone_brick_wall",
                "minecraft:purpur_block",
                "minecraft:purpur_pillar",
                "minecraft:purpur_slab",
                "minecraft:purpur_stairs",
                // Misc mineral/stone
                "minecraft:magma_block",
                "minecraft:glowstone",
                "minecraft:sea_lantern",
                "minecraft:amethyst_block",
                "minecraft:budding_amethyst",
                "minecraft:raw_iron_block",
                "minecraft:raw_copper_block",
                "minecraft:raw_gold_block",
                "minecraft:iron_block",
                "minecraft:gold_block",
                "minecraft:diamond_block",
                "minecraft:emerald_block",
                "minecraft:lapis_block",
                "minecraft:redstone_block",
                "minecraft:copper_block",
                "minecraft:exposed_copper",
                "minecraft:weathered_copper",
                "minecraft:oxidized_copper",
                "minecraft:cut_copper",
                "minecraft:cut_copper_slab",
                "minecraft:cut_copper_stairs",
                "minecraft:waxed_copper_block",
                "minecraft:smooth_stone",
                "minecraft:smooth_stone_slab",
                "minecraft:smooth_sandstone",
                "minecraft:smooth_sandstone_slab",
                "minecraft:smooth_sandstone_stairs",
                "minecraft:smooth_red_sandstone",
                "minecraft:smooth_red_sandstone_slab",
                "minecraft:smooth_red_sandstone_stairs",
                "minecraft:sandstone",
                "minecraft:chiseled_sandstone",
                "minecraft:cut_sandstone",
                "minecraft:sandstone_slab",
                "minecraft:sandstone_stairs",
                "minecraft:sandstone_wall",
                "minecraft:red_sandstone",
                "minecraft:chiseled_red_sandstone",
                "minecraft:cut_red_sandstone",
                "minecraft:red_sandstone_slab",
                "minecraft:red_sandstone_stairs",
                "minecraft:red_sandstone_wall",
                "minecraft:prismarine",
                "minecraft:prismarine_bricks",
                "minecraft:dark_prismarine",
                "minecraft:prismarine_slab",
                "minecraft:prismarine_brick_slab",
                "minecraft:dark_prismarine_slab",
                "minecraft:prismarine_stairs",
                "minecraft:prismarine_brick_stairs",
                "minecraft:dark_prismarine_stairs",
                "minecraft:prismarine_wall",
                "minecraft:bricks",
                "minecraft:brick_slab",
                "minecraft:brick_stairs",
                "minecraft:brick_wall",
                "minecraft:terracotta",
                "minecraft:white_terracotta",
                "minecraft:orange_terracotta",
                "minecraft:magenta_terracotta",
                "minecraft:light_blue_terracotta",
                "minecraft:yellow_terracotta",
                "minecraft:lime_terracotta",
                "minecraft:pink_terracotta",
                "minecraft:gray_terracotta",
                "minecraft:light_gray_terracotta",
                "minecraft:cyan_terracotta",
                "minecraft:purple_terracotta",
                "minecraft:blue_terracotta",
                "minecraft:brown_terracotta",
                "minecraft:green_terracotta",
                "minecraft:red_terracotta",
                "minecraft:black_terracotta",
                "minecraft:white_glazed_terracotta",
                "minecraft:orange_glazed_terracotta",
                "minecraft:magenta_glazed_terracotta",
                "minecraft:light_blue_glazed_terracotta",
                "minecraft:yellow_glazed_terracotta",
                "minecraft:lime_glazed_terracotta",
                "minecraft:pink_glazed_terracotta",
                "minecraft:gray_glazed_terracotta",
                "minecraft:light_gray_glazed_terracotta",
                "minecraft:cyan_glazed_terracotta",
                "minecraft:purple_glazed_terracotta",
                "minecraft:blue_glazed_terracotta",
                "minecraft:brown_glazed_terracotta",
                "minecraft:green_glazed_terracotta",
                "minecraft:red_glazed_terracotta",
                "minecraft:black_glazed_terracotta",
                // Concrete (hardened)
                "minecraft:white_concrete",
                "minecraft:orange_concrete",
                "minecraft:magenta_concrete",
                "minecraft:light_blue_concrete",
                "minecraft:yellow_concrete",
                "minecraft:lime_concrete",
                "minecraft:pink_concrete",
                "minecraft:gray_concrete",
                "minecraft:light_gray_concrete",
                "minecraft:cyan_concrete",
                "minecraft:purple_concrete",
                "minecraft:blue_concrete",
                "minecraft:brown_concrete",
                "minecraft:green_concrete",
                "minecraft:red_concrete",
                "minecraft:black_concrete"
            );
    }

    /**
     * Checks if the block's registry name matches any of the given identifiers.
     */
    private static boolean matchesBlock(BlockState state, String... ids) {
        String blockId = net.minecraft.registry.Registries.BLOCK.getId(state.getBlock()).toString();
        for (String id : ids) {
            if (blockId.equals(id)) return true;
        }
        return false;
    }
}
