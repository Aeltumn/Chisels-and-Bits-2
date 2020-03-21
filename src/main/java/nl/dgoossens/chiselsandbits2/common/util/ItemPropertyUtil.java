package nl.dgoossens.chiselsandbits2.common.util;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.LongNBT;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;
import nl.dgoossens.chiselsandbits2.api.item.*;
import nl.dgoossens.chiselsandbits2.api.bit.BitStorage;
import nl.dgoossens.chiselsandbits2.api.bit.VoxelWrapper;
import nl.dgoossens.chiselsandbits2.api.cache.CacheClearable;
import nl.dgoossens.chiselsandbits2.api.cache.CacheType;
import nl.dgoossens.chiselsandbits2.client.util.ClientItemPropertyUtil;
import nl.dgoossens.chiselsandbits2.common.bitstorage.StorageCapabilityProvider;
import nl.dgoossens.chiselsandbits2.common.chiseledblock.voxel.VoxelBlob;
import nl.dgoossens.chiselsandbits2.common.impl.item.ItemMode;
import nl.dgoossens.chiselsandbits2.common.impl.item.ItemModeType;
import nl.dgoossens.chiselsandbits2.common.impl.item.PlayerItemMode;
import nl.dgoossens.chiselsandbits2.common.items.*;
import nl.dgoossens.chiselsandbits2.common.network.client.CItemModePacket;
import nl.dgoossens.chiselsandbits2.common.network.client.CVoxelWrapperPacket;
import nl.dgoossens.chiselsandbits2.common.network.server.SSynchronizeBitStoragePacket;

import java.util.*;

/**
 * Utils for managing which item modes are being used by the player.
 */
public class ItemPropertyUtil implements CacheClearable {
    private static Map<UUID, VoxelWrapper> selected;

    static {
        CacheType.ROUTINE.register(new ItemPropertyUtil());
    }

    public ItemPropertyUtil() {
        if(selected != null) throw new RuntimeException("Can't initialise ItemModeUtil twice!");
        selected = new HashMap<>();
    }

    /**
     * Updates the stack capability from the server to the client.
     */
    public static void updateStackCapability(final ItemStack item, final BitStorage cap, final ServerPlayerEntity player) {
        if(player.getEntityWorld().isRemote)
            throw new UnsupportedOperationException("Can't update stack capability from client side!");
        validateSelectedVoxelWrapper(player, item);
        ChiselsAndBits2.getInstance().getNetworkRouter().sendTo(new SSynchronizeBitStoragePacket(cap, UselessUtil.getSlotFor(player.inventory, item)), player);
    }

    /**
     * Validate that the bit storage in the given item for the player still has positive amounts of contents in each slot.
     */
    public static void validateSelectedVoxelWrapper(final PlayerEntity player, final ItemStack item) {
        if(player.getEntityWorld().isRemote)
            throw new UnsupportedOperationException("Can't validate selected type from client side!");
        if(!(item.getItem() instanceof StorageItem))
            return;

        //Check if selected type is no longer valid
        VoxelWrapper selected = getSelectedVoxelWrapper(item);
        if(selected.isEmpty()) return;
        BitStorage storage = item.getCapability(StorageCapabilityProvider.STORAGE).orElse(null);
        if(storage == null) return;
        if(storage.get(selected) <= 0)
            setSelectedVoxelWrapper(player, item, VoxelWrapper.empty(), true);
    }

    /**
     * Set the voxel wapper for a StorageItem
     */
    public static void setSelectedVoxelWrapper(final PlayerEntity player, final ItemStack item, final VoxelWrapper w, final boolean updateTimestamp) {
        if(player.world.isRemote) {
            if(!item.equals(player.getHeldItemMainhand(), false))
                throw new UnsupportedOperationException("Can't set voxel wrapper for different item on client!");

            ChiselsAndBits2.getInstance().getNetworkRouter().sendToServer(new CVoxelWrapperPacket(w, updateTimestamp));
            return;
        }

        if(item.getItem() instanceof StorageItem) {
            //Check if this storage item at least contains this wrapper
            if(w.isEmpty() || item.getCapability(StorageCapabilityProvider.STORAGE).map(f -> f.get(w) > 0).orElse(false)) {
                ((StorageItem) item.getItem()).setSelected(player, item, w);
                if(updateTimestamp) item.setTagInfo("timestamp", LongNBT.valueOf(w.isEmpty() ? 0 : System.currentTimeMillis())); //Update timestamp (make sure empty will never be first)
            }
        } else if(item.getItem() instanceof MorphingBitItem) {
            ((MorphingBitItem) item.getItem()).setSelected(player, item, w);
        }
    }

    /**
     * Set the mode of this itemstack to this enum value.
     */
    public static void setItemMode(final PlayerEntity player, final ItemStack stack, final IItemMode mode) {
        if(player.world.isRemote) {
            if (mode instanceof ItemMode && mode.getType() == ItemModeType.CHISELED_BLOCK) {
                ClientItemPropertyUtil.setChiseledBlockMode((PlayerItemMode) mode);
                return;
            }

            if(!stack.equals(player.getHeldItemMainhand(), false))
                throw new UnsupportedOperationException("Can't set item mode for different item on client!");

            ChiselsAndBits2.getInstance().getNetworkRouter().sendToServer(new CItemModePacket(mode));
            return;
        }

        if (mode instanceof ItemMode && mode.getType() == ItemModeType.CHISELED_BLOCK)
            throw new UnsupportedOperationException("Can't set chiseled block item mode on the server side!");

        if (stack == null) return;
        if (stack.getItem() instanceof TypedItem) {
            TypedItem it = (TypedItem) stack.getItem();
            if(!mode.getType().equals(it.getAssociatedType())) return;
            it.setSelectedMode(player, stack, mode);
            selected.remove(player.getUniqueID());
        }
    }

