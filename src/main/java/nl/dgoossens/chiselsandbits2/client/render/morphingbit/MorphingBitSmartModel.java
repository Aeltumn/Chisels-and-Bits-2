package nl.dgoossens.chiselsandbits2.client.render.morphingbit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import nl.dgoossens.chiselsandbits2.client.render.model.BaseSmartModel;
import nl.dgoossens.chiselsandbits2.common.chiseledblock.voxel.VoxelBlob;
import nl.dgoossens.chiselsandbits2.common.items.MorphingBitItem;
import nl.dgoossens.chiselsandbits2.common.util.ItemPropertyUtil;

import java.util.concurrent.TimeUnit;

public class MorphingBitSmartModel extends BaseSmartModel {
    private static final LoadingCache<Integer, MorphingBitBaked> cache = CacheBuilder.newBuilder()
            .maximumSize(2000)
            .expireAfterAccess(5, TimeUnit.SECONDS)
            .build(new CacheLoader<Integer, MorphingBitBaked>() {
                @Override
                public MorphingBitBaked load(Integer key) {
                    return new MorphingBitBaked(key, false);
                }
            });
    private static final LoadingCache<Integer, MorphingBitBaked> bigCache = CacheBuilder.newBuilder()
            .maximumSize(2000)
            .expireAfterAccess(5, TimeUnit.SECONDS)
            .build(new CacheLoader<Integer, MorphingBitBaked>() {
                @Override
                public MorphingBitBaked load(Integer key) {
                    return new MorphingBitBaked(key, true);
                }
            });

    /**
     * Get the morphing bit model for a given selected bit type, if bit equals {@link nl.dgoossens.chiselsandbits2.common.chiseledblock.voxel.VoxelBlob#AIR_BIT} then
     * the gray default will be returned.
     */
    public static MorphingBitBaked getCachedModel(final int bit, final boolean big) {
        try {
            if(big) return bigCache.get(bit);
            else return cache.get(bit);
        } catch(Exception x) {
            x.printStackTrace();
            return new MorphingBitBaked(bit, false);
        }
    }

    @Override
    public IBakedModel handleItemState(IBakedModel originalModel, ItemStack stack, World world, LivingEntity entity) {
        if(!(stack.getItem() instanceof MorphingBitItem)) return getCachedModel(VoxelBlob.AIR_BIT, false);
        MorphingBitItem mbi = (MorphingBitItem) stack.getItem();
        if(mbi.isLocked(stack))
            return getCachedModel(mbi.getSelected(stack).getId(), (entity != null && entity.isSneaking()) || KeyModifier.SHIFT.isActive(KeyConflictContext.GUI));
        return getCachedModel(entity instanceof PlayerEntity ? ItemPropertyUtil.getGlobalSelectedVoxelWrapper((PlayerEntity) entity).getId() : world == null || world.isRemote ? ItemPropertyUtil.getGlobalSelectedVoxelWrapper().getId() : VoxelBlob.AIR_BIT, false);
    }
}
