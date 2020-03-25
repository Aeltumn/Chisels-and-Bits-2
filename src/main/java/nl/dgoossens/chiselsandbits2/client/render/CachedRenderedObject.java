package nl.dgoossens.chiselsandbits2.client.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import nl.dgoossens.chiselsandbits2.api.bit.BitLocation;
import nl.dgoossens.chiselsandbits2.api.item.IItemMode;
import nl.dgoossens.chiselsandbits2.client.util.ClientItemPropertyUtil;
import nl.dgoossens.chiselsandbits2.common.blocks.ChiseledBlockTileEntity;
import nl.dgoossens.chiselsandbits2.common.impl.item.PlayerItemMode;

public abstract class CachedRenderedObject {
    //Fields that we validate to see if we need to rebuild
    private BitLocation location; //Block being looked at
    private int heldSlot; //Held item
    private BlockState state; //State of target block
    private IItemMode mode; //Mode of item
    private Direction face; //Targeted face
    private long previousTileIteration = -Long.MAX_VALUE; //Iteration of tile looked at
    private boolean isEmpty; //Whether or not we have built a cached object yet
    private boolean crouching; //Whether the player is crouching

    public CachedRenderedObject(ItemStack item, PlayerEntity player, BitLocation location, Direction face, IItemMode mode) {
        if(item.isEmpty()) {
            isEmpty = true;
            return;
        }

        this.location = location;
        this.heldSlot = player.inventory.currentItem;
        this.state = player.world.getBlockState(location.blockPos);
        this.mode = mode;
        this.face = face;
        this.isEmpty = false; //We've successfully built the object
        this.crouching = player.isCrouching();

        final TileEntity te = player.world.getTileEntity(location.blockPos);
        if(te instanceof ChiseledBlockTileEntity)
            previousTileIteration = ((ChiseledBlockTileEntity) te).getIteration();
    }

    public Direction getFace() {
        return face;
    }

    public IItemMode getMode() {
        return mode;
    }

    public BitLocation getLocation() {
        return location;
    }

    public boolean isEmpty() {
        return isEmpty;
    }

    public void invalidate() {
        isEmpty = true;
    }

    /**
     * Returns whether or not this object is still valid if we have new inputs.
     */
    public boolean isValid(PlayerEntity player, BitLocation pos, Direction face, IItemMode mode) {
        return !isEmpty && heldSlot == player.inventory.currentItem && this.mode.equals(mode) && this.location.equals(pos) && player.isCrouching() == this.crouching && face == this.face && player.world.getBlockState(pos.blockPos).equals(state) && !didTileChange(player.world.getTileEntity(pos.blockPos));
    }

    /**
     * Determines if there is a difference between te and previousTile.
     */
    public boolean didTileChange(final TileEntity te) {
        if(te == null && previousTileIteration == -Long.MAX_VALUE) return false; //Both null? Same.
        if(te == null || previousTileIteration == -Long.MAX_VALUE) return true; //Not both not null? Different!
        if(te instanceof ChiseledBlockTileEntity) {
            final ChiseledBlockTileEntity newTile = (ChiseledBlockTileEntity) te;
            return newTile.getIteration() != previousTileIteration;
        }
        return true; //It changed if it isn't a chiseled block anymore.
    }

    /**
     * Render this cached object.
     */
    public abstract void render(MatrixStack matrix, IRenderTypeBuffer buffer, float partialTicks);
}
