package clearlag;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ClearLagMod implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("clearlag");

	private static ClearLagConfig config;
	private static int ticksLeft;

	@Override
	public void onInitialize() {
		config = ClearLagConfig.load();
		resetTimer();

		ServerTickEvents.END_SERVER_TICK.register(ClearLagMod::onTick);
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> registerCommands(dispatcher));

		LOGGER.info("ClearLag aktif. Interval: {} detik", config.intervalSeconds);
	}

	private static void resetTimer() {
		ticksLeft = config.intervalSeconds * 20;
	}

	private static void onTick(MinecraftServer server) {
		if (!config.autoClear) return;

		ticksLeft--;
		if (ticksLeft <= 0) {
			runClear(server);
			return;
		}

		if (ticksLeft % 20 == 0) {
			int seconds = ticksLeft / 20;
			if (config.warnAtSeconds.contains(seconds)) {
				send(server, config.warnMessage.replace("%time%", String.valueOf(seconds)),
						Formatting.YELLOW, config.warnInActionBar());
			}
		}
	}

	/** Bersihkan, kirim pesan hasil, ulang timer. */
	private static int[] runClear(MinecraftServer server) {
		int[] r = clear(server);
		send(server, config.clearedMessage
				.replace("%items%", String.valueOf(r[0]))
				.replace("%xp%", String.valueOf(r[1]))
				.replace("%mobs%", String.valueOf(r[2])), Formatting.GREEN, config.resultInActionBar());
		resetTimer();
		return r;
	}

	/** @return {items, xp, mobs} */
	private static int[] clear(MinecraftServer server) {
		int items = 0, xp = 0, mobs = 0;

		for (ServerWorld world : server.getWorlds()) {
			List<Entity> toRemove = new ArrayList<>();
			for (Entity entity : world.iterateEntities()) {
				if (entity instanceof ItemEntity item) {
					if (config.clearItems && !isProtected(item)) {
						toRemove.add(entity);
						items++;
					}
				} else if (config.clearExperienceOrbs && entity instanceof ExperienceOrbEntity) {
					toRemove.add(entity);
					xp++;
				} else if (config.clearHostileMobs
						&& entity instanceof MobEntity mob
						&& mob.getType().getSpawnGroup() == SpawnGroup.MONSTER
						&& !mob.hasCustomName()
						&& !mob.isPersistent()) {
					toRemove.add(entity);
					mobs++;
				}
			}
			for (Entity e : toRemove) {
				e.discard();
			}
		}
		return new int[]{items, xp, mobs};
	}

	private static boolean isProtected(ItemEntity entity) {
		String id = Registries.ITEM.getId(entity.getStack().getItem()).toString();
		return config.protectedItems.contains(id);
	}

	/** actionBar = true -> tampil di atas hotbar, false -> chat. */
	private static void send(MinecraftServer server, String message, Formatting color, boolean actionBar) {
		server.getPlayerManager().broadcast(Text.literal(message).formatted(color), actionBar);
	}

	private static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(CommandManager.literal("clearlag")
				.requires(CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK))
				.then(CommandManager.literal("now").executes(ctx -> {
					int[] r = runClear(ctx.getSource().getServer());
					return r[0] + r[1] + r[2];
				}))
				.then(CommandManager.literal("reload").executes(ctx -> {
					config = ClearLagConfig.load();
					resetTimer();
					ctx.getSource().sendFeedback(() -> Text.literal("[ClearLag] Config di-reload."), true);
					return 1;
				}))
				.then(CommandManager.literal("status").executes(ctx -> {
					int s = ticksLeft / 20;
					ctx.getSource().sendFeedback(() -> Text.literal(
							"[ClearLag] Auto-clear: " + (config.autoClear ? "ON" : "OFF")
									+ ", pembersihan berikutnya dalam " + s + " detik."), false);
					return s;
				})));
	}
}
