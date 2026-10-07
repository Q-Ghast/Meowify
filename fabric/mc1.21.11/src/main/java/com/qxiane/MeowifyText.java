package com.qxiane;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/**
 * Builds the suffix this mod appends to item names.
 *
 * <p>The text is normally the translation of {@link #SUFFIX_KEY} from
 * {@code assets/meowify/lang/&lt;locale&gt;.json}, so it follows the language selected in game and falls
 * back to {@code en_us}. The {@code customSuffix} option in {@code config/meowify.json} overrides it with
 * a literal string in every language.</p>
 */
public final class MeowifyText {

	/** Translation key of the suffix, defined in {@code assets/meowify/lang/}. */
	public static final String SUFFIX_KEY = "meowify.suffix";

	private MeowifyText() {
	}

	/** Whether the <em>global</em> location, the inventory tooltip, gets a suffix. */
	public static boolean hasSuffix() {
		return MeowifyConfig.isGlobalSuffixEnabled();
	}

	/** Whether Jade's overlay gets a suffix. */
	public static boolean hasSuffixJade() {
		return MeowifyConfig.isJadeSuffixEnabled();
	}

	/**
	 * The suffix to append, exactly as it should follow the name: the configured {@code customSuffix} gets
	 * a single separating space in front of it, while the translation of {@link #SUFFIX_KEY} is used as it
	 * ships because each language file carries its own spacing.
	 */
	public static Component suffix() {
		String custom = MeowifyConfig.customSuffix();
		if (custom != null) {
			// The custom suffix is trimmed, so exactly one space is inserted here. That way both
			// "meow~" and " meow~" end up rendering as "Stone meow~".
			return Component.literal(" " + custom);
		}
		return Component.translatable(SUFFIX_KEY);
	}

	/**
	 * Appends the suffix for the inventory tooltip, or returns the name unchanged when the global toggle
	 * is off. The style of the name is copied onto the suffix, so it keeps the same colour and formatting
	 * as the name it follows (rarity colour; italic for renamed items).
	 */
	public static Component appendSuffix(Component name) {
		if (!hasSuffix()) {
			return name;
		}
		return append(name, suffix());
	}

	/**
	 * Appends the suffix for Jade's overlay, or returns the name unchanged when the Jade toggle is off.
	 *
	 * <p>This is deliberately separate from {@link #appendSuffix}: the two toggles are independent, so
	 * Jade's overlay is driven by {@code enableJadeSuffix} alone and is unaffected by the global toggle.</p>
	 */
	public static Component appendSuffixForJade(Component name) {
		if (!hasSuffixJade()) {
			return name;
		}
		return append(name, suffix());
	}

	private static Component append(Component name, Component suffix) {
		Style style = name.getStyle();
		if (style == null) {
			style = Style.EMPTY;
		}

		MutableComponent styled = suffix.copy();
		styled.withStyle(style);
		return Component.empty().append(name).append(styled);
	}
}
