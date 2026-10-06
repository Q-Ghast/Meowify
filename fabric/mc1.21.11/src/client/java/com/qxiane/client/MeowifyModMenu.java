package com.qxiane.client;

import com.qxiane.MeowifyConfig;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;

/**
 * Adds the "Config" button to Fabric's mod list and hands ModMenu the screen it opens.
 *
 * <p>This is a ModMenu entrypoint, declared under {@code modmenu} in {@code fabric.mod.json}. ModMenu
 * only loads that entrypoint when it is installed, so nothing here runs otherwise and ModMenu stays
 * optional. The screen itself is built with Cloth Config, which ModMenu already depends on.</p>
 */
public class MeowifyModMenu implements ModMenuApi {

	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return parent -> {
			ConfigBuilder builder = ConfigBuilder.create()
					.setParentScreen(parent)
					.setTitle(Component.translatable("meowify.config.title"));

			ConfigEntryBuilder entries = builder.entryBuilder();
			builder.setSavingRunnable(() -> {
				// The entries below already write through on change; this only exists so Cloth Config
				// has a save action to call when the screen is closed.
			});

			builder.getOrCreateCategory(Component.translatable("meowify.config.title"))
					.addEntry(entries
							.startBooleanToggle(Component.translatable("meowify.config.enableGlobalSuffix"),
									MeowifyConfig.isGlobalSuffixEnabled())
							.setDefaultValue(true)
							.setTooltip(Component.translatable("meowify.config.enableGlobalSuffix.tooltip"))
							.setSaveConsumer(MeowifyConfig::setGlobalSuffixEnabled)
							.build())
					.addEntry(entries
							.startBooleanToggle(Component.translatable("meowify.config.enableJadeSuffix"),
									MeowifyConfig.isJadeSuffixEnabled())
							.setDefaultValue(true)
							.setTooltip(Component.translatable("meowify.config.enableJadeSuffix.tooltip"))
							.setSaveConsumer(MeowifyConfig::setJadeSuffixEnabled)
							.build())
					.addEntry(entries
							.startStrField(Component.translatable("meowify.config.customSuffix"),
									MeowifyConfig.customSuffix() == null ? "" : MeowifyConfig.customSuffix())
							.setDefaultValue("")
							.setTooltip(Component.translatable("meowify.config.customSuffix.tooltip"))
							.setSaveConsumer(MeowifyConfig::setCustomSuffix)
							.build());

			return builder.build();
		};
	}
}
