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

	/** Whether item names get a suffix. */
	public static boolean hasSuffix() {
		return MeowifyConfig.isGlobalSuffixEnabled();
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
	 * Appends the suffix after the given component, or returns it unchanged when the suffix is switched
	 * off. The style of the name is copied onto the suffix, so it keeps the same colour and formatting as
	 * the name it follows (rarity colour; italic for renamed items).
	 */
	public static Component appendSuffix(Component name) {
		if (!hasSuffix()) {
			return name;
		}

		Style style = name.getStyle();
		if (style == null) {
			style = Style.EMPTY;
		}

		MutableComponent suffix = suffix().copy();
		suffix.withStyle(style);
		return Component.empty().append(name).append(suffix);
	}
}
