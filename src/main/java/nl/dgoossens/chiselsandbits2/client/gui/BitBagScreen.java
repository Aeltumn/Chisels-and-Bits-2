package nl.dgoossens.chiselsandbits2.client.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.client.gui.widget.ExtendedButton;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;
import nl.dgoossens.chiselsandbits2.api.bit.BitStorage;
import nl.dgoossens.chiselsandbits2.api.bit.VoxelWrapper;
import nl.dgoossens.chiselsandbits2.common.bitstorage.BagContainer;
import nl.dgoossens.chiselsandbits2.common.bitstorage.StorageCapabilityProvider;
import nl.dgoossens.chiselsandbits2.common.items.BitBagItem;
import nl.dgoossens.chiselsandbits2.common.network.client.CVoidBagPacket;
import org.apache.logging.log4j.util.TriConsumer;

import java.util.*;

public class BitBagScreen extends ContainerScreen<BagContainer> {
    private static final ResourceLocation GUI_TEXTURE = new ResourceLocation(ChiselsAndBits2.MOD_ID, "textures/gui/bit_bag.png");
    private int selectedSlot = -1;
    private DurabilityBarRenderer cache;
    private ExtendedButton trashButton;
    private long trashButtonClicked;

    public BitBagScreen(BagContainer container, PlayerInventory inv, ITextComponent text) {
        super(container, inv, text);
        updateSize();
    }

    @Override
    protected void init() {
        super.init();

        //Setup buttons
        addButton(trashButton = new ExtendedButton(guiLeft - 18, guiTop + 2, 18, 18, "", b -> {
            if(trashButtonClicked != 0 && System.currentTimeMillis() - trashButtonClicked > 500) { //Make sure it isn't triggered double.
                //Actually void everything
                trashButtonClicked = 0;

                ChiselsAndBits2.getInstance().getNetworkRouter().sendToServer(new CVoidBagPacket(Minecraft.getInstance().player.inventory.getItemStack()));
            } else {
                //Set the trash button click to the current time
                trashButtonClicked = System.currentTimeMillis();
            }
        }));

        updateSize();
    }

