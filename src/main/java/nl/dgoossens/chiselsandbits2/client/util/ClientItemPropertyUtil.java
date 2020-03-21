package nl.dgoossens.chiselsandbits2.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IngameGui;
import net.minecraft.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;
import nl.dgoossens.chiselsandbits2.api.item.DyedItemColour;
import nl.dgoossens.chiselsandbits2.common.impl.item.ItemModeType;
import nl.dgoossens.chiselsandbits2.common.impl.item.PlayerItemMode;
import nl.dgoossens.chiselsandbits2.common.network.client.CItemStatePacket;
import nl.dgoossens.chiselsandbits2.common.network.client.CUpdatePlayerItemModesPacket;
import nl.dgoossens.chiselsandbits2.common.network.client.CTapeMeasureColourPacket;
import nl.dgoossens.chiselsandbits2.common.network.server.SPlayerItemModePacket;

import java.lang.reflect.Field;

/**
 * Handles client-only parts of the ItemPropertyUtil.
 */
@OnlyIn(Dist.CLIENT)
public class ClientItemPropertyUtil {
    private static PlayerItemMode cbm = (PlayerItemMode) ItemModeType.CHISELED_BLOCK.getDefault();

    /**
     * Read the global chiseled block mode from an incoming packet.
     */
    public static void readPlayerItemModes(final SPlayerItemModePacket packet) {
        cbm = packet.getChiseledBlockMode();
    }

    /**
     *  Get the chiseled block mode the main client player is using.
     */
    public static PlayerItemMode getChiseledBlockMode() {
        return cbm;
    }

    /**
     * Set the selected chiseled block mode.
     */
    public static void setChiseledBlockMode(final PlayerItemMode itemMode) {
        cbm = itemMode;
        ChiselsAndBits2.getInstance().getNetworkRouter().sendToServer(new CUpdatePlayerItemModesPacket(itemMode));
        reshowHighlightedStack();
    }

    /**
     * Set a tape measure you're holding's colour to the passed colour.
     */
    public static void setTapeMeasureColor(DyedItemColour c) {
        ChiselsAndBits2.getInstance().getNetworkRouter().sendToServer(new CTapeMeasureColourPacket(c));
    }

    /**
     * Set the lock state for the held item.
     */
    public static void setLockState(boolean b) {
        ChiselsAndBits2.getInstance().getNetworkRouter().sendToServer(new CItemStatePacket(b, true));
    }

    /**
     * Set the swap state for the held item.
     */
    public static void setSwapState(boolean b) {
        ChiselsAndBits2.getInstance().getNetworkRouter().sendToServer(new CItemStatePacket(b, false));
    }

    /**
     * Reshows the highlighted stack item.
     * Only works on client-side.
     */
    public static void reshowHighlightedStack() {
        try {
            IngameGui ig = Minecraft.getInstance().ingameGUI;
            //IngameGui#highlightingItemStack
            Field f = null;
            for(Field fe : IngameGui.class.getDeclaredFields()) {
                //We abuse the fact that IngameGui only has one ItemStack and that's the one we need.
                if(ItemStack.class.isAssignableFrom(fe.getType())) {
                    f = fe;
                    break;
                }
            }
            if(f == null) throw new RuntimeException("Unable to lookup textures.");
            f.setAccessible(true);
            f.set(ig, Minecraft.getInstance().player.getHeldItemMainhand());

            //IngameGui#remainingHighlightTicks
            Field f2 = null;
            int i = 0;
            for(Field fe : IngameGui.class.getDeclaredFields()) {
                //We want the third int type field which is remainingHighlightTicks.
                if(Integer.TYPE.isAssignableFrom(fe.getType())) {
                    i++;
                    if(i==3) {
                        f2 = fe;
                        break;
                    }
                }
            }
            f2.setAccessible(true);
            f2.set(ig, 40);
        } catch(Exception x) {
            x.printStackTrace();
        }
    }
}
