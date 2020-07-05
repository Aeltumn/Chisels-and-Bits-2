package nl.dgoossens.chiselsandbits2.common.impl;

import net.minecraft.state.IProperty;
import nl.dgoossens.chiselsandbits2.api.ChiselsAndBitsAPI;
import nl.dgoossens.chiselsandbits2.api.bit.RestrictionAPI;
import nl.dgoossens.chiselsandbits2.api.voxel.VoxelManager;
import nl.dgoossens.chiselsandbits2.common.impl.voxel.VoxelManagerImpl;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class ChiselsAndBitsAPIImpl implements ChiselsAndBitsAPI {
    private RestrictionAPI restrictionAPI = new RestrictionAPIImpl();
    private VoxelManager voxelManager = new VoxelManagerImpl();
    private Set<IProperty<?>> ignoredProperties = new HashSet<>();

    @Override
    public RestrictionAPI getRestrictions() {
        return restrictionAPI;
    }

    @Override
    public Collection<IProperty<?>> getIgnoredBlockStates() {
        return ignoredProperties;
    }

    @Override
    public VoxelManager getVoxelManager() {
        return voxelManager;
    }

    @Override
    public void addIgnoredBlockState(IProperty<?> property) {
        ignoredProperties.add(property);
    }
}
