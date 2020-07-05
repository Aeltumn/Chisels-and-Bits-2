package nl.dgoossens.chiselsandbits2.api.iterator;

import nl.dgoossens.chiselsandbits2.api.voxel.VoxelBlob;

/**
 * An iterator that goes through each bit in a 16x16x16 voxel blob.
 */
public final class BitIterator {
    private boolean done;
    public int x = -1, y, z;
    public int bit;

    public boolean hasNext() {
        if (done) return false;
        x++;
        if (x >= VoxelBlob.DIMENSION) {
            x = 0;
            y++;
            if (y >= VoxelBlob.DIMENSION) {
                y = 0;
                z++;
                if (z >= VoxelBlob.DIMENSION) {
                    done = true;
                    return false;
                }
            }
        }
        bit = x | y << 4 | z << 8;
        return true;
    }

    public int getNext(final VoxelBlob blob) {
        return blob.getIndex(bit);
    }

    public void setNext(final VoxelBlob blob, final int value) {
        blob.setIndex(bit, value);
    }
}
