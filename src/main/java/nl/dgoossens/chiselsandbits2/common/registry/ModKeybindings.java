package nl.dgoossens.chiselsandbits2.common.registry;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import nl.dgoossens.chiselsandbits2.common.impl.ItemMode;
import nl.dgoossens.chiselsandbits2.common.impl.MenuAction;

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

    private static final InputMappings.Input NONE = InputMappings.INPUT_INVALID;

    public KeyBinding modeMenu = new KeyBinding("Radial Menu", CONFLICT, KeyModifier.NONE, getKey(342), CATEGORY); //Left Alt

    public Map<MenuAction, KeyBinding> actionHotkeys = new HashMap<>();
    public Map<ItemMode, KeyBinding> modeHotkeys = new HashMap<>();

    public ModKeybindings() {
        //This is null if this is a data mode startup. (or server)
        if(Minecraft.getInstance()==null)
            return;

        //Generate Hotkeys
        for (MenuAction ma : MenuAction.values())
            if (ma.hasHotkey()) {
                InputMappings.Input def = NONE;
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
                ClientRegistry.registerKeyBinding(kb);
            }

        for (ItemMode im : ItemMode.values())
            if (im.hasHotkey()) {
                KeyBinding kb = new KeyBinding("general.chiselsandbits2.itemmode." + im.getTypelessName().toLowerCase() + ".hotkey", CONFLICT, NONE, CATEGORY);
                modeHotkeys.put(im, kb);
                ClientRegistry.registerKeyBinding(kb);
            }

        //Register Everything
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
