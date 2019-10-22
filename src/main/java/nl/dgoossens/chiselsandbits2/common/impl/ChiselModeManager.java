package nl.dgoossens.chiselsandbits2.common.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IngameGui;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.LongNBT;
import net.minecraft.nbt.StringNBT;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;
import nl.dgoossens.chiselsandbits2.api.*;
import nl.dgoossens.chiselsandbits2.common.bitstorage.BitStorageImpl;
import nl.dgoossens.chiselsandbits2.common.bitstorage.StorageCapabilityProvider;
import nl.dgoossens.chiselsandbits2.common.items.*;
import nl.dgoossens.chiselsandbits2.common.network.NetworkRouter;
import nl.dgoossens.chiselsandbits2.common.network.client.CSetItemModePacket;
import nl.dgoossens.chiselsandbits2.common.network.client.CSetMenuActionModePacket;
import nl.dgoossens.chiselsandbits2.common.network.server.SSynchronizeBitStoragePacket;

import java.awt.*;
import java.lang.reflect.Field;

import static nl.dgoossens.chiselsandbits2.api.ItemMode.*;

/**
 * The manager of which mode a tool is currently using.
 */
public class ChiselModeManager {
    /**
     * Set the main item mode of an itemstack.
     */
    public static void changeItemMode(final IItemMode newMode) {
        final CSetItemModePacket packet = new CSetItemModePacket(newMode);
        NetworkRouter.sendToServer(packet);

        //Show item mode change in hotbar
        if(packet.isValid(Minecraft.getInstance().player))
            reshowHighlightedStack();
    }

    /**
     * Set the menu action mode of an itemstack.
     * MenuAction#COLOURS and MenuAction#PLACE/MenuAction#SWAP
     * are accepted.
     */
    public static void changeMenuActionMode(final MenuAction newAction) {
        final CSetMenuActionModePacket packet = new CSetMenuActionModePacket(newAction);
        NetworkRouter.sendToServer(packet);

        //Show item mode change in hotbar
        if(packet.isValid(Minecraft.getInstance().player))
            reshowHighlightedStack();
    }

