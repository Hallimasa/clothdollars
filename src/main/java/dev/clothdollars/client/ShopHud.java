package dev.clothdollars.client;

import java.math.BigDecimal;
import com.simibubi.create.content.logistics.stockTicker.StockTickerInteractionHandler;
import com.simibubi.create.content.logistics.tableCloth.TableClothBlockEntity;
import dev.clothdollars.Prices;
import dev.clothdollars.Shop;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid="clothdollars", value=Dist.CLIENT)
public final class ShopHud {
    private static String compact(String digits) {
        int shift = digits.length() >= 7 ? 6 : digits.length() >= 4 ? 3 : 0;
        if (shift == 0) return digits;
        return new BigDecimal(digits).movePointLeft(shift).stripTrailingZeros().toPlainString()
                + (shift == 6 ? "m" : "k");
    }

    @SubscribeEvent public static void draw(RenderGuiEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui || mc.screen != null) return;
        String total = Shop.quote(mc.player.getMainHandItem());
        String text;
        if (mc.hitResult instanceof BlockHitResult hit && mc.level.getBlockEntity(hit.getBlockPos()) instanceof TableClothBlockEntity cloth
                && !Prices.price(cloth).isEmpty()) {
            text = "Price per order: $" + compact(Prices.price(cloth));
            if (!total.isEmpty()) text += "   List: $" + compact(total);
        } else if (!total.isEmpty() && mc.hitResult instanceof EntityHitResult hit
                && StockTickerInteractionHandler.getStockTickerPosition(hit.getEntity()) != null) {
            text = "Shopping list: $" + compact(total);
        } else return;
        var g = event.getGuiGraphics();
        var lines = mc.font.split(net.minecraft.network.chat.Component.literal(text), g.guiWidth()-16);
        int y = g.guiHeight()/2+18;
        for (var line : lines) {
            g.drawCenteredString(mc.font,line,g.guiWidth()/2,y,0xffffdf8a);
            y += mc.font.lineHeight+1;
        }
    }
}
