package nl.dgoossens.chiselsandbits2.api.item;

import net.minecraft.client.resources.I18n;
import net.minecraft.item.DyeColor;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;

/**
 * The different colours that IColourable items can be coloured to look like.
 */
public enum DyedItemColour {
    WHITE(16383998),
    ORANGE(16351261),
    MAGENTA(13061821),
    LIGHT_BLUE(3847130),
    YELLOW(16701501),
    LIME(8439583),
    PINK(15961002),
    GRAY(4673362),
    LIGHT_GRAY(10329495),
    CYAN(1481884),
    PURPLE(8991416),
    BLUE(3949738),
    BROWN(8606770),
    GREEN(6192150),
    RED(11546150),
    BLACK(1908001),
    ;

    private int colour;

    DyedItemColour(int col) {
        colour = col;
    }

    public int getColour() {
        return colour;
    }

    public static DyedItemColour fromDye(DyeColor dye) {
        switch (dye) {
            case WHITE:
                return WHITE;
            case ORANGE:
                return ORANGE;
            case MAGENTA:
                return MAGENTA;
            case LIGHT_BLUE:
                return LIGHT_BLUE;
            case YELLOW:
                return YELLOW;
            case LIME:
                return LIME;
            case PINK:
                return PINK;
            case GRAY:
                return GRAY;
            case LIGHT_GRAY:
                return LIGHT_GRAY;
            case CYAN:
                return CYAN;
            case PURPLE:
                return PURPLE;
            case BLUE:
                return BLUE;
            case BROWN:
                return BROWN;
            case GREEN:
                return GREEN;
            case RED:
                return RED;
            case BLACK:
                return BLACK;
        }
        return BLACK;
    }

    public String getLocalizedName() {
        return I18n.format("general." + ChiselsAndBits2.MOD_ID + ".tape_measure." + name().toLowerCase());
    }
}
