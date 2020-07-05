package nl.dgoossens.chiselsandbits2.common.bitstorage;

import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;
import nl.dgoossens.chiselsandbits2.api.bit.BitStorage;
import nl.dgoossens.chiselsandbits2.api.bit.VoxelWrapper;

public class BagContainer extends Container {
    private Inventory fakeInventory = new Inventory(1);
    private Inventory bagInventory;
    private ItemStack item;
    private int slotCount;
    private Slot inputSlot;

    public BagContainer(int id, PlayerInventory playerInventory, PacketBuffer data) {
        this(id, playerInventory.player, new Inventory(Math.min( //Take occupied + 1 up to maximum. So you always have a slot to put it into the bag.
                playerInventory.player.getHeldItemMainhand().getCapability(StorageCapabilityProvider.STORAGE).map(BitStorage::getMaximumSlots).orElse(0),
                playerInventory.player.getHeldItemMainhand().getCapability(StorageCapabilityProvider.STORAGE).map(BitStorage::getOccupiedSlotCount).orElse(0) + 1
        )), playerInventory.player.getHeldItemMainhand());
    }

    public BagContainer(int id, PlayerEntity player, Inventory bagInventory, ItemStack item) {
        super(ChiselsAndBits2.getInstance().getRegister().BIT_BAG_CONTAINER.get(), id);
        this.bagInventory = bagInventory;
        this.item = item;
        this.slotCount = bagInventory.getSizeInventory();

        //Build inventory contents
        updateInventoryContents();

        bagInventory.openInventory(player);
        int numRows = getRowCount();
        int i = (numRows - 4) * 18;

        for (int j = 0; j < numRows; ++j) {
            for (int k = 0; k < ((slotCount - j * 9) >= 9 ? 9 : slotCount % 9); ++k)
                this.addSlot(new ReadonlySlot(bagInventory, k + j * 9, 8 + k * 18, 18 + j * 18));
        }

        for (int l = 0; l < 3; ++l) {
            for (int j1 = 0; j1 < 9; ++j1)
                this.addSlot(new Slot(player.inventory, j1 + l * 9 + 9, 8 + j1 * 18, 103 + l * 18 + i));
        }

        for (int i1 = 0; i1 < 9; ++i1)
            this.addSlot(new Slot(player.inventory, i1, 8 + i1 * 18, 161 + i));

        //Input slot
        inputSlot = new InsertSlot(fakeInventory, 0, -17, 21, item, this::updateInventoryContents);
        this.addSlot(inputSlot);
    }

    public Slot getInputSlot() {
        return inputSlot;
    }

    public void updateInventoryContents() {
        final BitStorage store = item.getCapability(StorageCapabilityProvider.STORAGE).orElse(null);
        if (store == null) return;
        for (int i = 0; i < bagInventory.getSizeInventory(); i++) {
            ItemStack s = store.getSlotContent(i).getStack();
            s.setCount(Math.max(1, (int) (store.get(VoxelWrapper.forBlock(Block.getBlockFromItem(s.getItem()))) / 4096.0d)));
            bagInventory.setInventorySlotContents(i, s);
        }
    }

    public Inventory getBagInventory() {
        return bagInventory;
    }

    public ItemStack getBag() {
        return item;
    }

    public int getRowCount() {
        return (slotCount / 9 + (slotCount % 9 != 0 ? 1 : 0));
    }

    public int getSlotCount() {
        return slotCount;
    }

    @Override
    public boolean canInteractWith(PlayerEntity playerIn) {
        return true;
    }

    @Override
    public ItemStack transferStackInSlot(PlayerEntity playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);
        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();
            if (index < this.bagInventory.getSizeInventory()) {
                if (!this.mergeItemStack(itemstack1, this.bagInventory.getSizeInventory(), this.inventorySlots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.mergeItemStack(itemstack1, 0, this.bagInventory.getSizeInventory(), false)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }
        }

        return itemstack;
    }

    @Override
    public void onContainerClosed(PlayerEntity playerIn) {
        super.onContainerClosed(playerIn);
        bagInventory.closeInventory(playerIn);
    }
}
