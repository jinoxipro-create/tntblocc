package fr.jinoxi.bloctnt;

import fr.jinoxi.bloctnt.mixin.DisplayInvoker;
import fr.jinoxi.bloctnt.mixin.TextDisplayInvoker;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class BlockTimerManager {

	/** Duree avant l'explosion : 15 secondes. */
	public static final int DELAY_TICKS = 15 * 20;
	/** Puissance d'explosion identique a une TNT. */
	public static final float EXPLOSION_POWER = 4.0f;
	/** Hauteur du texte au-dessus du bas du bloc. */
	private static final double TEXT_HEIGHT = 1.3;
	private static final String DISPLAY_TAG = "bloctnt_timer";

	private static final List<Timer> TIMERS = new ArrayList<>();
	private static final Set<UUID> DISPLAYS = new HashSet<>();

	private static final class Timer {
		final ResourceKey<Level> dimension;
		final BlockPos pos;
		final Block block;
		final UUID displayId;
		int ticksLeft = DELAY_TICKS;
		int lastShown = -1;

		Timer(ResourceKey<Level> dimension, BlockPos pos, Block block, UUID displayId) {
			this.dimension = dimension;
			this.pos = pos;
			this.block = block;
			this.displayId = displayId;
		}
	}

	private BlockTimerManager() {}

	/** Appele quand un bloc vient d'etre pose. */
	public static void start(ServerLevel level, BlockPos rawPos) {
		BlockPos pos = rawPos.immutable();
		if (level.getBlockState(pos).isAir()) return;

		for (Timer t : TIMERS) {
			if (t.dimension.equals(level.dimension()) && t.pos.equals(pos)) return;
		}

		Display.TextDisplay display = new Display.TextDisplay(EntityType.TEXT_DISPLAY, level);
		display.setPos(pos.getX() + 0.5, pos.getY() + TEXT_HEIGHT, pos.getZ() + 0.5);
		display.addTag(DISPLAY_TAG);
		((DisplayInvoker) display).bloctnt$setBillboardConstraints(Display.BillboardConstraints.CENTER);
		((TextDisplayInvoker) display).bloctnt$setText(timerText(DELAY_TICKS / 20));

		// On enregistre l'UUID avant le spawn pour que le nettoyage ne le supprime pas
		DISPLAYS.add(display.getUUID());
		level.addFreshEntity(display);

		TIMERS.add(new Timer(level.dimension(), pos, level.getBlockState(pos).getBlock(), display.getUUID()));
	}

	public static void tick(MinecraftServer server) {
		Iterator<Timer> it = TIMERS.iterator();
		while (it.hasNext()) {
			Timer t = it.next();
			ServerLevel level = server.getLevel(t.dimension);
			if (level == null) {
				DISPLAYS.remove(t.displayId);
				it.remove();
				continue;
			}

			// Chunk decharge : le timer est mis en pause
			if (!level.hasChunk(t.pos.getX() >> 4, t.pos.getZ() >> 4)) continue;

			// Bloc casse ou remplace : on annule
			if (!level.getBlockState(t.pos).is(t.block)) {
				removeDisplay(level, t);
				it.remove();
				continue;
			}

			t.ticksLeft--;

			int seconds = (t.ticksLeft + 19) / 20;
			if (seconds != t.lastShown) {
				t.lastShown = seconds;
				Entity e = level.getEntity(t.displayId);
				if (e instanceof Display.TextDisplay display) {
					((TextDisplayInvoker) display).bloctnt$setText(timerText(seconds));
				}
			}

			if (t.ticksLeft <= 0) {
				removeDisplay(level, t);
				it.remove();
				explode(level, t.pos);
			}
		}
	}

	private static void explode(ServerLevel level, BlockPos pos) {
		level.removeBlock(pos, false);
		level.explode(
				null,
				pos.getX() + 0.5, pos.getY() + 0.0625, pos.getZ() + 0.5,
				EXPLOSION_POWER,
				Level.ExplosionInteraction.TNT
		);
	}

	private static void removeDisplay(ServerLevel level, Timer t) {
		Entity e = level.getEntity(t.displayId);
		if (e != null) e.discard();
		DISPLAYS.remove(t.displayId);
	}

	private static Component timerText(int seconds) {
		ChatFormatting color = seconds <= 5 ? ChatFormatting.RED : ChatFormatting.YELLOW;
		return Component.literal(seconds + "s").withStyle(color, ChatFormatting.BOLD);
	}

	/** Supprime les textes orphelins (ex. apres un redemarrage du serveur). */
	public static void onEntityLoad(Entity entity, ServerLevel level) {
		if (entity.getTags().contains(DISPLAY_TAG) && !DISPLAYS.contains(entity.getUUID())) {
			level.getServer().execute(entity::discard);
		}
	}

	public static void clear() {
		TIMERS.clear();
		DISPLAYS.clear();
	}
}
