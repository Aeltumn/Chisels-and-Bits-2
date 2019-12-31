package nl.dgoossens.chiselsandbits2.common.items;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;
import nl.dgoossens.chiselsandbits2.api.bit.BitStorage;
import nl.dgoossens.chiselsandbits2.api.bit.VoxelWrapper;
import nl.dgoossens.chiselsandbits2.api.item.IItemMenu;
import nl.dgoossens.chiselsandbits2.api.item.attributes.IItemScrollWheel;
import nl.dgoossens.chiselsandbits2.api.item.attributes.IPropertyOwner;
import nl.dgoossens.chiselsandbits2.api.item.property.SelectedProperty;
import nl.dgoossens.chiselsandbits2.common.chiseledblock.voxel.VoxelBlob;
import nl.dgoossens.chiselsandbits2.common.bitstorage.StorageCapabilityProvider;
import nl.dgoossens.chiselsandbits2.common.utils.ItemPropertyUtil;
import nl.dgoossens.chiselsandbits2.common.registry.ModItemGroups;

public abstract class StorageItem extends Item implements IItemScrollWheel, IPropertyOwner {
    protected int PROPERTY_SELECTED;
    public StorageItem() {
        super(new Item.Properties().maxStackSize(1).group(ModItemGroups.CHISELS_AND_BITS2));

        PROPERTY_SELECTED = addProperty(new SelectedProperty(() -> VoxelWrapper.forAbstract(VoxelBlob.AIR_BIT)));
    }

    /**
     * Get the selected mode for this item.
     */
    public VoxelWrapper getSelected(final ItemStack stack) {
        return getProperty(PROPERTY_SELECTED, VoxelWrapper.class).get(stack);
    }

    /**
     * Set the selected item for this storage.
     */
    public void setSelected(final World world, final ItemStack stack, final VoxelWrapper w) {
        getProperty(PROPERTY_SELECTED, VoxelWrapper.class).set(world, stack, w);
    }

    @Override
    public ICapabilityProvider initCapabilities(final ItemStack stack, final CompoundNBT nbt) {
        return new StorageCapabilityProvider();
    }

    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        return !getSelected(stack).isEmpty() && ChiselsAndBits2.getInstance().getConfig().showBitsAvailableAsDurability.get();
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        VoxelWrapper s = getSelected(stack);
        if(s.isEmpty()) return 0;
        BitStorage store = stack.getCapability(StorageCapabilityProvider.STORAGE).orElse(null);
        if(store == null) return 0;
        return (double) (ChiselsAndBits2.getInstance().getConfig().bitsPerTypeSlot.get() - store.get(s)) / (double) ChiselsAndBits2.getInstance().getConfig().bitsPerTypeSlot.get();
    }

    /**
     * Display the mode in the highlight tip. (and color for tape measure)
     */
    @Override
    public String getHighlightTip(ItemStack item, String displayName) {
        VoxelWrapper im = getSelected(item);
        if(im.isEmpty()) return displayName;
        return displayName + " - " + im.getDisplayName();
    }

    /**
     * Scrolling on the chisel scrolls through the possible modes, alternative to the menu.
     */
    @Override
    public boolean scroll(final PlayerEntity player, final ItemStack stack, final double dwheel) {
        if (!ChiselsAndBits2.getInstance().getConfig().enableModeScrolling.get()) return false;

        VoxelWrapper wrapper = getSelected(stack);
        stack.getCapability(StorageCapabilityProvider.STORAGE).ifPresent(bs -> {
            if(bs.getOccupiedSlotCount() <= 1) return; //You can't scroll without at least 2 elements.
            int j = bs.getSlot(wrapper);
            j += (dwheel < 0 ? -1 : 1);
            if(bs.getOccupiedSlotCount() <= j) j = 0;
            if(j < 0) j = bs.getOccupiedSlotCount() - 1;

            getProperty(PROPERTY_SELECTED, VoxelWrapper.class).set(player.world, stack, bs.getSlotContent(j));
        });
        return true;
    }
}
