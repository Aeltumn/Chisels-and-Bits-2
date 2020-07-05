package nl.dgoossens.chiselsandbits2.common.impl;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.IProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import nl.dgoossens.chiselsandbits2.api.ChiselsAndBitsAPI;
import nl.dgoossens.chiselsandbits2.api.bit.RestrictionAPI;
import nl.dgoossens.chiselsandbits2.api.block.BitAccess;
import nl.dgoossens.chiselsandbits2.common.impl.voxel.BitAccessImpl;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class ChiselsAndBitsAPIImpl implements ChiselsAndBitsAPI {
    private RestrictionAPI restrictionAPI = new RestrictionAPIImpl();
    private Set<IProperty<?>> ignoredProperties = new HashSet<>();

    @Override
    public Optional<BitAccess> getBitAccess(PlayerEntity player, World world, BlockPos pos) {
        BitAccess ba = new BitAccessImpl(player, world, pos);
        if (ba.getNativeBlob() == null) return Optional.empty();
        return Optional.of(ba);
    }

    @Override
    public RestrictionAPI getRestrictions() {
        return restrictionAPI;
    }

    @Override
    public Collection<IProperty<?>> getIgnoredBlockStates() {
        return ignoredProperties;
    }

    @Override
    public void addIgnoredBlockState(IProperty<?> property) {
        ignoredProperties.add(property);
    }
}
