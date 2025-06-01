package com.hezaerd;

import com.hezaerd.registry.ModEntityType;
import com.hezaerd.registry.ModItems;
import com.hezaerd.registry.ModStats;
import com.hezaerd.utils.ModLib;
import net.fabricmc.api.ModInitializer;

public class SkippingStones implements ModInitializer {
	@Override
	public void onInitialize() {
		ModLib.LOGGER.info("Initializing..");

		ModStats.init();
		ModItems.init();
		ModEntityType.init();
	}
}