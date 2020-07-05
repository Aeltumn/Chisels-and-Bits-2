package nl.dgoossens.chiselsandbits2.common.items;

import com.google.common.collect.Multimap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemTier;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.Tags;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;
import nl.dgoossens.chiselsandbits2.api.item.ItemModeType;
import nl.dgoossens.chiselsandbits2.api.item.StandardTypedItem;
import nl.dgoossens.chiselsandbits2.api.item.attributes.BitModifyItem;
import nl.dgoossens.chiselsandbits2.client.util.ItemTooltipWriter;
import nl.dgoossens.chiselsandbits2.common.impl.item.ItemModeTypes;

import javax.annotation.Nullable;
import java.util.List;

public class WrenchItem extends StandardTypedItem implements BitModifyItem {
    public WrenchItem(Properties builder) {
        super(builder);
    }

    @Override
    public boolean canPerformModification(ModificationType type) {
        return type == ModificationType.ROTATE || type == ModificationType.MIRROR;
    }

    @Override
    public ItemModeType getAssociatedType() {
        return ItemModeTypes.WRENCH;
    }

    @Override
    public boolean showIconInHotbar() {
        return true;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        ItemTooltipWriter.addItemInformation(tooltip, "wrench.help",
                Minecraft.getInstance().gameSettings.keyBindUseItem,
                ChiselsAndBits2.getInstance().getKeybindings().modeMenu
        );
    }

    /**
     * Hitting entities takes durability damage. (same as ItemTool)
     */
    @Override
    public boolean hitEntity(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.damageItem(1, attacker, l -> l.sendBreakAnimation(EquipmentSlotType.MAINHAND));
        return true;
    }

    /**
     * The enchantability should be equal to that of a wooden item.
     * The item can also be enchanted with any enchantment that can go onto durability items:
     * mending, unbreaking, curse of vanishing
     */
    @Override
    public int getItemEnchantability() {
        return ItemTier.IRON.getEnchantability();
    }

    /**
     * Make the item repairable in anvils using wooden planks.
     */
    @Override
    public boolean getIsRepairable(ItemStack toRepair, ItemStack repair) {
        if (Tags.Items.INGOTS_IRON.contains(repair.getItem())) return true; //Can repair with wooden planks.
        return super.getIsRepairable(toRepair, repair);
    }

    /**
     * Here we customise the attack speed and attack damage to show up in the tooltip.
     */
    @Override
    public Multimap<String, AttributeModifier> getAttributeModifiers(EquipmentSlotType slot, ItemStack stack) {
        Multimap<String, AttributeModifier> multimap = super.getAttributeModifiers(slot, stack);
        if (slot == EquipmentSlotType.MAINHAND) {
            multimap.put(SharedMonsterAttributes.ATTACK_DAMAGE.getName(), new AttributeModifier(ATTACK_DAMAGE_MODIFIER, () -> "Tool modifier", 0.8, AttributeModifier.Operation.ADDITION));
            multimap.put(SharedMonsterAttributes.ATTACK_SPEED.getName(), new AttributeModifier(ATTACK_SPEED_MODIFIER, () -> "Tool modifier", -1.5, AttributeModifier.Operation.ADDITION));
        }
        return multimap;
    }
}
