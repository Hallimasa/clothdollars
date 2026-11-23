package dev.clothdollars.mixin;

import dev.clothdollars.Shop;
import com.simibubi.create.content.logistics.stockTicker.StockTickerInteractionHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=StockTickerInteractionHandler.class, remap=false)
abstract class CheckoutMixin {
    @Inject(method="interactWithShop", at=@At("HEAD"), cancellable=true)
    private static void checkout(Player player, Level level, BlockPos pos, ItemStack stack, CallbackInfo ci) {
        if (Shop.checkout(player, level, pos, stack)) ci.cancel();
    }
}
