package dev.clothdollars.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.logistics.tableCloth.TableClothBlockEntity;
import com.simibubi.create.content.logistics.tableCloth.TableClothRenderer;
import dev.clothdollars.Prices;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=TableClothRenderer.class, remap=false)
abstract class TagRenderer {
    @Inject(method="renderSafe(Lcom/simibubi/create/content/logistics/tableCloth/TableClothBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V", at=@At("TAIL"))
    private void price(TableClothBlockEntity cloth,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay,CallbackInfo ci) {
        String value = Prices.price(cloth);
        if (value.isEmpty()) return;
        String label = "$" + value;
        if (value.length()>5) {
            String[] units = {"","k","m","b","t"};
            int group = (value.length()-1)/3;
            label = group < units.length ? "$"+value.substring(0,value.length()-group*3)+units[group] : "$"+value.charAt(0)+"e"+(value.length()-1);
        }
        var transform = cloth.priceTag.getSlotPositioning();
        var offset = transform.getLocalOffset(cloth.getLevel(),cloth.getBlockPos(),cloth.getBlockState());
        pose.pushPose();
        pose.translate(offset.x,offset.y,offset.z);
        transform.rotate(cloth.getLevel(),cloth.getBlockPos(),cloth.getBlockState(),pose);
        pose.translate(0, 0, -0.025);
        Font font = Minecraft.getInstance().font;
        float scale = Math.min(0.021f, 0.215f/font.width(label));
        pose.scale(-scale,-scale,scale);
        font.drawInBatch(label,-font.width(label)/2f,-4,0xffffe6a3,true,pose.last().pose(),buffers,Font.DisplayMode.NORMAL,0,net.minecraft.client.renderer.LightTexture.FULL_BRIGHT);
        pose.popPose();
    }
}
