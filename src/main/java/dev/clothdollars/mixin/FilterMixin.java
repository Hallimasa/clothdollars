package dev.clothdollars.mixin;

import dev.clothdollars.Prices;
import com.simibubi.create.content.logistics.tableCloth.TableClothFilteringBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=TableClothFilteringBehaviour.class, remap=false)
abstract class FilterMixin {
    @Inject(method="setFilter", at=@At("RETURN"))
    private void itemCurrency(ItemStack item, CallbackInfoReturnable<Boolean> ci) {
        var entity = ((BlockEntityBehaviour)(Object)this).blockEntity;
        if (ci.getReturnValue() && !item.isEmpty() && !((Prices.Tag)entity).clothdollars$get().isEmpty()) {
            ((Prices.Tag)entity).clothdollars$set("");
            entity.notifyUpdate();
        }
    }
}
