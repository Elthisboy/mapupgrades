package com.elthisboy.mapupgrades;

import com.elthisboy.mapupgrades.command.UpgradeCommand;
import com.elthisboy.mapupgrades.UpgradeConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Mapupgrades implements ModInitializer {
	public static final String MOD_ID = "mapupgrades";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		UpgradeConfig.load();

		if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("placeholder-api")) {
			UpgradePlaceholders.register();
			LOGGER.info("[MapUpgrades] Text Placeholder API detected – placeholders registered.");
		}

		CommandRegistrationCallback.EVENT.register(UpgradeCommand::register);

		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			Scoreboard scoreboard = server.getScoreboard();
			ScoreboardObjective objective = scoreboard.getNullableObjective(UpgradeManager.MONEY_OBJECTIVE);
			if (objective == null) {
				objective = scoreboard.addObjective(
						UpgradeManager.MONEY_OBJECTIVE,
						ScoreboardCriterion.DUMMY,
						Text.translatable("scoreboard.mapupgrades.money"),
						ScoreboardCriterion.RenderType.INTEGER,
						true,
						null
				);
			}
		});

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
				UpgradeManager.applyPersistentUpgrades(handler.player));

		ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
			((UpgradeDataHolder) newPlayer).mapupgrades$copyUpgradesFrom((UpgradeDataHolder) oldPlayer);
			UpgradeManager.applyPersistentUpgrades(newPlayer);
		});

		LOGGER.info("Map Upgrades initialized");
	}
}