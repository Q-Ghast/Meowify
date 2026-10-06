package com.qxiane.compat.jade;

import com.qxiane.MeowifyConfig;
import com.qxiane.MeowifyText;

import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.network.chat.Component;
import snownee.jade.api.Accessor;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.JadeIds;
import snownee.jade.api.callback.JadeTooltipCollectedCallback;
import snownee.jade.api.ui.BoxElement;
import snownee.jade.api.ui.Element;

/**
 * Jade integration: appends the suffix to the block and entity names Jade shows in its overlay.
 *
 * <p>Jade discovers Fabric plugins through the {@code jade} entrypoint in {@code fabric.mod.json}, so
 * this class is listed there instead of carrying Forge's {@code @WailaPlugin} annotation. The entrypoint
 * is only loaded when Jade is installed, which is what keeps Jade optional.</p>
 *
 * <p>Names in Jade's overlay are the elements tagged with {@link JadeIds#CORE_OBJECT_NAME}, the uid Jade
 * registers its own {@code ObjectNameProvider} under. That single tag covers block names, entity names
 * and the special cases Jade itself resolves (picked results, custom names, item entities, item and
 * block displays), so Jade's naming logic does not have to be reimplemented.</p>
 *
 * <p>This class lives in the {@code client} source set because Jade's element types extend Minecraft
 * client classes, which the {@code main} source set cannot see.</p>
 */
public class MeowifyJadePlugin implements IWailaPlugin {

	@Override
	public void registerClient(IWailaClientRegistration registration) {
		registration.addTooltipCollectedCallback(new SuffixApplier());
	}

	/**
	 * Replaces every name element of a collected tooltip with the same name plus the suffix.
	 */
	private static final class SuffixApplier implements JadeTooltipCollectedCallback {

		@Override
		public void onTooltipCollected(BoxElement boxElement, Accessor<?> accessor) {
			if (!MeowifyConfig.isJadeSuffixEnabled()) {
				return;
			}

			ITooltip tooltip = boxElement.getTooltip();
			// ITooltip#get is typed as Minecraft's LayoutElement, which has no text accessor, so the
			// elements are narrowed to Jade's own Element to reach getNarration().
			for (LayoutElement element : tooltip.get(JadeIds.CORE_OBJECT_NAME)) {
				if (!(element instanceof Element name)) {
					continue;
				}
				Component text = name.getNarration();
				if (text == null || text.getString().isBlank()) {
					continue;
				}
				tooltip.replace(JadeIds.CORE_OBJECT_NAME, MeowifyText.appendSuffix(text));
			}
		}
	}
}
