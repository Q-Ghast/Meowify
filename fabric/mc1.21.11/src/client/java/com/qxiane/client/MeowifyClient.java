package com.qxiane.client;

import java.util.List;

import com.qxiane.MeowifyText;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.network.chat.Component;

public class MeowifyClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		ItemTooltipCallback.EVENT.register((stack, tooltipContext, tooltipType, lines) -> appendSuffix(lines));
	}

	/**
	 * Replaces the first line of the tooltip, which is the item name, with the name plus the suffix.
	 */
	private static void appendSuffix(List<Component> lines) {
		if (lines.isEmpty() || !MeowifyText.hasSuffix()) {
			return;
		}
		lines.set(0, MeowifyText.appendSuffix(lines.get(0)));
	}
}
