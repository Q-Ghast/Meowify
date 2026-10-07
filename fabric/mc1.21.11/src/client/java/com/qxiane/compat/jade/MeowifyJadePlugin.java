package com.qxiane.compat.jade;

import java.lang.reflect.Field;
import java.util.WeakHashMap;

import com.qxiane.MeowifyConfig;
import com.qxiane.MeowifyText;

import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.network.chat.Component;
import snownee.jade.api.Accessor;
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
 * <h2>Timing matters more than the replacement itself</h2>
 *
 * <p>The name is rewritten at <em>collection</em> time, before Jade measures the tooltip. Doing it later,
 * just before the overlay is drawn, produces a box whose width was computed from the plain name while
 * the text has grown by the suffix: the text overflows and the end of it is clipped away. That failure is
 * easy to misread, because a short suffix can end up almost entirely outside the box and look like it was
 * never added.</p>
 *
 * <p>The rewrite also has to happen on the element already in the tooltip rather than through
 * {@code ITooltip#replace}. That call builds a brand new element, and the element it leaves in the
 * tooltip is not the one the tooltip ends up drawing from.</p>
 *
 * <p>The name element is the one tagged {@link JadeIds#CORE_OBJECT_NAME}, the uid Jade registers its own
 * {@code ObjectNameProvider} under. That single tag covers block names, entity names and the special cases
 * Jade resolves itself (picked results, custom names, item entities, item and block displays), so Jade's
 * naming logic does not have to be reimplemented.</p>
 *
 * <p>This class lives in the {@code client} source set because Jade's element types extend Minecraft
 * client classes, which the {@code main} source set cannot see.</p>
 */
public class MeowifyJadePlugin implements IWailaPlugin {

	/**
	 * The text an element draws, which {@code TextElementImpl#render} reads straight from this field. It is
	 * private and Jade exposes no setter, so it is reached reflectively. If a future Jade changes the field,
	 * this resolves to null and the overlay is left exactly as Jade drew it rather than throwing.
	 */
	private static final Field TEXT_FIELD = resolveTextField();

	/**
	 * The component this code last wrote into an element, so an element that is handed over again is not
	 * given a second suffix. Keys are weak so elements Jade drops can be collected.
	 */
	private static final WeakHashMap<Object, Component> LAST_WRITTEN = new WeakHashMap<>();

	@Override
	public void registerClient(IWailaClientRegistration registration) {
		registration.addTooltipCollectedCallback(new SuffixApplier());
	}

	/** Runs while the tooltip is still being assembled, before Jade measures it. */
	private static final class SuffixApplier implements JadeTooltipCollectedCallback {

		@Override
		public void onTooltipCollected(BoxElement boxElement, Accessor<?> accessor) {
			if (TEXT_FIELD == null || !MeowifyConfig.isJadeSuffixEnabled()) {
				return;
			}

			// ITooltip#get is typed as Minecraft's LayoutElement, which has no text accessor.
			for (LayoutElement element : boxElement.getTooltip().get(JadeIds.CORE_OBJECT_NAME)) {
				patch(element);
			}
		}
	}

	/** Rewrites the element's own text field, leaving the element instance and its tag in place. */
	private static void patch(Object element) {
		if (!(element instanceof Element)) {
			return;
		}
		try {
			Object current = TEXT_FIELD.get(element);
			if (!(current instanceof Component component) || LAST_WRITTEN.get(element) == current) {
				return;
			}
			Component next = MeowifyText.appendSuffixForJade(component);
			TEXT_FIELD.set(element, next);
			LAST_WRITTEN.put(element, next);
		} catch (IllegalAccessException | RuntimeException ignored) {
			// An element that cannot be patched is left as Jade drew it.
		}
	}

	private static Field resolveTextField() {
		try {
			Field field = Class.forName("snownee.jade.impl.ui.TextElementImpl").getDeclaredField("text");
			field.setAccessible(true);
			return field;
		} catch (ReflectiveOperationException | RuntimeException e) {
			return null;
		}
	}
}
