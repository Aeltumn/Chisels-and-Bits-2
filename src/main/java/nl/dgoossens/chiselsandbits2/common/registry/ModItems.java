package nl.dgoossens.chiselsandbits2.common.registry;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistryEntry;
import net.minecraftforge.registries.IForgeRegistryEntry;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;
import nl.dgoossens.chiselsandbits2.common.items.*;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModItems {
    public final Item CHISEL = new ChiselItem(new Item.Properties().maxDamage(-1).group(ModItemGroups.CHISELS_AND_BITS2));
    public final Item PATTERN = new PatternItem(new Item.Properties().maxStackSize(1).group(ModItemGroups.CHISELS_AND_BITS2));
    public final Item SAW = new SawItem(new Item.Properties().maxDamage(238).group(ModItemGroups.CHISELS_AND_BITS2));
    public final Item TAPE_MEASURE = new TapeMeasureItem(new Item.Properties().maxStackSize(1).group(ModItemGroups.CHISELS_AND_BITS2));
    public final Item WRENCH = new WrenchItem(new Item.Properties().maxDamage(238).group(ModItemGroups.CHISELS_AND_BITS2));
    public final Item MALLET = new MalletItem(new Item.Properties().maxDamage(238).group(ModItemGroups.CHISELS_AND_BITS2));
    public final Item BLUEPRINT = new BlueprintItem(new Item.Properties().maxStackSize(1).group(ModItemGroups.CHISELS_AND_BITS2));
    public final Item MORPHING_BIT = new MorphingBitItem(new Item.Properties().group(ModItemGroups.CHISELS_AND_BITS2));

    //all of the coloured bit bags
    public final Item BIT_BAG = new BitBagItem();
    public final Item WHITE_BIT_BAG = new BitBagItem();
    public final Item ORANGE_BIT_BAG = new BitBagItem();
    public final Item MAGENTA_BIT_BAG = new BitBagItem();
    public final Item LIGHT_BLUE_BIT_BAG = new BitBagItem();
    public final Item YELLOW_BIT_BAG = new BitBagItem();
    public final Item LIME_BIT_BAG = new BitBagItem();
    public final Item PINK_BIT_BAG = new BitBagItem();
    public final Item GRAY_BIT_BAG = new BitBagItem();
    public final Item LIGHT_GRAY_BIT_BAG = new BitBagItem();
    public final Item CYAN_BIT_BAG = new BitBagItem();
    public final Item PURPLE_BIT_BAG = new BitBagItem();
    public final Item BLUE_BIT_BAG = new BitBagItem();
    public final Item BROWN_BIT_BAG = new BitBagItem();
    public final Item GREEN_BIT_BAG = new BitBagItem();
    public final Item RED_BIT_BAG = new BitBagItem();
    public final Item BLACK_BIT_BAG = new BitBagItem();

    public final Item BIT_BEAKER = new BitBeakerItem();
    public final Item WHITE_TINTED_BIT_BEAKER = new BitBeakerItem();
    public final Item ORANGE_TINTED_BIT_BEAKER = new BitBeakerItem();
    public final Item MAGENTA_TINTED_BIT_BEAKER = new BitBeakerItem();
    public final Item LIGHT_BLUE_TINTED_BIT_BEAKER = new BitBeakerItem();
    public final Item YELLOW_TINTED_BIT_BEAKER = new BitBeakerItem();
    public final Item LIME_TINTED_BIT_BEAKER = new BitBeakerItem();
    public final Item PINK_TINTED_BIT_BEAKER = new BitBeakerItem();
    public final Item GRAY_TINTED_BIT_BEAKER = new BitBeakerItem();
    public final Item LIGHT_GRAY_TINTED_BIT_BEAKER = new BitBeakerItem();
    public final Item CYAN_TINTED_BIT_BEAKER = new BitBeakerItem();
    public final Item PURPLE_TINTED_BIT_BEAKER = new BitBeakerItem();
    public final Item BLUE_TINTED_BIT_BEAKER = new BitBeakerItem();
    public final Item BROWN_TINTED_BIT_BEAKER = new BitBeakerItem();
    public final Item GREEN_TINTED_BIT_BEAKER = new BitBeakerItem();
    public final Item RED_TINTED_BIT_BEAKER = new BitBeakerItem();
    public final Item BLACK_TINTED_BIT_BEAKER = new BitBeakerItem();
    
    public final Item OAK_PALETTE = new PaletteItem();
    public final Item SPRUCE_PALETTE = new PaletteItem();
    public final Item BIRCH_PALETTE = new PaletteItem();
    public final Item JUNGLE_PALETTE = new PaletteItem();
    public final Item ACACIA_PALETTE = new PaletteItem();
    public final Item DARK_OAK_PALETTE = new PaletteItem();
    
    @SubscribeEvent
    public static void onItemRegistry(final RegistryEvent.Register<Item> e) {
        //Register all items in this class automatically.
        registerAll(e, ChiselsAndBits2.getInstance().getItems(), Item.class);
    }

    /**
     * Internal method to automatically register all fields in the class to the registry.
     */
    static <T extends IForgeRegistryEntry<T>> void registerAll(final RegistryEvent.Register<T> register, final Object k, final Class<T> type) {
        for (Field f : k.getClass().getFields()) {
            if (!type.isAssignableFrom(f.getType())) continue;
            try {
                register.getRegistry().register(((ForgeRegistryEntry<T>) f.get(k)).setRegistryName(ChiselsAndBits2.MOD_ID,
                        f.getName().toLowerCase()));
            } catch (Exception x) {
                x.printStackTrace();
            }
        }
    }

    //Cached data to make colour lookups faster.
    private Map<BagBeakerColours, BitBagItem> bags = new HashMap<>();
    private Map<BagBeakerColours, BitBeakerItem> beakers = new HashMap<>();
    
    public ModItems() {
        //Build the caches
        for (Field f : getClass().getFields()) {
            if(f.getName().endsWith("_BIT_BAG")) {
                String val = f.getName().substring(0, f.getName().length() - "_BIT_BAG".length());
                try {
                    bags.put(BagBeakerColours.valueOf(val), (BitBagItem) f.get(this));
                } catch(Exception x) {}
            }
            if(f.getName().endsWith("_TINTED_BIT_BEAKER")) {
                String val = f.getName().substring(0, f.getName().length() - "_TINTED_BIT_BEAKER".length());
                try {
                   beakers.put(BagBeakerColours.valueOf(val), (BitBeakerItem) f.get(this));
                } catch(Exception x) {}
            }
        }
    }
    
    /**
     * Get the colour of the bit bag, we hijack MenuAction's colour values.
     */
    public BagBeakerColours getBagBeakerColour(final ItemStack stack) {
        for(BagBeakerColours c : BagBeakerColours.values()) {
            if(stack.getItem().equals(bags.get(c))) return c;
            if(stack.getItem().equals(beakers.get(c))) return c;
        }
        return null;
    }

    /**
     * Internal method used by recipe generators.
     */
    public BitBagItem getBitBag(final BagBeakerColours colour) {
        return bags.get(colour);
    }

    /**
     * Internal method used by recipe generators.
     */
    public BitBeakerItem getBitBeaker(final BagBeakerColours colour) {
        return beakers.get(colour);
    }

    /**
     * Internal method used by recipe generators.
     */
    public PaletteItem getPalette(final String fieldName) {
        try {
            for (Field f : getClass().getFields()) {
                if(fieldName.equals(f.getName()))
                    return (PaletteItem) f.get(this);
            }
        } catch(Exception x) {}
        return null;
    }
}
