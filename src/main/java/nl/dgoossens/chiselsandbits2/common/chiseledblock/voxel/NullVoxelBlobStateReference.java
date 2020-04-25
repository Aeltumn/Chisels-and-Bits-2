package nl.dgoossens.chiselsandbits2.common.chiseledblock.voxel;

/**
 * A null reference that returns the {@link VoxelBlob#NULL_BLOB}.
 */
public class NullVoxelBlobStateReference extends VoxelBlobStateReference {
    /**
     * The null voxel blob reference that references an empty air
     * blob.
     */
    public final static VoxelBlobStateReference NULL_REFERENCE = new NullVoxelBlobStateReference();

    NullVoxelBlobStateReference() {
        super(new VoxelBlobStateInstance(new byte[0]));
    }

    @Override
    public VoxelBlob getVoxelBlob() {
        return VoxelBlob.NULL_BLOB;
    }

    @Override
    public byte[] getByteArray() {
        return new byte[0];
    }
}
