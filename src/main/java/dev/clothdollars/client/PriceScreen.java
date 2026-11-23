package dev.clothdollars.client;

import java.math.BigInteger;
import com.simibubi.create.content.logistics.tableCloth.TableClothBlockEntity;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;
import net.minecraft.client.gui.components.Tooltip;
import dev.clothdollars.ClothDollars.PriceEdit;
import dev.clothdollars.Prices;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class PriceScreen extends Screen {
    private final BlockPos pos;
    private final String previous;
    private boolean editing;
    private EditBox amount;
    private IconButton confirm;

    public static void open(TableClothBlockEntity cloth) {
        Minecraft.getInstance().setScreen(new PriceScreen(cloth.getBlockPos(), Prices.price(cloth)));
    }
    private PriceScreen(BlockPos pos, String price) {
        super(Component.literal("Price per order"));
        this.pos = pos;
        previous = price;
        editing = !price.isEmpty();
    }
    private Button button(int x, int y, int w, String label, Runnable action) {
        return addRenderableWidget(Button.builder(Component.literal(label), b -> action.run()).bounds(x,y,w,18).build());
    }
    @Override protected void init() {
        int x = width / 2, y = height / 2;
        if (!editing) {
            button(x-78,y-6,156,"Use CobbleDollars", () -> { editing = true; rebuildWidgets(); });
            button(x-42,y+21,84,"Cancel",this::onClose);
            return;
        }
        String value = amount == null ? previous : amount.getValue();
        amount = addRenderableWidget(new EditBox(font,x-78,y-37,156,18,title));
        amount.setMaxLength(64);
        amount.setFilter(s -> s.matches("[0-9]{0,64}"));
        amount.setValue(value.isEmpty() ? "1" : value);
        int[] steps = {1000,10000,100000,1000000};
        String[] labels = {"1k","10k","100k","1m"};
        for (int i=0;i<steps.length;i++) {
            final int step = steps[i];
            button(x-91+i*46,y-15,44,"+"+labels[i],() -> adjust(step));
            button(x-91+i*46,y+5,44,"-"+labels[i],() -> adjust(-step));
        }
        button(x-88,y+65,70,"Cancel",this::onClose);
        confirm = new IconButton(x+72,y+65,AllIcons.I_CONFIRM);
        confirm.setTooltip(Tooltip.create(Component.literal("Confirm")));
        confirm.withCallback(() -> {
            PacketDistributor.sendToServer(new PriceEdit(pos, amount.getValue()));
            onClose();
        });
        addRenderableWidget(confirm);
        amount.setResponder(s -> confirm.active = Prices.valid(s));
        confirm.active = Prices.valid(amount.getValue());
        setInitialFocus(amount);
    }
    private void adjust(int delta) {
        String value = amount.getValue();
        BigInteger result = new BigInteger(value.isEmpty() ? "0" : value).add(BigInteger.valueOf(delta)).max(BigInteger.ONE);
        if (result.toString().length() <= 64) amount.setValue(result.toString());
    }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial) {
        int x = width/2-96, y = height/2-60;
        renderBackground(g,mouseX,mouseY,partial);
        AllGuiTextures.STOCK_KEEPER_CATEGORY_HEADER.render(g,x,y);
        for (int i=0;i<5;i++) AllGuiTextures.STOCK_KEEPER_CATEGORY.render(g,x,y+18+i*20);
        if (editing) AllGuiTextures.STOCK_KEEPER_CATEGORY_FOOTER.render(g,x,y+118);
        g.drawCenteredString(font,title,width/2,y+5,0xffefe0cc);
        for (var widget : renderables) widget.render(g,mouseX,mouseY,partial);
    }
    @Override public boolean isPauseScreen() { return false; }
}
