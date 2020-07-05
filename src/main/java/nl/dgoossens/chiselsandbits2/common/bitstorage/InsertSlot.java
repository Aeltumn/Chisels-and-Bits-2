package nl.dgoossens.chiselsandbits2.common.bitstorage;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.thread.SidedThreadGroups;
import nl.dgoossens.chiselsandbits2.api.bit.VoxelWrapper;
import nl.dgoossens.chiselsandbits2.api.item.attributes.IVoxelStorer;
import nl.dgoossens.chiselsandbits2.common.chiseledblock.voxel.VoxelBlob;
import nl.dgoossens.chiselsandbits2.common.items.ChiseledBlockItem;

import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

public class InsertSlot extends Slot {
    private ItemStack item;
    private Runnable update;

    public InsertSlot(final IInventory inv, final int index, final int xPos, final int yPos, final ItemStack item, final Runnable update) {
        super(inv, index, xPos, yPos);
        this.item = item;
        this.update = update;
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        return stack.getItem() instanceof ChiseledBlockItem;
    }

    @Override
    public void onSlotChanged() {
        //Only run on server
        if (Thread.currentThread().getThreadGroup() != SidedThreadGroups.SERVER) return;

        //Put the contents into the inventory
        if (getHasStack()) {
            ItemStack block = getStack();
            if (block.getItem() instanceof IVoxelStorer) {
                IVoxelStorer voxel = (IVoxelStorer) block.getItem();
                VoxelBlob vb = voxel.getVoxelBlob(block);
                item.getCapability(StorageCapabilityProvider.STORAGE).ifPresent(cap -> {
                    Map<Integer, LongAdder> blocks = vb.getBlockSums();
                    for (int bitType : blocks.keySet()) {
                        VoxelWrapper w = VoxelWrapper.forAbstract(bitType);
                        int slot = cap.findSlot(w);
                        if (slot == -1) continue; //Can't remove this bit type if we have no space to put it
                        long queryRoom = cap.queryRoom(w);
                        if (queryRoom < blocks.get(bitType).longValue()) {
                            //Not enough room to deposit all bits
                            vb.removeBitType(bitType, queryRoom);
                            cap.add(w, queryRoom);
                        } else {
                            //We have enough room so remove this type from the voxelblob
                            vb.removeBitType(bitType); //Remove all of this bit type
                            cap.add(w, blocks.get(bitType).longValue());
                        }
                    }
                });
                //If the vb is empty we remove the item from the slot
                if (vb.filled() <= 0)
                    putStack(ItemStack.EMPTY);
                else {
                    voxel.setVoxelBlob(block, vb);
                    putStack(block);
                }

                update.run();
            }
        }
    }
}
