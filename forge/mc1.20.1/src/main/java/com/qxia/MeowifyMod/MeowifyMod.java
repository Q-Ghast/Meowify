package com.qxia.MeowifyMod;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(MeowifyMod.MOD_ID)
public class MeowifyMod {

    public static final String MOD_ID = "meowify";
    private static final Logger LOGGER = LoggerFactory.getLogger(MeowifyMod.class);

    public MeowifyMod() {
        LOGGER.info("Meowify mod is loading... 喵~");
        MeowifyConfig.register();
        // Enables the "Config" button on the mod list; without it Forge greys the button out.
        MinecraftForge.registerConfigScreen(MeowifyConfig::configScreen);
        MinecraftForge.EVENT_BUS.register(new MeowifyEventHandler());
    }
}