    /**
     * Reshows the highlighted stack item.
     */
    private static void reshowHighlightedStack() {
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

    /**
     * Updates the stack capability from the server to the client.
     */
    public static void updateStackCapability(final ItemStack item, final BitStorage cap, final PlayerEntity player) {
        NetworkRouter.sendTo(new SSynchronizeBitStoragePacket(cap, player.inventory.getSlotFor(item)), (ServerPlayerEntity) player);
    }

    /**
     * Scrolls through the current options and goes to the next one depending
     * on which direction was scrolled in.
     */
    public static void scrollOption(IItemMode currentMode, ItemStack item, final double dwheel) {
        if (!ChiselsAndBits2.getInstance().getConfig().enableModeScrolling.get()) return;
        if (currentMode instanceof ItemMode) {
            int offset = ((ItemMode) currentMode).ordinal();
            do {
                offset += (dwheel < 0 ? -1 : 1);
                if (offset >= values().length) offset = 0;
                if (offset < 0) offset = values().length - 1;
            } while (ItemMode.values()[offset].getType() != currentMode.getType());
            changeItemMode(ItemMode.values()[offset]);
        } else {
            IItemMode i = getMode(item);
            if(!(i instanceof SelectedItemMode)) return; //Just in case.
            SelectedItemMode current = ((SelectedItemMode) i);
            item.getCapability(StorageCapabilityProvider.STORAGE).ifPresent(c -> {
                BitStorageImpl bs = (BitStorageImpl) c;
                switch(currentMode.getType()) {
                    case SELECTED_BLOCK:
                    {
                        if(bs.listBlocks().size() <= 1) return; //You can't scroll without at least 2 elements.
                        int j = bs.getBlockIndex(current.getBlock());
                        j += (dwheel < 0 ? -1 : 1);
                        if(bs.listBlocks().size() <= j) j = 0;
                        if(j < 0) j = bs.listBlocks().size() - 1;
                        changeItemMode(SelectedItemMode.fromBlock(bs.getBlock(j)));
                    }
                        break;
                    case SELECTED_FLUID:
                    {
                        if(bs.listFluids().size() <= 1) return; //You can't scroll without at least 2 elements.
                        int j = bs.getFluidIndex(current.getFluid());
                        j += (dwheel < 0 ? -1 : 1);
                        if(bs.listFluids().size() <= j) j = 0;
                        if(j < 0) j = bs.listFluids().size() - 1;
                        changeItemMode(SelectedItemMode.fromFluid(bs.getFluid(j)));
                    }
                        break;
                    case SELECTED_BOOKMARK:
                    {
                        if(bs.listColours().size() <= 1) return; //You can't scroll without at least 2 elements.
                        int j = bs.listColours().indexOf(current.getColour());
                        j += (dwheel < 0 ? -1 : 1);
                        if(bs.listColours().size() <= j) j = 0;
                        if(j < 0) j = bs.listColours().size() - 1;
                        changeItemMode(SelectedItemMode.fromColour(bs.listColours().get(j)));
                    }
                        break;
                }
            });
        }
    }

    /**
     * Get the item mode being used by the item currently held by
     * the player.
     */
    public static IItemMode getMode(final PlayerEntity player) {
        final ItemStack ei = player.getHeldItemMainhand();
        return getMode(ei);
    }

    /**
     * Fetch the item mode associated with this item.
     * Type is required for the returned value when no
     * mode is found!
     */
    public static IItemMode getMode(final ItemStack stack) {
        final CompoundNBT nbt = stack.getTag();
        if (nbt != null && nbt.contains("mode")) {
            try {
                return resolveMode(nbt.getString("mode"), stack);
            } catch (final Exception x) {
                x.printStackTrace();
            }
        }

        return (stack.getItem() instanceof BitBagItem) ? SelectedItemMode.NONE_BAG :
                (stack.getItem() instanceof BitBeakerItem) ? SelectedItemMode.NONE_BEAKER :
                        (stack.getItem() instanceof PaletteItem) ? SelectedItemMode.NONE_BOOKMARK :
                                (stack.getItem() instanceof ChiselItem) ? CHISEL_SINGLE :
                                        (stack.getItem() instanceof PatternItem) ? PATTERN_REPLACE :
                                                (stack.getItem() instanceof TapeMeasureItem) ? TAPEMEASURE_BIT :
                                                        (stack.getItem() instanceof WrenchItem) ? WRENCH_ROTATE :
                                                                (stack.getItem() instanceof BlueprintItem) ? BLUEPRINT_UNKNOWN :
                                                                        MALLET_UNKNOWN;
    }

    /**
     * Get the selected item of an item stack, this item stack should be a BitStorage holder.
     */
    public static SelectedItemMode getSelectedItem(final ItemStack stack) {
        IItemMode ret = getMode(stack);
        if(ret instanceof SelectedItemMode)
            return (SelectedItemMode) ret;
        return null;
    }

    /**
     * If this stack is of a dynamic itemmode type this will return the timestamp when
     * the selection was made.
     * Used to determine what to place as the most recently selected item mode of a storage
     * item is used for placement regardless of the inventory slots.
     */
    public static long getSelectionTime(final ItemStack stack) {
        if(stack.getItem() instanceof IItemMenu) {
            final CompoundNBT nbt = stack.getTag();
            if (nbt != null && nbt.contains("timestamp"))
                return nbt.getLong("timestamp");
        }
        return 0L;
    }

    /**
     * Resolves an IItemMode from the output of
     * {@link IItemMode#getName()}
     */
    public static IItemMode resolveMode(final String name, final ItemStack item) {
        if(item != null && item.getItem() instanceof PaletteItem) {
            return name.equalsIgnoreCase("null") ? SelectedItemMode.NONE_BOOKMARK : SelectedItemMode.fromColour(new Color(Integer.valueOf(name), true));
        } else if(item != null && item.getItem() instanceof StorageItem) {
            return SelectedItemMode.fromName(name, item.getItem() instanceof BitBeakerItem);
        } else {
            try {
                return ItemMode.valueOf(name);
            } catch (final IllegalArgumentException il) {}
        }
        return CHISEL_SINGLE;
    }

    /**
     * Set the mode of this itemstack to this enum value.
     */
    public static void setMode(final ItemStack stack, final IItemMode mode) {
        if (stack != null) {
            stack.setTagInfo("mode", new StringNBT(mode.getName()));
            if(mode.getType().isDynamic())
                stack.setTagInfo("timestamp", new LongNBT(System.currentTimeMillis()));
        }
    }

    /**
     * Fetch the menu action associated to this item,
     * can be a colour or place/replace.
     */
    public static MenuAction getMenuActionMode(final ItemStack stack) {
        try {
            final CompoundNBT nbt = stack.getTag();
            if (nbt != null && nbt.contains("menuAction"))
                return MenuAction.valueOf(nbt.getString("menuAction"));
        } catch (final IllegalArgumentException iae) { //nope!
        } catch (final Exception e) {
            e.printStackTrace();
        }

        return (stack.getItem() instanceof TapeMeasureItem) ? MenuAction.WHITE :
                MenuAction.PLACE;
    }

    /**
     * Set the menu action mode of this itemstack to this enum value.
     */
    public static void setMenuActionMode(final ItemStack stack, final MenuAction action) {
        if (stack != null) stack.setTagInfo("menuAction", new StringNBT(action.name()));
    }
}
