package nl.dgoossens.chiselsandbits2;

import net.minecraft.client.gui.ScreenManager;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import nl.dgoossens.chiselsandbits2.api.bit.BitStorage;
import nl.dgoossens.chiselsandbits2.api.ChiselsAndBitsAPI;
import nl.dgoossens.chiselsandbits2.client.ClientSide;
import nl.dgoossens.chiselsandbits2.client.UndoTracker;
import nl.dgoossens.chiselsandbits2.client.gui.BitBagScreen;
import nl.dgoossens.chiselsandbits2.common.bitstorage.BitStorageImpl;
import nl.dgoossens.chiselsandbits2.common.bitstorage.StorageCapability;
import nl.dgoossens.chiselsandbits2.common.impl.ChiselsAndBitsAPIImpl;
import nl.dgoossens.chiselsandbits2.common.impl.item.ItemMode;
import nl.dgoossens.chiselsandbits2.common.impl.item.ItemModeType;
import nl.dgoossens.chiselsandbits2.common.impl.item.ItemModeWrapper;
import nl.dgoossens.chiselsandbits2.common.network.NetworkRouter;
import nl.dgoossens.chiselsandbits2.common.registry.*;
import nl.dgoossens.chiselsandbits2.common.impl.item.GlobalCBMCapability;

@Mod(ChiselsAndBits2.MOD_ID)
public class ChiselsAndBits2 {
    public static final String MOD_ID = "chiselsandbits2";

    private static ChiselsAndBits2 instance;

    private final NetworkRouter NETWORK_ROUTER;
    private final ChiselsAndBitsAPI API;
    private final ModStatistics STATISTICS;
    private final ModConfiguration CONFIGURATION;
    private final UndoTracker UNDO;
    private final Registration REGISTER;
    private ClientSide CLIENT;
    private ModKeybindings KEYBINDINGS;

    public ChiselsAndBits2() {
        //Temporary solution until FML stops being a PITA
        ClassLoadEnforcer.loadAllClasses();

        instance = this;
        API = new ChiselsAndBitsAPIImpl();
        CONFIGURATION = new ModConfiguration();
        NETWORK_ROUTER = new NetworkRouter();
        STATISTICS = new ModStatistics();
        UNDO = new UndoTracker();
        REGISTER = new Registration();

        //Only initialise the client class when on the CLIENT distribution.
        DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> {
            CLIENT = new ClientSide();
            KEYBINDINGS = new ModKeybindings();
        });

        //Register to mod bus
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setupClient);
        DistExecutor.runWhenOn(Dist.CLIENT, () -> CLIENT::initialise);

        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, CONFIGURATION.SERVER);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, CONFIGURATION.CLIENT);
    }

    public static ChiselsAndBits2 getInstance() {
        return instance;
    }

    // Ran after all registry events have finished.
    private void setup(final FMLCommonSetupEvent event) {
        //Register things
        CapabilityManager.INSTANCE.register(BitStorage.class, new StorageCapability(), BitStorageImpl::new);
        CapabilityManager.INSTANCE.register(ItemModeWrapper.class, new GlobalCBMCapability(), ItemModeWrapper::new);
        NETWORK_ROUTER.init();

        //Setup vanilla restrictions
        getAPI().getRestrictions().restrictBlockStateProperty(BlockStateProperties.SNOWY, false, true); //Make all snowy grass not snowy automatically
        getAPI().addIgnoredBlockState(BlockStateProperties.DISTANCE_0_7);
        getAPI().addIgnoredBlockState(BlockStateProperties.DISTANCE_1_7);
        getAPI().addIgnoredBlockState(BlockStateProperties.PERSISTENT); //Persistence messes with breaking a bit off of a leaf and then replacing it.
    }

    private void setupClient(final FMLClientSetupEvent event) {
        //Register client-side things
        KEYBINDINGS.setup();

        //Register container screens
        ScreenManager.registerFactory(getRegister().BIT_BAG_CONTAINER.get(), BitBagScreen::new);
    }

    public ChiselsAndBitsAPI getAPI() {
        return API;
    }

    public Registration getRegister() {
        return REGISTER;
    }

    public ClientSide getClient() {
        return CLIENT;
    }

    public NetworkRouter getNetworkRouter() {
        return NETWORK_ROUTER;
    }

    public ModConfiguration getConfig() {
        return CONFIGURATION;
    }

    public ModKeybindings getKeybindings() {
        return KEYBINDINGS;
    }

    public ModStatistics getStatistics() {
        return STATISTICS;
    }

    public UndoTracker getUndoTracker() {
        return UNDO;
    }

    /**
     * Temporary way to determine whether or not unfinished features should be shown.
     */
    @Deprecated
    public static boolean showUnfinishedFeatures() {
        return false;
    }
}
