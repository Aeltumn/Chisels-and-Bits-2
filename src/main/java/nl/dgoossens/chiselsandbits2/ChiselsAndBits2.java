package nl.dgoossens.chiselsandbits2;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLModIdMappingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import nl.dgoossens.chiselsandbits2.api.BeakerStorage;
import nl.dgoossens.chiselsandbits2.api.ChiselsAndBitsAPI;
import nl.dgoossens.chiselsandbits2.client.ClientSide;
import nl.dgoossens.chiselsandbits2.api.ICacheClearable;
import nl.dgoossens.chiselsandbits2.client.render.models.SmartModelManager;
import nl.dgoossens.chiselsandbits2.common.bitstorage.BagCapability;
import nl.dgoossens.chiselsandbits2.api.BagStorage;
import nl.dgoossens.chiselsandbits2.common.bitstorage.BagStorageImpl;
import nl.dgoossens.chiselsandbits2.common.bitstorage.BeakerCapability;
import nl.dgoossens.chiselsandbits2.common.bitstorage.BeakerStorageImpl;
import nl.dgoossens.chiselsandbits2.common.impl.ChiselsAndBitsAPIImpl;
import nl.dgoossens.chiselsandbits2.common.registry.ModConfiguration;
import nl.dgoossens.chiselsandbits2.common.registry.ModBlocks;
import nl.dgoossens.chiselsandbits2.common.registry.ModItems;
import nl.dgoossens.chiselsandbits2.common.registry.ModKeybindings;
import nl.dgoossens.chiselsandbits2.common.utils.ModelUtil;
import nl.dgoossens.chiselsandbits2.network.NetworkRouter;

import java.util.ArrayList;
import java.util.List;

@Mod(ChiselsAndBits2.MOD_ID)
public class ChiselsAndBits2 {
    public static final String MOD_ID = "chiselsandbits2";
    private static ChiselsAndBits2 instance;

    private final ModItems ITEMS = new ModItems();
    private final ModBlocks BLOCKS = new ModBlocks();
    private final ClientSide CLIENT = new ClientSide();
    private final ModConfiguration CONFIGURATION = new ModConfiguration();
    private final NetworkRouter NETWORK_ROUTER = new NetworkRouter();
    private final ModKeybindings KEYBINDINGS = new ModKeybindings();
    private final ChiselsAndBitsAPI API = new ChiselsAndBitsAPIImpl();
    private final SmartModelManager SMART_MODEL_MANAGER;

    public ChiselsAndBits2() {
        instance = this;
        SMART_MODEL_MANAGER = new SmartModelManager();

        //Register to mod bus
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, CONFIGURATION.SERVER);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, CONFIGURATION.CLIENT);
    }

    public static ChiselsAndBits2 getInstance() { return instance; }
    public static ChiselsAndBitsAPI getAPI() { return getInstance().API; }
    public static ModItems getItems() { return getInstance().ITEMS; }
    public static ModBlocks getBlocks() { return getInstance().BLOCKS; }
    public static ClientSide getClient() { return getInstance().CLIENT; }
    public static ModConfiguration getConfig() { return getInstance().CONFIGURATION; }
    public static ModKeybindings getKeybindings() { return getInstance().KEYBINDINGS; }

    // Ran after all registry events have finished.
    private void setup(final FMLCommonSetupEvent event) {
        DistExecutor.runWhenOn(Dist.CLIENT, () -> CLIENT::setup);

        //Register event busses
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(NETWORK_ROUTER);

        FMLJavaModLoadingContext.get().getModEventBus().register(SMART_MODEL_MANAGER);
        CapabilityManager.INSTANCE.register(BagStorage.class, new BagCapability(), BagStorageImpl::new);
        CapabilityManager.INSTANCE.register(BeakerStorage.class, new BeakerCapability(), BeakerStorageImpl::new);
    }

    //TODO this all needs someplaceelse to live
    boolean idsHaveBeenMapped = false;
    List<ICacheClearable> cacheClearables = new ArrayList<>();

    @SubscribeEvent
    public void idsMapped(final FMLModIdMappingEvent event) {
        idsHaveBeenMapped = true;
        //BlockBitInfo.recalculateFluidBlocks();
        clearCache();
        new ModelUtil().clearCache();
    }

    public void clearCache() {
        if (idsHaveBeenMapped) {
            for(final ICacheClearable clearable : cacheClearables)
                clearable.clearCache();

            //TODO addClearable(UndoTracker.getInstance());
            //VoxelBlob.clearCache();
        }
    }

    /**
     * Adds an IClearable to fromName cleared when clearCache() is called.
     */
    public void addClearable(final ICacheClearable cache) {
        if(!cacheClearables.contains(cache)) cacheClearables.add(cache);
    }
}
