package com.qxia.MeowifyMod;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/**
 * Builds the suffix this mod appends, shared by the item tooltip, the hotbar item name overlay and the
 * Jade integration.
 *
 * <p>The text is normally the translation of {@link #SUFFIX_KEY} from
 * {@code assets/meowify/lang/&lt;locale&gt;.json}, so it follows the language selected in game and falls
 * back to {@code en_us}. The {@code customSuffix} option in {@code config/meowify-client.toml} overrides
 * it with a literal string in every language.</p>
 *
 * <p>Whether a suffix is produced at all depends on {@link MeowifyConfig}: the global toggle covers the
 * two vanilla display locations, and the Jade toggle covers Jade's overlay.</p>
 */
public final class MeowifyText {

    /** Translation key of the suffix, defined in {@code assets/meowify/lang/}. */
    public static final String SUFFIX_KEY = "meowify.suffix";

    private MeowifyText() {
    }

    /** Whether the two vanilla display locations (tooltip and hotbar overlay) get a suffix. */
    public static boolean hasSuffixGlobal() {
        return MeowifyConfig.isGlobalSuffixEnabled();
    }

    /** Whether Jade's overlay gets a suffix. */
    public static boolean hasSuffixJade() {
        return MeowifyConfig.isJadeSuffixEnabled();
    }

    /**
     * The suffix to append, exactly as it should follow the name: the configured {@code customSuffix}
     * gets a single separating space in front of it, while the translation of {@link #SUFFIX_KEY} is
     * used as it ships because each language file carries its own spacing. The returned component is
     * unstyled; {@link #append} copies the style of the name onto it.
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
     * Appends the suffix after the given component for the two vanilla display locations. Returns the
     * component unchanged when the suffix is switched off, so disabling the mod costs nothing.
     *
     * <p>The style of the name is copied onto the suffix, so it keeps the same colour and formatting as
     * the name it follows (rarity colour; italic for renamed items).</p>
     */
    public static MutableComponent appendSuffix(Component name) {
        if (!hasSuffixGlobal()) {
            return name.copy();
        }
        return append(name, suffix());
    }

    /**
     * Appends the suffix after the given component for Jade's overlay. Returns the component unchanged
     * when the suffix is switched off.
     */
    public static Component appendSuffixForJade(Component name) {
        if (!hasSuffixJade()) {
            return name;
        }
        return append(name, suffix());
    }

    private static MutableComponent append(Component name, Component suffix) {
        Style style = name.getStyle();
        if (style == null) {
            style = Style.EMPTY;
        }

        return Component.empty().append(name).append(suffix.copy().withStyle(style));
    }
}
