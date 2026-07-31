package de.maexle.skyblockcounter;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.util.math.ChunkPos;
import java.util.HashSet;
import java.util.Set;
import java.util.HashSet;
import java.util.Set;

public class SkyblockCounter implements ModInitializer {
	public static final String MOD_ID = "skyblockcounter";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {

		LOGGER.info("Hello World!");
	}

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}
}
