package dev.clothdollars.mixin;

import java.math.BigInteger;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import fr.harmex.cobbledollars.common.utils.extensions.BigIntegerExtensionsKt;
import com.simibubi.create.content.logistics.tableCloth.TableClothBlockEntity;
import com.simibubi.create.content.logistics.tableCloth.TableClothRenderer;
import dev.clothdollars.Prices;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=TableClothRenderer.class, remap=false)
abstract class TagRenderer {
    @Inject(method="renderSafe(Lcom/simibubi/create/content/logistics/tableCloth/TableClothBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V", at=@At("TAIL"))
    private void price(TableClothBlockEntity cloth,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay,CallbackInfo ci) {
        String value = Prices.price(cloth);
        if (value.isEmpty()) return;
        var label = BigIntegerExtensionsKt.format(new BigInteger(value), true).getVisualOrderText();
        Font font = Minecraft.getInstance().font;
        int width = font.width(label);
        float scale = Math.min(0.03f, 0.72f / (width + 2));

        // Keep the text upright in both positions. Place the front label past the tag model.
        pose.pushPose();
        pose.translate(0.5, cloth.sideOccluded ? 0.18 : -0.17, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(180 - cloth.facing.toYRot()));
        pose.translate(0, 0, cloth.sideOccluded ? -0.39 : -0.60);
        pose.scale(-scale, -scale, scale);
        font.drawInBatch8xOutline(label, -width / 2f, -4, 0xffffffff, 0xff000000,
            pose.last().pose(), buffers, LightTexture.FULL_BRIGHT);
        pose.popPose();
    }
}
