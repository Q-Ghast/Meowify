package com.qxia.MeowifyMod;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/**
 * The " 喵~" text appended by this mod, shared by the item tooltip and the hotbar item name overlay.
 */
public final class MeowifyText {

    public static final String SUFFIX = " \u55b5~";

    private MeowifyText() {
    }

    /**
     * Appends {@link #SUFFIX} after the given component. The style of the component is copied onto the
     * suffix so it keeps the same colour and formatting as the name it follows.
     */
    public static MutableComponent appendSuffix(Component name) {
        Style style = name.getStyle();
        if (style == null) {
            style = Style.EMPTY;
        }

        return Component.empty().append(name).append(Component.literal(SUFFIX).withStyle(style));
    }
}
