package dev.clothdollars.mixin;

import java.util.List;
import dev.clothdollars.Shop;
import com.simibubi.create.content.logistics.tableCloth.ShoppingListItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=ShoppingListItem.class, remap=false)
abstract class ListTooltipMixin {
    @Inject(method="appendHoverText", at=@At("HEAD"), cancellable=true)
    private void quote(ItemStack stack, Item.TooltipContext context, List<Component> lines, TooltipFlag flags, CallbackInfo ci) {
        String total = Shop.quote(stack);
        if (total.isEmpty()) return;
        var list = ShoppingListItem.getList(stack);
        if (list != null && context.level() != null)
            for (var goods : list.bakeEntries(context.level(),null).getFirst().getStacksByCount())
                lines.add(goods.stack.getHoverName().copy().append(" ×" + goods.count));
        lines.add(Component.literal("Total: $" + total));
        lines.add(Component.translatable("create.table_cloth.hand_to_shop_keeper"));
        lines.add(Component.translatable("create.table_cloth.sneak_click_discard"));
        ci.cancel();
    }
}
