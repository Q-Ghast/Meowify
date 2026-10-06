package com.qxia.MeowifyMod;

import org.apache.commons.lang3.StringUtils;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

/**
 * The mod's configuration file, written to {@code config/meowify-client.toml}.
 *
 * <p>The type is {@code CLIENT} because Meowify is a client-side only mod: these settings only affect
 * what the local player sees. Forge also lists CLIENT configs in the mod list's config screen, so the
 * options can be changed in game.</p>
 */
public final class MeowifyConfig {

    public static final ForgeConfigSpec SPEC;

    private static final ForgeConfigSpec.BooleanValue ENABLE_GLOBAL_SUFFIX;
    private static final ForgeConfigSpec.BooleanValue ENABLE_JADE_SUFFIX;
    private static final ForgeConfigSpec.ConfigValue<String> CUSTOM_SUFFIX;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        ENABLE_GLOBAL_SUFFIX = builder
                .comment("Append the suffix to item names in the inventory tooltip and in the item name",
                        "that fades out in the middle of the screen when you switch hotbar slots.")
                .define("enableGlobalSuffix", true);

        ENABLE_JADE_SUFFIX = builder
                .comment("Append the suffix to the block, entity and item names in Jade's overlay.",
                        "Only has an effect when Jade is installed.")
                .define("enableJadeSuffix", true);

        CUSTOM_SUFFIX = builder
                .comment("Use this text instead of the translated suffix that ships in",
                        "assets/meowify/lang/<locale>.json. Leave it empty to keep the translated one,",
                        "which follows the language selected in game.",
                        "The value is a literal string, not a translation key, so it is used verbatim in",
                        "every language. Example: customSuffix = \"meow~\" renders as \"Stone meow~\".")
                .define("customSuffix", "");

        SPEC = builder.build();
    }

    private MeowifyConfig() {
    }

    /** Registers the config file with Forge. Called from the mod constructor. */
    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, SPEC);
    }

    /** Whether item names outside of Jade get the suffix. */
    public static boolean isGlobalSuffixEnabled() {
        return ENABLE_GLOBAL_SUFFIX.get();
    }

    /** Whether the names in Jade's overlay get the suffix. */
    public static boolean isJadeSuffixEnabled() {
        return ENABLE_JADE_SUFFIX.get();
    }

    /**
     * The custom suffix the player typed, with surrounding whitespace stripped, or {@code null} when it
     * is not set. The leading space of a value like {@code " meow~"} is significant and is therefore
     * preserved by the trim.
     */
    public static String customSuffix() {
        return StringUtils.trimToNull(CUSTOM_SUFFIX.get());
    }
}
