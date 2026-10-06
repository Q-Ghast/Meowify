package com.qxiane.compat.jade;

import java.lang.reflect.Field;
import java.util.WeakHashMap;

import com.qxiane.MeowifyConfig;
import com.qxiane.MeowifyText;

import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.network.chat.Component;
import snownee.jade.api.Accessor;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.JadeIds;
import snownee.jade.api.ui.BoxElement;
import snownee.jade.api.ui.Element;

/**
 * Jade integration: appends the suffix to the block, entity and item names Jade shows in its overlay.
 *
 * <p>Jade discovers Fabric plugins through the {@code jade} entrypoint in {@code fabric.mod.json}, so
 * this class is listed there instead of carrying Forge's {@code @WailaPlugin} annotation. The entrypoint
 * is only loaded when Jade is installed, which is what keeps Jade optional.</p>
 *
 * <h2>Why this patches at render time</h2>
 *
 * <p>The obvious hook, {@code JadeTooltipCollectedCallback}, does run and {@code ITooltip#replace} does
 * report success there, but the change never reaches the screen: the tooltip it hands out is not the one
 * Jade draws from. Measured on 1.21.11 with Jade 21.1.6, replacing the name in that callback left the
 * overlay showing the plain name even when the replacement was a hard-coded literal.</p>
 *
 * <p>{@code JadeBeforeRenderCallback} does get the object that is about to be drawn, so the name is
 * patched there instead. Jade hands out a fresh element tree every tick, so the patch is applied once
 * per element rather than once per frame; see {@link #LAST_WRITTEN}.</p>
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

	/**
	 * The text an element draws, which {@code TextElementImpl#render} reads straight from this field. It
	 * is private and Jade exposes no setter, so it is reached reflectively. If a future Jade changes the
	 * field, this resolves to null and the suffix is simply not added rather than throwing.
	 */
	private static final Field TEXT_FIELD = resolveTextField();

	/**
	 * The component this code last wrote into an element. Jade rebuilds the tree every tick, so the same
	 * element is handed to the callback once per frame while the overlay is visible; comparing against
	 * the last write keeps the suffix from being appended over and over. A rebuilt element does not match
	 * and gets patched again, which is what keeps the suffix present as the player looks around.
	 *
	 * <p>Keys are weak so elements that Jade drops can be collected.
	 */
	private static final WeakHashMap<Object, Component> LAST_WRITTEN = new WeakHashMap<>();

	@Override
	public void registerClient(IWailaClientRegistration registration) {
		registration.addBeforeRenderCallback(this::beforeRender);
	}

	/** Runs immediately before the tooltip is drawn, on the element that is about to be drawn. */
	private boolean beforeRender(BoxElement box, Object animation, Object graphics, Accessor<?> accessor) {
		if (TEXT_FIELD == null || !MeowifyConfig.isJadeSuffixEnabled()) {
			return false;
		}

		ITooltip tooltip = box.getTooltip();
		// ITooltip#get is typed as Minecraft's LayoutElement, which has no text accessor.
		for (LayoutElement element : tooltip.get(JadeIds.CORE_OBJECT_NAME)) {
			patch(element);
		}
		return false;
	}

	private static void patch(Object element) {
		if (!(element instanceof Element)) {
			return;
		}
		try {
			Object current = TEXT_FIELD.get(element);
			if (!(current instanceof Component component)) {
				return;
			}
			if (LAST_WRITTEN.get(element) == current) {
				return; // already carries the suffix
			}
			Component next = MeowifyText.appendSuffix(component);
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
