package com.qxia.MeowifyMod;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * The in-game configuration screen, opened from the "Config" button on the mod list.
 *
 * <p>Forge only enables that button for mods that register a screen factory, which {@link MeowifyMod}
 * does through {@code MinecraftForge.registerConfigScreen}.</p>
 *
 * <p>This is a plain {@link Screen} rather than one of the vanilla options screens: those are built
 * around {@code OptionInstance} buttons and an {@code OptionsList} that cannot hold an {@link EditBox},
 * and this screen has three controls that are simpler to place directly.</p>
 *
 * <p>The two toggles are written to {@code config/meowify-client.toml} as soon as they are clicked, and
 * {@code ForgeConfigSpec} persists that to disk immediately. The custom suffix field is applied when
 * the screen closes, because saving on every keystroke would rewrite the file per character. There is
 * deliberately no Cancel button: what is on screen when you press Done is what gets saved.</p>
 */
public class MeowifyConfigScreen extends Screen {

    /** Windows at least this wide put the caption beside the field instead of above it. */
    private static final int SIDE_BY_SIDE_WIDTH = 320;

    private static final int FIELD_WIDTH = 200;
    private static final int FIELD_HEIGHT = 20;
    private static final int FIELD_MAX_LENGTH = 64;
    private static final int LABEL_GAP = 4;
    private static final int ROW_SPACING = 24;

    private final Screen parent;
    private final Component suffixCaption = Component.translatable("meowify.config.customSuffix");

    private CycleButton<Boolean> globalToggle;
    private CycleButton<Boolean> jadeToggle;
    private EditBox customSuffixField;

    public MeowifyConfigScreen(Screen parent) {
        super(Component.translatable("meowify.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int x = width / 2 - FIELD_WIDTH / 2;
        // Three rows centred on the screen, nudged up so the caption above the field has room.
        int firstRow = height / 2 - 40;

        globalToggle = addRenderableWidget(CycleButton
                .onOffBuilder(MeowifyConfig.isGlobalSuffixEnabled())
                .create(x, firstRow, FIELD_WIDTH, FIELD_HEIGHT,
                        Component.translatable("meowify.config.enableGlobalSuffix"),
                        (button, value) -> MeowifyConfig.setGlobalSuffixEnabled(value)));

        jadeToggle = addRenderableWidget(CycleButton
                .onOffBuilder(MeowifyConfig.isJadeSuffixEnabled())
                .create(x, firstRow + ROW_SPACING, FIELD_WIDTH, FIELD_HEIGHT,
                        Component.translatable("meowify.config.enableJadeSuffix"),
                        (button, value) -> MeowifyConfig.setJadeSuffixEnabled(value)));

        String current = MeowifyConfig.customSuffix();
        customSuffixField = addRenderableWidget(new EditBox(font, x, firstRow + 2 * ROW_SPACING,
                FIELD_WIDTH, FIELD_HEIGHT, suffixCaption));
        customSuffixField.setMaxLength(FIELD_MAX_LENGTH);
        // An empty field means "use the translated suffix", which the hint spells out.
        customSuffixField.setValue(current == null ? "" : current);
        customSuffixField.setHint(Component.translatable("meowify.config.customSuffix.hint"));

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(width / 2 - 100, height - 27, 200, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        FormattedCharSequence caption = suffixCaption.getVisualOrderText();
        int captionWidth = font.width(caption);
        int captionX;
        int captionY;

        if (width >= SIDE_BY_SIDE_WIDTH) {
            captionX = customSuffixField.getX() - LABEL_GAP - captionWidth;
            captionY = customSuffixField.getY() + (FIELD_HEIGHT - font.lineHeight) / 2 + 1;
        } else {
            captionX = width / 2 - captionWidth / 2;
            captionY = customSuffixField.getY() - font.lineHeight - LABEL_GAP;
        }

        guiGraphics.drawString(font, caption, captionX, captionY, 0xFFFFFF, false);
    }

    @Override
    public void tick() {
        super.tick();
        customSuffixField.tick();
    }

    @Override
    public void onClose() {
        String typed = customSuffixField.getValue();
        if (!typed.equals(storedSuffix())) {
            MeowifyConfig.setCustomSuffix(typed);
        }
        minecraft.setScreen(parent);
    }

    /** The stored custom suffix, normalised the same way the config does it. */
    private static String storedSuffix() {
        String current = MeowifyConfig.customSuffix();
        return current == null ? "" : current;
    }
}
