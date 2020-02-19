package nl.dgoossens.chiselsandbits2.client.util;

import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;
import org.apache.commons.lang3.text.WordUtils;

import java.util.List;
import java.util.Objects;

public final class ItemTooltipWriter {
    private static final int WRAP_LENGTH = 42; //The amount of characters to automatically cut off a tooltip line after.

    /**
     * Automatically builds the tooltip of an item by parsing the language entry.
     * | -> new line
     * $ -> next keybinding
     */
    public static void addItemInformation(List<ITextComponent> textComponents, String languageKey, KeyBinding... keys) {
        String fullString = I18n.format("item." + ChiselsAndBits2.MOD_ID + "." + languageKey);
        StringBuilder past = new StringBuilder();
        int j = 0;
        for (char c : fullString.toCharArray()) {
            if (c == '|') {
                //New line
                append(textComponents, past.toString());
                past = new StringBuilder();
                continue;
            }
            if (c == '$') {
                //Next keybinding name
                if (keys.length == j) continue; //If we got no more keybinds we can't put them in.
                past.append(getKeyName(keys[j]).getFormattedText());
                j++;
                continue;
            }
            past.append(c);
        }
        append(textComponents, past.toString());
    }

    private static void append(List<ITextComponent> textComponents, String toAdd) {
        for (String t : WordUtils.wrap(toAdd, WRAP_LENGTH, "|", true).split("\\|")) {
            StringTextComponent st = new StringTextComponent(t);
            st.getStyle().setColor(TextFormatting.GRAY);
            textComponents.add(st);
        }
    }

    /**
     * Gets the showable display name for a keybind for item tooltips or other places.
     */
    public static ITextComponent getKeyName(final KeyBinding keyBinding) {
        if (keyBinding == null)
            return new TranslationTextComponent("general." + ChiselsAndBits2.MOD_ID + ".no_keybinding");
        if (keyBinding.getKey().getKeyCode() == 0 && keyBinding.getDefault().getKeyCode() != 0)
            return new StringTextComponent(keyBinding.getKeyModifierDefault().getLocalizedComboName(keyBinding.getDefault(), () -> {
                String s = keyBinding.getDefault().getTranslationKey();
                int i = keyBinding.getDefault().getKeyCode();
                String s1 = null;
                switch (keyBinding.getDefault().getType()) {
                    case KEYSYM:
                        s1 = InputMappings.getKeynameFromKeycode(i);
                        break;
                    case SCANCODE:
                        s1 = InputMappings.getKeyNameFromScanCode(i);
                        break;
                    case MOUSE:
                        String s2 = I18n.format(s);
                        s1 = Objects.equals(s2, s) ? I18n.format(InputMappings.Type.MOUSE.getName(), i + 1) : s2;
                }

                return s1 == null ? I18n.format(s) : s1;
            })); //This is basically a copy of getLocalizedName but using the default instead.
        return new StringTextComponent(keyBinding.getLocalizedName());
    }
}
