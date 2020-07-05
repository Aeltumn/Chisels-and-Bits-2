package nl.dgoossens.chiselsandbits2.common.network.client;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkEvent;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;
import nl.dgoossens.chiselsandbits2.api.bit.BitOperation;
import nl.dgoossens.chiselsandbits2.api.voxel.VoxelTile;
import nl.dgoossens.chiselsandbits2.common.blocks.ChiseledBlockTileEntity;
import nl.dgoossens.chiselsandbits2.api.iterator.BitIterator;
import nl.dgoossens.chiselsandbits2.api.voxel.VoxelBlob;
import nl.dgoossens.chiselsandbits2.common.util.InventoryUtils;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Executes the undo action.
 * Sent CLIENT -> SERVER.
 */
public class CUndoPacket {
    private BlockPos pos;
    private VoxelBlob before, after;
    private boolean redo; //true = redo, false = undo
    private int groupId;

    private CUndoPacket() {
    }

    public CUndoPacket(final BlockPos pos, final VoxelBlob before, final VoxelBlob after, final boolean redo, final int groupId) {
        this.pos = pos;
        this.after = after;
        this.before = before;
        this.redo = redo;
        this.groupId = groupId;
    }

    public void handle(final ServerPlayerEntity player) {
        if (!isValid(groupId))
            return; //If this group is invalid, don't even try.

        if (!inRange(player)) {
            player.sendStatusMessage(new TranslationTextComponent("general." + ChiselsAndBits2.MOD_ID + ".info.out_of_range"), true);
            invaliate();
            return;
        }

        try {
            final World world = player.getEntityWorld();
            final Optional<VoxelTile> tileOpt = ChiselsAndBits2.getInstance().getAPI().getVoxelManager().getVoxelTile(world, pos);
            if (tileOpt.isPresent()) {
                final VoxelBlob bbef = before;
                final VoxelBlob baft = after;

                //If before and after are equal we call it good enough.
                if (bbef.equals(baft))
                    return;

                final VoxelTile tile = tileOpt.get();
                final VoxelBlob vb = tile.getVoxelBlob();
                InventoryUtils.CalculatedInventory inventory = InventoryUtils.buildInventory(player);
                inventory.determineEffectState(world, pos);

                final BitIterator i = new BitIterator();
                while (i.hasNext()) {
                    final int inBef = i.getNext(bbef);
                    final int inAft = i.getNext(baft);

                    //If we need to set it to air and it currently isn't air
                    if (inBef != VoxelBlob.AIR_BIT && inAft == VoxelBlob.AIR_BIT) {
                        if (inventory.removeBit(vb, i.x, i.y, i.z)) {
                            player.sendStatusMessage(new TranslationTextComponent("general." + ChiselsAndBits2.MOD_ID + ".undo.missing_durability"), true);
                            invaliate();
                            return; //This will only be false if we have no more chisel in which case we can call it quits.
                        }
                    } else if (inBef != inAft) {
                        switch (inventory.placeBit(vb, i.x, i.y, i.z, BitOperation.SWAP, inAft)) {
                            case 2:
                                //If we don't have materials we quit.
                                player.sendStatusMessage(new TranslationTextComponent("general." + ChiselsAndBits2.MOD_ID + ".undo.missing_bits"), true);
                                invaliate();
                                return;
                            case 1: //If the chisel runs out of durability we can also quit.
                                player.sendStatusMessage(new TranslationTextComponent("general." + ChiselsAndBits2.MOD_ID + ".undo.missing_durability"), true);
                                invaliate();
                                return;
                        }
                    }
                }

                //Actually apply the operation.
                TileEntity te = world.getTileEntity(pos);
                if (!(te instanceof ChiseledBlockTileEntity)) {
                    world.setBlockState(pos, ChiselsAndBits2.getInstance().getRegister().CHISELED_BLOCK.get().getDefaultState(), 3);
                    te = world.getTileEntity(pos);
                }
                if (te instanceof ChiseledBlockTileEntity)
                    ((ChiseledBlockTileEntity) te).completeOperation(player, vb, false);

                inventory.playEffects(world, pos);
                inventory.apply();

            }
        } catch (Exception x) {
            x.printStackTrace();
            player.sendStatusMessage(new TranslationTextComponent("general." + ChiselsAndBits2.MOD_ID + ".undo.error_" + (redo ? "redo" : "undo")), true);
            invaliate();
        }
    }

    private boolean inRange(final PlayerEntity player) {
        double reach = player.isCreative() ? 32 : 6;
        return player.getDistanceSq(new Vec3d(pos.getX(), pos.getY(), pos.getZ())) < reach * reach;
    }

    public static void encode(CUndoPacket msg, PacketBuffer buf) {
        buf.writeBlockPos(msg.pos);
        final byte[] bef = msg.before.toByteArray();
        buf.writeVarInt(bef.length);
        buf.writeBytes(bef);
        final byte[] aft = msg.after.toByteArray();
        buf.writeVarInt(aft.length);
        buf.writeBytes(aft);
        buf.writeBoolean(msg.redo);
        buf.writeVarInt(msg.groupId);
    }

    public static CUndoPacket decode(PacketBuffer buffer) {
        CUndoPacket pc = new CUndoPacket();
        pc.pos = buffer.readBlockPos();
        final int lena = buffer.readVarInt();
        final byte[] ta = new byte[lena];
        buffer.readBytes(ta);
        final int lenb = buffer.readVarInt();
        final byte[] tb = new byte[lenb];
        buffer.readBytes(tb);

        pc.before = VoxelBlob.readFromBytes(ta);
        pc.after = VoxelBlob.readFromBytes(tb);

        pc.redo = buffer.readBoolean();
        pc.groupId = buffer.readVarInt();
        return pc;
    }

    public static void handle(final CUndoPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> pkt.handle(ctx.get().getSender()));
        ctx.get().setPacketHandled(true);
    }

    //--- STATIC GROUP ID MANAGEMENT ---
    private static int latestGroupId = 0;
    private static boolean didFail = false;

    /**
     * Starts a new group of packets and gets their id.
     * This method is only going to get called on the client side.
     */
    public static int nextGroupId() {
        return latestGroupId + 1;
    }

    /**
     * Checks if a group id is still valid.
     * This method will only be called on the server side.
     */
    public static boolean isValid(int groupId) {
        if (groupId == latestGroupId) {
            //Is this the same group we've had previously?
            return !didFail; //Return if this group has failed or not
        }
        //This is a new group, update the group id and give this group a fresh chance.
        latestGroupId = groupId;
        didFail = false;
        return true;
    }

    /**
     * Invalidates the current group, it has failed.
     */
    public static void invaliate() {
        System.out.println("THEY FAILED LOL");
        didFail = true;
    }
}
