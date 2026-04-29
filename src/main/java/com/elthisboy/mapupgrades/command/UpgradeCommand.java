package com.elthisboy.mapupgrades.command;

import com.elthisboy.mapupgrades.UpgradeConfig;
import com.elthisboy.mapupgrades.UpgradeDataHolder;
import com.elthisboy.mapupgrades.UpgradeManager;
import com.elthisboy.mapupgrades.UpgradeType;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.scoreboard.ScoreAccess;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public final class UpgradeCommand {
    private UpgradeCommand() {}

    public static void register(
            CommandDispatcher<ServerCommandSource> dispatcher,
            CommandRegistryAccess registryAccess,
            CommandManager.RegistrationEnvironment environment
    ) {
        dispatcher.register(CommandManager.literal("upgrade")

            // /upgrade reload  (op only)
            .then(CommandManager.literal("reload")
                .requires(src -> src.hasPermissionLevel(2))
                .executes(ctx -> {
                    UpgradeConfig.load();
                    ctx.getSource().sendFeedback(
                            () -> Text.translatable("command.mapupgrades.reload.success"), true);
                    return 1;
                }))

            // /upgrade info [<targets>]
            .then(CommandManager.literal("info")
                .then(CommandManager.argument("targets", EntityArgumentType.players())
                    .requires(src -> src.hasPermissionLevel(2))
                    .executes(ctx -> {
                        int count = 0;
                        for (ServerPlayerEntity p : EntityArgumentType.getPlayers(ctx, "targets")) {
                            info(p);
                            count++;
                        }
                        return count;
                    }))
                .executes(ctx -> info(ctx.getSource().getPlayerOrThrow())))

            // /upgrade buy <targets> <type> [<cost>]
            .then(CommandManager.literal("buy")
                .requires(src -> src.hasPermissionLevel(2))
                .then(CommandManager.argument("targets", EntityArgumentType.players())
                    .then(CommandManager.argument("type", StringArgumentType.word())
                        .suggests((ctx, builder) -> suggestUpgradeTypes(builder))
                        .then(CommandManager.argument("cost", IntegerArgumentType.integer(1))
                            .executes(ctx -> {
                                UpgradeType type = parseType(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "type"));
                                if (type == null) return 0;
                                int cost = IntegerArgumentType.getInteger(ctx, "cost");
                                int count = 0;
                                for (ServerPlayerEntity p : EntityArgumentType.getPlayers(ctx, "targets")) {
                                    count += buy(p, type, cost);
                                }
                                return count;
                            }))
                        .executes(ctx -> {
                            UpgradeType type = parseType(ctx.getSource(),
                                    StringArgumentType.getString(ctx, "type"));
                            if (type == null) return 0;
                            int count = 0;
                            for (ServerPlayerEntity p : EntityArgumentType.getPlayers(ctx, "targets")) {
                                count += buyWithConfigPrice(p, type);
                            }
                            return count;
                        }))))

            // /upgrade add <targets> <type> <amount>
            .then(CommandManager.literal("add")
                .requires(src -> src.hasPermissionLevel(2))
                .then(CommandManager.argument("targets", EntityArgumentType.players())
                    .then(CommandManager.argument("type", StringArgumentType.word())
                        .suggests((ctx, builder) -> suggestUpgradeTypes(builder))
                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
                            .executes(ctx -> {
                                UpgradeType type = parseType(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "type"));
                                if (type == null) return 0;
                                int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                for (ServerPlayerEntity p : EntityArgumentType.getPlayers(ctx, "targets")) {
                                    add(p, type, amount);
                                }
                                return 1;
                            })))))

            // /upgrade set <targets> <type> <level>
            .then(CommandManager.literal("set")
                .requires(src -> src.hasPermissionLevel(2))
                .then(CommandManager.argument("targets", EntityArgumentType.players())
                    .then(CommandManager.argument("type", StringArgumentType.word())
                        .suggests((ctx, builder) -> suggestUpgradeTypes(builder))
                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(0))
                            .executes(ctx -> {
                                UpgradeType type = parseType(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "type"));
                                if (type == null) return 0;
                                int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                for (ServerPlayerEntity p : EntityArgumentType.getPlayers(ctx, "targets")) {
                                    set(p, type, amount);
                                }
                                return 1;
                            })))))

            // /upgrade reset <targets> [<type>]
            .then(CommandManager.literal("reset")
                .requires(src -> src.hasPermissionLevel(2))
                .then(CommandManager.argument("targets", EntityArgumentType.players())
                    .then(CommandManager.argument("type", StringArgumentType.word())
                        .suggests((ctx, builder) -> suggestUpgradeTypes(builder))
                        .executes(ctx -> {
                            UpgradeType type = parseType(ctx.getSource(),
                                    StringArgumentType.getString(ctx, "type"));
                            if (type == null) return 0;
                            for (ServerPlayerEntity p : EntityArgumentType.getPlayers(ctx, "targets")) {
                                set(p, type, 0);
                            }
                            return 1;
                        }))
                    .executes(ctx -> {
                        for (ServerPlayerEntity p : EntityArgumentType.getPlayers(ctx, "targets")) {
                            resetAll(p);
                        }
                        return 1;
                    })))
        );
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static CompletableFuture<Suggestions> suggestUpgradeTypes(SuggestionsBuilder builder) {
        for (UpgradeType type : UpgradeType.values()) {
            builder.suggest(type.asString());
        }
        return builder.buildFuture();
    }

    private static UpgradeType parseType(ServerCommandSource source, String input) {
        try {
            return UpgradeType.valueOf(input.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {}

        for (UpgradeType type : UpgradeType.values()) {
            if (type.asString().equalsIgnoreCase(input)) return type;
        }

        source.sendError(Text.translatable("error.mapupgrades.unknown_type", input));
        return null;
    }

    /** Sends a message in the action bar (overlay) instead of the chat. */
    private static void actionBar(ServerPlayerEntity player, Text text) {
        player.sendMessage(text, true);
    }

    /** Plays a success sound (level-up chime). */
    private static void playSuccess(ServerPlayerEntity player) {
        player.playSoundToPlayer(
                SoundEvents.ENTITY_PLAYER_LEVELUP,
                SoundCategory.PLAYERS,
                0.5f, 1.8f
        );
    }

    /** Plays an error sound. */
    private static void playError(ServerPlayerEntity player) {
        player.playSoundToPlayer(
                SoundEvents.ENTITY_VILLAGER_NO,
                SoundCategory.PLAYERS,
                0.6f, 1.0f
        );
    }

    /**
     * Spawns a burst of particles around the player on a successful upgrade.
     * – Inner cluster: HAPPY_VILLAGER (green sparkles) at torso height
     * – Outer burst:   TOTEM_OF_UNDYING (golden rays) for a visible "level-up" pop
     */
    private static void spawnUpgradeParticles(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        double x = player.getX();
        double y = player.getY() + 1.0;
        double z = player.getZ();

        world.spawnParticles(
                ParticleTypes.HAPPY_VILLAGER,
                x, y, z,
                25, 0.4, 0.5, 0.4, 0.1
        );
        world.spawnParticles(
                ParticleTypes.TOTEM_OF_UNDYING,
                x, y + 0.5, z,
                12, 0.6, 0.8, 0.6, 0.3
        );
    }

    // -------------------------------------------------------------------------
    // Sub-command implementations
    // -------------------------------------------------------------------------

    private static int info(ServerPlayerEntity player) {
        UpgradeDataHolder holder = (UpgradeDataHolder) player;
        player.sendMessage(Text.translatable("command.mapupgrades.info.header"), false);

        for (UpgradeType type : UpgradeType.values()) {
            int currentLevel = holder.mapupgrades$getUpgradeLevel(type);
            UpgradeConfig.UpgradeSettings cfg = UpgradeConfig.getInstance().get(type);
            String maxDisplay = (cfg.maxLevel > 0) ? String.valueOf(cfg.maxLevel) : "\u221e";
            boolean atMax = cfg.maxLevel > 0 && currentLevel >= cfg.maxLevel;

            if (atMax) {
                player.sendMessage(Text.translatable(
                        "command.mapupgrades.info.line_maxed",
                        type.getDisplayName(), currentLevel, maxDisplay
                ), false);
            } else {
                player.sendMessage(Text.translatable(
                        "command.mapupgrades.info.line",
                        type.getDisplayName(), currentLevel, maxDisplay,
                        cfg.costForNextLevel(currentLevel)
                ), false);
            }
        }
        return 1;
    }

    private static int buyWithConfigPrice(ServerPlayerEntity player, UpgradeType type) {
        UpgradeDataHolder holder = (UpgradeDataHolder) player;
        UpgradeConfig.UpgradeSettings cfg = UpgradeConfig.getInstance().get(type);
        int currentLevel = holder.mapupgrades$getUpgradeLevel(type);

        if (cfg.maxLevel > 0 && currentLevel >= cfg.maxLevel) {
            actionBar(player, Text.translatable("error.mapupgrades.max_level_reached",
                    type.getDisplayName(), cfg.maxLevel));
            playError(player);
            return 0;
        }

        return buy(player, type, cfg.costForNextLevel(currentLevel));
    }

    private static int buy(ServerPlayerEntity player, UpgradeType type, int cost) {
        Scoreboard scoreboard = player.getServer().getScoreboard();
        ScoreboardObjective objective = scoreboard.getNullableObjective(UpgradeManager.MONEY_OBJECTIVE);
        if (objective == null) {
            actionBar(player, Text.translatable("error.mapupgrades.no_money_objective"));
            playError(player);
            return 0;
        }

        UpgradeDataHolder holder = (UpgradeDataHolder) player;
        UpgradeConfig.UpgradeSettings cfg = UpgradeConfig.getInstance().get(type);
        int currentLevel = holder.mapupgrades$getUpgradeLevel(type);

        if (cfg.maxLevel > 0 && currentLevel >= cfg.maxLevel) {
            actionBar(player, Text.translatable("error.mapupgrades.max_level_reached",
                    type.getDisplayName(), cfg.maxLevel));
            playError(player);
            return 0;
        }

        ScoreAccess access = scoreboard.getOrCreateScore(player, objective);
        int balance = access.getScore();
        if (balance < cost) {
            actionBar(player, Text.translatable("error.mapupgrades.not_enough_money", cost, balance));
            playError(player);
            return 0;
        }

        access.setScore(balance - cost);
        holder.mapupgrades$addUpgradeLevel(type, 1);
        UpgradeManager.applyPersistentUpgrades(player);

        actionBar(player, Text.translatable("command.mapupgrades.buy.success",
                type.getDisplayName(), cost));
        playSuccess(player);
        spawnUpgradeParticles(player);
        return 1;
    }

    private static void add(ServerPlayerEntity player, UpgradeType type, int amount) {
        UpgradeDataHolder holder = (UpgradeDataHolder) player;
        holder.mapupgrades$addUpgradeLevel(type, amount);
        UpgradeManager.applyPersistentUpgrades(player);
        actionBar(player, Text.translatable("command.mapupgrades.add.success",
                amount, type.getDisplayName()));
        playSuccess(player);
        spawnUpgradeParticles(player);
    }

    private static void set(ServerPlayerEntity player, UpgradeType type, int amount) {
        UpgradeDataHolder holder = (UpgradeDataHolder) player;
        holder.mapupgrades$setUpgradeLevel(type, amount);
        UpgradeManager.applyPersistentUpgrades(player);
        actionBar(player, Text.translatable("command.mapupgrades.set.success",
                type.getDisplayName(), amount));
        playSuccess(player);
    }

    private static void resetAll(ServerPlayerEntity player) {
        UpgradeDataHolder holder = (UpgradeDataHolder) player;
        for (UpgradeType type : UpgradeType.values()) {
            holder.mapupgrades$setUpgradeLevel(type, 0);
        }
        UpgradeManager.applyPersistentUpgrades(player);
        actionBar(player, Text.translatable("command.mapupgrades.reset.all"));
        playError(player);
    }
}
