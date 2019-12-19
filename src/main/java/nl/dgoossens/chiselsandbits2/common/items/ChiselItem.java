package nl.dgoossens.chiselsandbits2.common.items;

import com.google.common.collect.Multimap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemTier;
import net.minecraft.util.Direction;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.Tags;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;
import nl.dgoossens.chiselsandbits2.api.item.IBitModifyItem;
import nl.dgoossens.chiselsandbits2.api.item.IItemModeType;
import nl.dgoossens.chiselsandbits2.client.gui.ItemModeMenu;
import nl.dgoossens.chiselsandbits2.common.impl.ItemModeType;
import nl.dgoossens.chiselsandbits2.common.impl.MenuAction;
import nl.dgoossens.chiselsandbits2.common.utils.ItemModeUtil;
import nl.dgoossens.chiselsandbits2.common.utils.ItemTooltipWriter;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChiselItem extends TypedItem implements IBitModifyItem {
    public ChiselItem(Item.Properties builder) {
        super(builder);
    }

    @Override
    public boolean canPerformModification(ModificationType type) {
        return type == ModificationType.BUILD || type == ModificationType.EXTRACT;
    }

    @Override
    public boolean showIconInHotbar() {
        return true;
    }

    @Override
    public IItemModeType getAssociatedType() {
        return ItemModeType.CHISEL;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        ItemTooltipWriter.addItemInformation(tooltip, "chisel.help",
                Minecraft.getInstance().gameSettings.keyBindAttack,
                Minecraft.getInstance().gameSettings.keyBindUseItem,
                ChiselsAndBits2.getInstance().getKeybindings().modeMenu
        );
    }

    @Override
    public Set<ItemModeMenu.MenuButton> getMenuButtons(final ItemStack item) {
        Set<ItemModeMenu.MenuButton> ret = new HashSet<>();
        if (ItemModeUtil.getMenuActionMode(item).equals(MenuAction.PLACE))
            ret.add(new ItemModeMenu.MenuButton(MenuAction.PLACE, -ItemModeMenu.TEXT_DISTANCE - 18, -20, Direction.WEST));
        else
            ret.add(new ItemModeMenu.MenuButton(MenuAction.SWAP, -ItemModeMenu.TEXT_DISTANCE - 18, -20, Direction.WEST));
        return ret;
    }

    /**
     * Hitting entities takes durability damage. (same as ItemTool)
     */
    @Override
    public boolean hitEntity(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        //Do 50 durability damage because otherwise it's barely noticable.
        stack.damageItem(50, attacker, l -> l.sendBreakAnimation(EquipmentSlotType.MAINHAND));
        return true;
    }

    /**
     * The enchantability should be equal to that of a golden item.
     * The item can also be enchanted with any enchantment that can go onto durability items:
     * mending, unbreaking, curse of vanishing
     */
    @Override
    public int getItemEnchantability() {
        return ItemTier.GOLD.getEnchantability();
    }

    /**
     * Make the item repairable in anvils using gold.
     */
    @Override
    public boolean getIsRepairable(ItemStack toRepair, ItemStack repair) {
        if (Tags.Items.INGOTS_GOLD.contains(repair.getItem())) return true; //Can repair with gold.
        return super.getIsRepairable(toRepair, repair);
    }

    /**
     * Here we customise the attack speed and attack damage to show up in the tooltip,
     * attack damage is 1 and attack speed is 2.
     */
    @Override
    public Multimap<String, AttributeModifier> getAttributeModifiers(EquipmentSlotType slot, ItemStack stack) {
        Multimap<String, AttributeModifier> multimap = super.getAttributeModifiers(slot, stack);
        if (slot == EquipmentSlotType.MAINHAND) {
            multimap.put(SharedMonsterAttributes.ATTACK_DAMAGE.getName(), new AttributeModifier(ATTACK_DAMAGE_MODIFIER, () -> "Tool modifier", 0, AttributeModifier.Operation.ADDITION));
            multimap.put(SharedMonsterAttributes.ATTACK_SPEED.getName(), new AttributeModifier(ATTACK_SPEED_MODIFIER, () -> "Tool modifier", -2, AttributeModifier.Operation.ADDITION));
        }
        return multimap;
    }

    /**
     * Overwrite the Properties.maxDamage() with the configuration value.
     */
    @Override
    public int getMaxDamage(ItemStack stack) {
        return ChiselsAndBits2.getInstance().getConfig().chiselDurability.get();
    }
}
