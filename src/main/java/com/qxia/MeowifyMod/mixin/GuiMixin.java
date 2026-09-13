package com.qxia.MeowifyMod.mixin;

import com.qxia.MeowifyMod.MeowifyText;
import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Appends " 喵~" to the item name that is briefly shown in the middle of the screen when the player
 * switches the held item / hotbar slot.
 */
@Mixin(Gui.class)
public abstract class GuiMixin {

    /**
     * Replaces the {@code highlightTip} that {@code Gui#renderSelectedItemName} is about to draw, i.e.
     * exactly the text shown for the overlay. Hooking the assignment makes this independent of local
     * variable slot numbers and of any other component the method builds on the way.
     *
     * <p>{@code remap = false} because this two-argument overload is added by Forge itself: it has no
     * SRG name, so the method name is already the literal runtime name in both dev and production.</p>
     */
    @ModifyVariable(
            method = "renderSelectedItemName(Lnet/minecraft/client/gui/GuiGraphics;I)V",
            remap = false,
            at = @At(
                    value = "INVOKE_ASSIGN",
                    target = "Lnet/minecraft/world/item/ItemStack;getHighlightTip(Lnet/minecraft/network/chat/Component;)Lnet/minecraft/network/chat/Component;"
            )
    )
    private Component meowify$appendSuffix(Component highlightTip) {
        return MeowifyText.appendSuffix(highlightTip);
    }
}
