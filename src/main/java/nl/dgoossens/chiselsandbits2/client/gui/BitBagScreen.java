package nl.dgoossens.chiselsandbits2.client.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.block.Block;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;
import nl.dgoossens.chiselsandbits2.api.bit.BitStorage;
import nl.dgoossens.chiselsandbits2.api.bit.VoxelWrapper;
import nl.dgoossens.chiselsandbits2.common.bitstorage.BagContainer;
import nl.dgoossens.chiselsandbits2.common.bitstorage.StorageCapabilityProvider;
import nl.dgoossens.chiselsandbits2.common.items.BitBagItem;

import java.util.*;

public class BitBagScreen extends ContainerScreen<BagContainer> {
    private static final ResourceLocation GUI_TEXTURE = new ResourceLocation(ChiselsAndBits2.MOD_ID, "textures/gui/bit_bag.png");
    private int selectedSlot = -1;

    public BitBagScreen(BagContainer container, PlayerInventory inv, ITextComponent text) {
        super(container, inv, text);
        this.ySize = 114 + container.getRowCount() * 18;
    }

    @Override
    protected void init() {
        super.init();

        //Determine selected slot
        final ItemStack bag = container.getBag();
        if(bag.getItem() instanceof BitBagItem) {
            VoxelWrapper w = ((BitBagItem) bag.getItem()).getSelected(bag);
            selectedSlot = -1;
            if(!w.isEmpty())
                bag.getCapability(StorageCapabilityProvider.STORAGE).ifPresent(cap -> {
                    for(int i = 0; i < container.getSlotCount(); i++) {
                        if(cap.getSlotContent(i).equals(w)) {
                            selectedSlot = i;
                            return; //We've found what we're looking for.
                        }
                    }
                });
        }
    }

    @Override
    public void render(int mouseX, int mouseY, float mouseZ) {
        this.renderBackground();
        super.render(mouseX, mouseY, mouseZ);
        if (this.minecraft.player.inventory.getItemStack().isEmpty() && this.hoveredSlot != null && this.hoveredSlot.getHasStack()) {
            final ItemStack item = this.hoveredSlot.getStack();
            FontRenderer font = item.getItem().getFontRenderer(item);
            net.minecraftforge.fml.client.config.GuiUtils.preItemToolTip(item);
            ArrayList<String> text = new ArrayList<>(this.getTooltipFromItem(item));
            if(this.hoveredSlot.inventory == container.getBagInventory()) {
                //Add bit count if this is in the bag
                long bits = container.getBag().getCapability(StorageCapabilityProvider.STORAGE).map(b -> b.get(VoxelWrapper.forBlock(Block.getBlockFromItem(item.getItem())))).orElse(0L);
                text.add(TextFormatting.GRAY.toString()+(((bits*100) / 4096)/100.0d)+" blocks "+TextFormatting.ITALIC.toString()+"("+bits+" bits)");
            }
            this.renderTooltip(text, mouseX, mouseY, (font == null ? this.font : font));
            net.minecraftforge.fml.client.config.GuiUtils.postItemToolTip();
        }
    }

    /**
     * Draw the foreground layer for the GuiContainer (everything in front of the items)
     */
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        this.font.drawString(this.title.getFormattedText(), 8.0F, 6.0F, 4210752);
        this.font.drawString(this.playerInventory.getDisplayName().getFormattedText(), 8.0F, (float)(this.ySize - 96 + 2), 4210752);

        //Render selection marker over the selected slot
        if(selectedSlot >= 0) {
            this.minecraft.getTextureManager().bindTexture(GUI_TEXTURE);
            //We are already translated to the top left corner of the GUI here
            //Draw selection marker
            this.blit(5 + (selectedSlot % 9) * 18, 15 + (selectedSlot / 9) * 18, 176, 18, 22, 22);
        }
    }

    /**
     * Draws the background layer of this container (behind the items).
     */
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.minecraft.getTextureManager().bindTexture(GUI_TEXTURE);
        int i = (this.width - this.xSize) / 2;
        int j = (this.height - this.ySize) / 2;
        //Draw player inventory
        this.blit(i, j + container.getRowCount() * 18 + 17, 0, 126, this.xSize, 96);
        //Draw background for slots
        this.blit(i, j, 0, 0, this.xSize, container.getRowCount() * 18 + 17);

        i += 7;
        j += 17;

        //Draw slots
        int looseSlots = container.getSlotCount() - ((container.getRowCount() - 1) * 9);
        //Draw rows - 1 full rows
        for(int k = 0; k < (container.getRowCount() - 1); k++)
            drawRow(i, j + k * 18,9);
        //Draw one row with the remainder of the slots
        drawRow(i, j + ((container.getRowCount() - 1) * 18), looseSlots);
    }

    //Draws a row of slots
    private void drawRow(int i, int j, int rowSize) {
        for(int k = 0; k < rowSize; k++)
            this.blit(i + 18 * k, j, 176, 0, 18, 18);
    }
}