    private void updateSize() {
        this.ySize = 114 + container.getRowCount() * 18;

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
    protected void handleMouseClick(Slot slotIn, int slotId, int mouseButton, ClickType type) {
        if((type == ClickType.PICKUP || type == ClickType.THROW) && trashButton.isHovered())
            return;

        super.handleMouseClick(slotIn, slotId, mouseButton, type);
    }

    @Override
    public void render(int mouseX, int mouseY, float mouseZ) {
        this.renderBackground();
        super.render(mouseX, mouseY, mouseZ);
        if(!trashButton.isHovered()) trashButtonClicked = 0; //Always require continuous hovering during clicking.
        if(trashButton.isHovered()) {
            FontRenderer font = getMinecraft().fontRenderer;
            this.renderTooltip(Arrays.asList(I18n.format("button."+ChiselsAndBits2.MOD_ID+".trash"+(trashButtonClicked != 0 ? ".confirm" : ""))), mouseX, mouseY, (font == null ? this.font : font));
        } else if(hoveredSlot == container.getInputSlot()) {
            FontRenderer font = getMinecraft().fontRenderer;
            this.renderTooltip(Arrays.asList(I18n.format("button."+ChiselsAndBits2.MOD_ID+".input")), mouseX, mouseY, (font == null ? this.font : font));
        } else {
            //Avoid double rendering tooltips
            if (this.minecraft.player.inventory.getItemStack().isEmpty() && this.hoveredSlot != null && this.hoveredSlot.getHasStack()) {
                final ItemStack item = this.hoveredSlot.getStack();
                FontRenderer font = item.getItem().getFontRenderer(item);
                net.minecraftforge.fml.client.gui.GuiUtils.preItemToolTip(item);
                ArrayList<String> text = new ArrayList<>(this.getTooltipFromItem(item));
                if(this.hoveredSlot.inventory == container.getBagInventory()) {
                    //Add bit count if this is in the bag
                    long bits = container.getBag().getCapability(StorageCapabilityProvider.STORAGE).map(b -> b.get(VoxelWrapper.forBlock(Block.getBlockFromItem(item.getItem())))).orElse(0L);
                    text.add(TextFormatting.GRAY.toString()+(((bits*100) / 4096)/100.0d)+" blocks "+TextFormatting.ITALIC.toString()+"("+bits+" bits)");
                }
                this.renderTooltip(text, mouseX, mouseY, (font == null ? this.font : font));
                net.minecraftforge.fml.client.gui.GuiUtils.postItemToolTip();
            }
        }
    }

    /**
     * Draw the foreground layer for the GuiContainer (everything in front of the items)
     */
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        this.font.drawString(this.title.getFormattedText(), 8.0F, 6.0F, 4210752);
        this.font.drawString(this.playerInventory.getDisplayName().getFormattedText(), 8.0F, (float)(this.ySize - 96 + 2), 4210752);

        //Render selection marker over the selected slot
        this.minecraft.getTextureManager().bindTexture(GUI_TEXTURE);
        if(selectedSlot >= 0) {
            //We are already translated to the top left corner of the GUI here
            //Draw selection marker
            this.blit(5 + (selectedSlot % 9) * 18, 15 + (selectedSlot / 9) * 18, 176, 18, 22, 22);
        }

        //Render button icons
        if(trashButtonClicked != 0) this.blit(-15, 5, 176 + 12, 40, 12, 13);
        else this.blit(-15, 5, 176, 40, 12, 13);

        this.blit(-14, 24, 176, 53, 10, 11);

        //Render durability bars
        if(this.minecraft.player.isCreative()) return; //No durability bars in creative
        BitStorage store = container.getBag().getCapability(StorageCapabilityProvider.STORAGE).orElse(null);
        if(store == null) return;
        getDurabilityBarRenderer().setAlpha(0.5f); //Make them a bit translucent
        long s = ChiselsAndBits2.getInstance().getConfig().bitsPerTypeSlot.get();
        drawForEachSlot(7, 17, (i, j, k) -> {
            if(store.getSlotContent(k).isEmpty()) return;
            getDurabilityBarRenderer().renderDurabilityBar(s - store.get(store.getSlotContent(k)), ChiselsAndBits2.getInstance().getConfig().bitsPerTypeSlot.get(), i, j);
        });
    }

    public DurabilityBarRenderer getDurabilityBarRenderer() {
        if(cache == null) cache = new DurabilityBarRenderer();
        return cache;
    }

    /**
     * Draws the background layer of this container (behind the items).
     */
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.minecraft.getTextureManager().bindTexture(GUI_TEXTURE);
        int i2 = (this.width - this.xSize) / 2;
        int j2 = (this.height - this.ySize) / 2;
        //Draw player inventory
        this.blit(i2, j2 + container.getRowCount() * 18 + 17, 0, 126, this.xSize, 96);
        //Draw background for slots
        this.blit(i2, j2, 0, 0, this.xSize, container.getRowCount() * 18 + 17);

        //Draw slots
        drawForEachSlot(i2 + 7, j2 + 17, (i, j, k) -> this.blit(i, j, 176, 0, 18, 18));

        //Draw input slot background
        this.blit(i2 - 18, j2 + 20, 176, 64, 18, 18);
    }

    private void drawForEachSlot(int i, int j, TriConsumer<Integer, Integer, Integer> draw) {
        int looseSlots = container.getSlotCount() - ((container.getRowCount() - 1) * 9);
        //Draw rows - 1 full rows
        for(int q = 0; q < (container.getRowCount() - 1); q++)
            drawRow(i, j + q * 18, 9, draw);
        //Draw one row with the remainder of the slots
        drawRow(i, j + (((container.getRowCount() - 1) * 18)), looseSlots, draw);
    }

    //Draws a row of slots
    private void drawRow(int i, int j, int rowSize, TriConsumer<Integer, Integer, Integer> run) {
        for(int k = 0; k < rowSize; k++)
            run.accept(i + k * 18, j, k);
    }
}
