package com.qxia.MeowifyMod.compat.jade;

import java.util.List;

import com.qxia.MeowifyMod.MeowifyText;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.Accessor;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.Identifiers;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.ui.IElement;
import snownee.jade.impl.ui.TextElement;

/**
 * Jade (WAILA fork) integration: appends " 喵~" to the names Jade shows in its overlay.
 *
 * <p>This class is only ever loaded when Jade is installed. Jade discovers it by scanning for the
 * {@link WailaPlugin} annotation, so no entry in {@code mods.toml} is needed. When Jade is absent the
 * class is never loaded and the mod behaves exactly as before.</p>
 *
 * <p>Two kinds of line carry a name in Jade's overlay:</p>
 * <ul>
 *     <li>the <b>title</b> line built by Jade's own {@code ObjectNameProvider} &mdash; this covers block
 *     names, entity names, and the special cases Jade handles (picked results, custom names, item
 *     entities, item/block displays);</li>
 *     <li>the <b>item name</b> columns of an item storage listing (chests, furnaces, ...), which Jade
 *     renders as {@code "12× Stone"}. Jade does not tag that line, so it is recognised by its
 *     {@code "× "} form.</li>
 * </ul>
 *
 * <p>Both cases are handled in a "tooltip collected" callback, i.e. after every provider has had its
 * say and immediately before the tooltip is turned into a renderer. Jade builds a brand new tooltip
 * every client tick, so elements are never suffixed twice.</p>
 */
@WailaPlugin
public class MeowifyJadePlugin implements IWailaPlugin {

    /**
     * Marker used by Jade to separate the item amount from the item name in storage listings, e.g.
     * {@code "12× Stone"}.
     */
    private static final String AMOUNT_SEPARATOR = "\u00d7 ";

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.addTooltipCollectedCallback(MeowifyJadePlugin::appendSuffixToNames);
    }

    private static void appendSuffixToNames(ITooltip tooltip, Accessor<?> accessor) {
        int lines = tooltip.size();
        for (int line = 0; line < lines; line++) {
            appendSuffixToNames(tooltip, line, IElement.Align.LEFT);
            appendSuffixToNames(tooltip, line, IElement.Align.RIGHT);
        }
    }

    private static void appendSuffixToNames(ITooltip tooltip, int line, IElement.Align align) {
        // The returned list is Jade's backing list, so replacing an entry in place affects the tooltip.
        List<IElement> elements = tooltip.get(line, align);
        for (int i = 0; i < elements.size(); i++) {
            IElement element = elements.get(i);
            if (!(element instanceof TextElement textElement)) {
                continue;
            }
            FormattedText text = textElement.text;
            if (!(text instanceof Component component)) {
                continue;
            }
            if (isNameLine(element, component)) {
                elements.set(i, new TextElement(MeowifyText.appendSuffix(component)));
            }
        }
    }

    /**
     * Whether this element is one of the name lines described in the class javadoc. The null check on
     * the tag covers the vanilla tooltip rendered on top of a chat item link, whose elements are not
     * tagged by a Jade provider.
     */
    private static boolean isNameLine(IElement element, Component text) {
        ResourceLocation tag = element.getTag();
        if (Identifiers.CORE_OBJECT_NAME.equals(tag)) {
            return true;
        }
        // A storage listing line: "<amount>× <item name>". The name itself may legitimately contain
        // "× ", so a further "× " after the first one means this line was already rewritten (or was
        // never an amount line) and must be left alone.
        String message = text.getString();
        int separator = message.indexOf(AMOUNT_SEPARATOR);
        if (separator <= 0) {
            return false;
        }
        return message.indexOf(AMOUNT_SEPARATOR, separator + AMOUNT_SEPARATOR.length()) < 0;
    }
}
