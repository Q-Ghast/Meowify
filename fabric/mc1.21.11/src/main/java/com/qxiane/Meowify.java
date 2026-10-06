package com.qxiane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;

public class Meowify implements ModInitializer {

	public static final String MOD_ID = "meowify";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// Meowify is client-side only; the mod id and logger are shared with the client entrypoint,
		// which is where everything that needs a client actually lives.
		MeowifyConfig.load();
	}
}
