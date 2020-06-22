package nl.dgoossens.chiselsandbits2.common.registry;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import nl.dgoossens.chiselsandbits2.common.impl.item.ItemMode;
import nl.dgoossens.chiselsandbits2.common.impl.item.MenuAction;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class ModKeybindings {
    private static final IKeyConflictContext CONFLICT = new IKeyConflictContext() {
        @Override
        public boolean isActive() {
            return Minecraft.getInstance().currentScreen == null;
        }

        @Override
        public boolean conflicts(IKeyConflictContext other) {
            return this == other; //For some reason the keybinds don't trigger unless this is true.
        }
    };

    private static final String CATEGORY = "Chisels & Bits 2";

    public KeyBinding modeMenu = new KeyBinding("Radial Menu", CONFLICT, KeyModifier.NONE, getKey(342), CATEGORY); //Left Alt

    public Map<MenuAction, KeyBinding> actionHotkeys = new HashMap<>();
    public Map<ItemMode, KeyBinding> modeHotkeys = new HashMap<>();

    public ModKeybindings() {
        //Generate Hotkeys
        for (MenuAction ma : MenuAction.values())
            if (ma.hasHotkey()) {
                InputMappings.Input def = InputMappings.INPUT_INVALID;;
                KeyModifier mod = KeyModifier.NONE;
                //Undo and redo are set by default to Ctl+Z and Ctl+Y
                if(ma.equals(MenuAction.UNDO)) {
                    def = getKey(90);
                    mod = KeyModifier.CONTROL;
                } else if(ma.equals(MenuAction.REDO)) {
                    def = getKey(89);
                    mod = KeyModifier.CONTROL;
                }
                if(ma.equals(MenuAction.PLACE)) continue; //No keybind for place because its shared with swap.
                KeyBinding kb = new KeyBinding("general.chiselsandbits2.menuaction." + ma.name().toLowerCase() + ".hotkey", CONFLICT, mod, def, CATEGORY);
                actionHotkeys.put(ma, kb);
            }

        for (ItemMode im : ItemMode.values())
            if (im.hasHotkey()) {
                KeyBinding kb = new KeyBinding("general.chiselsandbits2.itemmode." + im.getTypelessName().toLowerCase() + ".hotkey", CONFLICT, InputMappings.INPUT_INVALID, CATEGORY);
                modeHotkeys.put(im, kb);
            }


    }

    public void setup() {
        //Register Everything
        for(KeyBinding kb : actionHotkeys.values())
            ClientRegistry.registerKeyBinding(kb);
        for(KeyBinding kb : modeHotkeys.values())
            ClientRegistry.registerKeyBinding(kb);

        for (Field f : getClass().getFields()) {
            if (!KeyBinding.class.isAssignableFrom(f.getType())) continue;
            try {
                ClientRegistry.registerKeyBinding((KeyBinding) f.get(this));
            } catch (Exception x) {
                x.printStackTrace();
            }
        }
    }

    private static InputMappings.Input getKey(int key) {
        return InputMappings.Type.KEYSYM.getOrMakeInput(key);
    }
}
