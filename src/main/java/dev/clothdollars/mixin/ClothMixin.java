package dev.clothdollars.mixin;

import com.simibubi.create.content.logistics.tableCloth.TableClothBlockEntity;
import dev.clothdollars.Prices;
import dev.clothdollars.Shop;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(value = TableClothBlockEntity.class, remap = false)
abstract class ClothMixin implements Prices.Tag {
    @Unique private String clothdollars$price = "";
    public String clothdollars$get() { return clothdollars$price; }
    public void clothdollars$set(String price) { clothdollars$price = price; }
    @Inject(method="write", at=@At("TAIL"))
    private void save(CompoundTag tag, HolderLookup.Provider lookup, boolean sync, CallbackInfo ci) {
        if (!clothdollars$price.isEmpty()) tag.putString("ClothDollarsPrice", clothdollars$price);
    }
    @Inject(method="read", at=@At("TAIL"))
    private void load(CompoundTag tag, HolderLookup.Provider lookup, boolean sync, CallbackInfo ci) {
        clothdollars$price = tag.getString("ClothDollarsPrice");
    }
    @Inject(method="useShop", at=@At("HEAD"), cancellable=true)
    private void shopping(Player player, CallbackInfoReturnable<ItemInteractionResult> ci) {
        if (Shop.add((TableClothBlockEntity)(Object)this, player)) ci.setReturnValue(ItemInteractionResult.SUCCESS);
    }
}
