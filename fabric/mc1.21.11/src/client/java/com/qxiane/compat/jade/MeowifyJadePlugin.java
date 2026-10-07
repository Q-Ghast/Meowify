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
 * <h2>Why the element is replaced rather than its text rewritten</h2>
 *
 * <p>Jade measures a text element once, when it is constructed, and never recomputes that measurement.
 * Writing a longer component into {@code TextElementImpl#text} therefore leaves the element still
 * reporting the width of the original name while the text drawn from it has grown: the tooltip is sized
 * for the shorter string and the end of the suffix is clipped away. A long name pushes the suffix
 * entirely outside the box, which reads as if nothing had been appended at all.</p>
 *
 * <p>{@code ITooltip#replace} avoids that: it builds a brand new text element whose width is measured
 * from the suffixed text, and it marks the tooltip line dirty so the line is measured again. It also puts
 * the {@code CORE_OBJECT_NAME} tag back onto the replacement, so the name stays reachable under the same
 * tag.</p>
 *
 * <p>The replacement happens at <em>collection</em> time, before Jade measures the tooltip for the
 * frame.</p>
 *
 * <p>The name element is the one tagged {@link JadeIds#CORE_OBJECT_NAME}, the uid Jade registers its own
 * {@code ObjectNameProvider} under. That single tag covers block names, entity names and the special
 * cases Jade resolves itself (picked results, custom names, item entities, item and block displays), so
 * Jade's naming logic does not have to be reimplemented.</p>
 *
 * <p>This class lives in the {@code client} source set because Jade's element types extend Minecraft
 * client classes, which the {@code main} source set cannot see.</p>
 */
public class MeowifyJadePlugin implements IWailaPlugin {

	@Override
	public void registerClient(IWailaClientRegistration registration) {
		registration.addTooltipCollectedCallback(new SuffixApplier());
	}

	/** Runs while the tooltip is still being assembled, before Jade measures it. */
	private static final class SuffixApplier implements JadeTooltipCollectedCallback {

		@Override
		public void onTooltipCollected(BoxElement boxElement, Accessor<?> accessor) {
			if (!MeowifyConfig.isJadeSuffixEnabled()) {
				return;
			}

			Component name = readName(boxElement.getTooltip());
			if (name == null) {
				return;
			}
			// replace() rewrites the tooltip, so the name is read before this call, never during.
			boxElement.getTooltip().replace(JadeIds.CORE_OBJECT_NAME, MeowifyText.appendSuffixForJade(name));
		}

		/** The component of the first name element in the tooltip, or null when there is none. */
		private static Component readName(ITooltip tooltip) {
			for (LayoutElement element : tooltip.get(JadeIds.CORE_OBJECT_NAME)) {
				if (!(element instanceof Element text)) {
					continue;
				}
				Component name = text.getNarration();
				if (name != null && !name.getString().isBlank()) {
					return name;
				}
			}
			return null;
		}
	}
}
