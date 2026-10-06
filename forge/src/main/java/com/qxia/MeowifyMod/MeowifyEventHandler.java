package com.qxia.MeowifyMod;

import net.minecraft.network.chat.Component;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class MeowifyEventHandler {

    @SubscribeEvent
    public void onItemTooltip(ItemTooltipEvent event) {
        if (!event.getToolTip().isEmpty()) {
            Component originalName = event.getToolTip().get(0);
            event.getToolTip().set(0, MeowifyText.appendSuffix(originalName));
        }
    }
}
