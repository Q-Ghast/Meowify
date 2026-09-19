package com.qxia.MeowifyMod;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/**
 * The suffix this mod appends, shared by the item tooltip, the hotbar item name overlay and the Jade
 * integration.
 *
 * <p>The text comes from {@code assets/meowify/lang/&lt;locale&gt;.json} and is therefore shown in
 * whatever language the player has selected in-game, falling back to {@code en_us} for any locale
 * that has no translation.</p>
 */
public final class MeowifyText {

    /** Translation key of the suffix, defined in {@code assets/meowify/lang/}. */
    public static final String SUFFIX_KEY = "meowify.suffix";

    private MeowifyText() {
    }

    /**
     * Appends the localised suffix after the given component. The style of the component is copied onto
     * the suffix so it keeps the same colour and formatting as the name it follows.
     */
    public static MutableComponent appendSuffix(Component name) {
        Style style = name.getStyle();
        if (style == null) {
            style = Style.EMPTY;
        }

        return Component.empty().append(name).append(Component.translatable(SUFFIX_KEY).withStyle(style));
    }
}
