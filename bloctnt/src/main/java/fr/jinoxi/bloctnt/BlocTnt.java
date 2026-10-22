package fr.jinoxi.bloctnt;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public class BlocTnt implements ModInitializer {
	public static final String MOD_ID = "bloctnt";

	@Override
	public void onInitialize() {
		ServerTickEvents.END_SERVER_TICK.register(BlockTimerManager::tick);
		ServerEntityEvents.ENTITY_LOAD.register(BlockTimerManager::onEntityLoad);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> BlockTimerManager.clear());
	}
}
