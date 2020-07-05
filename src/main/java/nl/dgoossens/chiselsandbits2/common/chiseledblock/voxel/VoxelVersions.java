package nl.dgoossens.chiselsandbits2.common.chiseledblock.voxel;

import nl.dgoossens.chiselsandbits2.common.chiseledblock.serialization.BlobSerializer;
import nl.dgoossens.chiselsandbits2.common.chiseledblock.serialization.LegacyBlobSerializer;

public enum VoxelVersions {
    ANY(-1),
    LEGACY(-42, LegacyBlobSerializer.class), //For compatibility with C&B-WorldFixer
    COMPACT(1, BlobSerializer.class),
    ;

    private int id;
    private Class<? extends VoxelSerializer> worker;

    VoxelVersions(int id) {
        this(id, null);
    }

    VoxelVersions(int id, Class<? extends VoxelSerializer> klass) {
        this.id = id;
        this.worker = klass;
    }

    /**
     * Get the current default voxel verison.
     */
    public static int getDefault() {
        return COMPACT.getId();
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
     * Get the object that does all the work, the voxel serializer.
     */
    public VoxelSerializer getWorker() throws Exception {
        return worker != null ? worker.newInstance() : null;
    }
}
