package nl.dgoossens.chiselsandbits2.common.chiseledblock.iterators;

import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import nl.dgoossens.chiselsandbits2.common.chiseledblock.voxel.VoxelBlob;

/**
 * An iterator that goes through each bit in a 16x16x16 voxel blob.
 */
public class BitIterator {
    public final static float ONE_16_TH = 1.0f / VoxelBlob.DIMENSION;
    private int bit;
    private boolean done;
    public int x, y, z;

    public boolean hasNext() {
        if(done) return false;
        x++;
        if(x >= VoxelBlob.DIMENSION) {
            x = 0;
            y++;
            if(y >= VoxelBlob.DIMENSION) {
                y = 0;
                z++;
                if(z >= VoxelBlob.DIMENSION) {
                    done = true;
                    return false;
                }
            }
        }
        bit = x | y << 4 | z << 8;
        return true;
    }

    /**
     * Get the shape of the currently selected bit.
     */
    public VoxelShape getShape() {
        return VoxelShapes.create(ONE_16_TH * x, ONE_16_TH * y, ONE_16_TH * z, ONE_16_TH * (x + 1), ONE_16_TH * (y + 1), ONE_16_TH * (z + 1));
    }

    public int getNext(final VoxelBlob blob) {
        return blob.getIndex(bit);
    }

    public void setNext(final VoxelBlob blob, final int value) {
        blob.setIndex(bit, value);
    }
}