    /**
     * Checks if the item mode of the provided stack is contained in the list of valid modes.
     */
    public static boolean isItemMode(final ItemStack stack, final ItemMode... modes) {
        if(!(stack.getItem() instanceof TypedItem)) return false;

        final IItemMode res = ((TypedItem) stack.getItem()).getSelectedMode(stack);
        if(res == null) return false;
        for(final ItemMode i : modes) {
            if(i.equals(res)) return true;
        }
        return false;
    }

    /**
     * Get the timestamp at whih the last selection was made for this item.
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
     * Get the selected voxel wrapper of an item stack if it has any.
     */
    public static VoxelWrapper getSelectedVoxelWrapper(final ItemStack stack) {
        if(stack.getItem() instanceof StorageItem)
            return ((StorageItem) stack.getItem()).getSelected(stack);
        if(stack.getItem() instanceof MorphingBitItem)
            return ((MorphingBitItem) stack.getItem()).getSelected(stack);
        return VoxelWrapper.empty();
    }

    /**
     * Force recalculates this client player's selected bit type.
     */
    public static void recalculateGlobalSelectedVoxelWrapper(final PlayerEntity player) {
        selected.remove(player.getUniqueID());
    }

    /**
     * Gets the currently selected bit type as a selected item mode for the main
     * player. Client-side only!
     */
    public static VoxelWrapper getGlobalSelectedVoxelWrapper() {
        return getGlobalSelectedVoxelWrapper(ChiselsAndBits2.getInstance().getClient().getPlayer());
    }

    /**
     * Gets the currently selected bit type as a selected item mode.
     */
    public static VoxelWrapper getGlobalSelectedVoxelWrapper(final PlayerEntity player) {
        if(!selected.containsKey(player.getUniqueID())) {
            long stamp = -1;
            VoxelWrapper res = selected.getOrDefault(player.getUniqueID(), VoxelWrapper.empty());

            //Scan all storage containers for the most recently selected one.
            for (ItemStack item : player.inventory.mainInventory) {
                if (item.getItem() instanceof StorageItem) {
                    VoxelWrapper w = ((StorageItem) item.getItem()).getSelected(item);
                    if(w.isEmpty()) continue; //Ignore empty selections
                    long l = ItemPropertyUtil.getSelectionTime(item);
                    if (l > stamp) {
                        stamp = l;
                        if(w.getId() != VoxelBlob.AIR_BIT)
                            res = w;
                    }
                }
            }
            selected.put(player.getUniqueID(), res);
        }
        return selected.getOrDefault(player.getUniqueID(), VoxelWrapper.empty());
    }

    /**
     * Get the timestamp the bit storage has that is currently being used to place bits.
     */
    public static long getHighestSelectionTimestamp(final PlayerEntity player) {
        long stamp = -1;

        for (ItemStack item : player.inventory.mainInventory) {
            if (item.getItem() instanceof StorageItem) {
                VoxelWrapper w = ((StorageItem) item.getItem()).getSelected(item);
                if(w.isEmpty()) continue; //Ignore empty selections

                long l = getSelectionTime(item);
                if (l > stamp) stamp = l;
            }
        }
        return stamp;
    }

    /**
     * Gets the currently selected bit storage item slot.
     */
    public static int getGlobalSelectedVoxelWrapperSlot(final PlayerEntity player) {
        long stamp = -1; //Start at -1 so even 0 is valid.
        int res = -1;

        for (int i = 0; i < player.inventory.mainInventory.size(); i++) {
            ItemStack item = player.inventory.mainInventory.get(i);
            if (item.getItem() instanceof StorageItem) {
                VoxelWrapper w = ((StorageItem) item.getItem()).getSelected(item);
                if(w.isEmpty()) continue; //Ignore empty selections

                long l = ItemPropertyUtil.getSelectionTime(item);
                if (l > stamp) {
                    stamp = l;
                    res = i;
                }
            }
        }
        return res;
    }

    @Override
    public void clearCache() {
        //Remove the player from the cache.
        if(FMLEnvironment.dist == Dist.DEDICATED_SERVER) {
            //Server
            for(ServerWorld w : ServerLifecycleHooks.getCurrentServer().getWorlds()) {
                for(ServerPlayerEntity p : w.getPlayers())
                    selected.remove(p.getUniqueID());
            }
        } else {
            //LAN/Client
            PlayerEntity player = ChiselsAndBits2.getInstance().getClient().getPlayer();
            if(player != null) {
                selected.remove(player.getUniqueID());
                //We'll settle for everyone in the same dimension. Doubt anyone will ever notice this.
                for(PlayerEntity p : player.getEntityWorld().getPlayers())
                    selected.remove(p.getUniqueID());
            }
        }
    }
}
