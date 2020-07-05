package nl.dgoossens.chiselsandbits2.api.voxel;

import nl.dgoossens.chiselsandbits2.common.impl.serialization.BlobSerializer;
import nl.dgoossens.chiselsandbits2.common.impl.serialization.LegacyBlobSerializer;

/**
 * The various versions of the format that is used to store voxel data.
 * If any data is loaded in an older format it is immediately updated to
 * the default format version.
 */
public enum VoxelVersions {
    ANY(-1),
    LEGACY(-42, LegacyBlobSerializer.class), //For compatibility with C&B-WorldFixer
    COMPACT(1, BlobSerializer.class),
    ;

    private final int id;
    private final Class<? extends VoxelSerializer> serializerClass;

    VoxelVersions(int id) {
        this(id, null);
    }
    VoxelVersions(int id, Class<? extends VoxelSerializer> serializerClass) {
        this.id = id;
        this.serializerClass = serializerClass;
    }

    /**
     * Get the version id for the default version.
     */
    public static int getDefaultId() {
        return getDefault().getId();
    }

    /**
     * Get the default voxel version.
     */
    public static VoxelVersions getDefault() {
        return COMPACT;
    }

    /**
     * Get the voxel version using the id specified.
     */
    public static VoxelVersions getVersion(int i) {
        for (VoxelVersions vv : VoxelVersions.values()) {
            if (vv.id == i) return vv;
        }
        return ANY;
    }

    /**
     * Get this versions id.
     */
    public int getId() {
        return id;
    }

    /**
     * Get the class of the serializer in charge of converting to
     * and from this format.
     */
    public VoxelSerializer getSerializerClass() throws Exception {
        return serializerClass != null ? serializerClass.newInstance() : null;
    }
}
