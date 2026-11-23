package dev.clothdollars;

import com.simibubi.create.content.logistics.tableCloth.TableClothBlockEntity;
import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@Mod("clothdollars")
public final class ClothDollars {
    public ClothDollars(IEventBus bus) {
        bus.addListener(ClothDollars::register);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, ClothDollars::click);
    }
    public record PriceEdit(BlockPos pos, String amount) implements CustomPacketPayload {
        static final Type<PriceEdit> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("clothdollars", "price"));
        static final StreamCodec<RegistryFriendlyByteBuf, PriceEdit> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, PriceEdit::pos, ByteBufCodecs.stringUtf8(64), PriceEdit::amount, PriceEdit::new);
        @Override public Type<PriceEdit> type() { return TYPE; }
    }
    private static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(PriceEdit.TYPE, PriceEdit.CODEC, (packet, context) -> {
            var player = context.player();
            if (!Prices.valid(packet.amount()) || player.isSpectator() || !player.mayBuild()
                    || !player.getMainHandItem().isEmpty() || !player.canInteractWithBlock(packet.pos(), 0)
                    || !(player.level().getBlockEntity(packet.pos()) instanceof TableClothBlockEntity cloth)
                    || !cloth.isShop() || !player.getUUID().equals(cloth.owner) || !cloth.priceTag.mayInteract(player)) return;
            cloth.priceTag.setFilter(ItemStack.EMPTY);
            ((Prices.Tag) cloth).clothdollars$set(packet.amount());
            cloth.notifyUpdate();
        });
    }
    private static void click(PlayerInteractEvent.RightClickBlock event) {
        var player = event.getEntity();
        if (event.getHand() != InteractionHand.MAIN_HAND || !player.getMainHandItem().isEmpty() || player.isSpectator()) return;
        var entity = event.getLevel().getBlockEntity(event.getPos());
        if (entity instanceof TableClothBlockEntity cloth && cloth.isShop() && player.mayBuild()
                && player.getUUID().equals(cloth.owner) && cloth.targetsPriceTag(player, event.getHitVec())) {
            if (event.getLevel().isClientSide() && FMLEnvironment.dist == Dist.CLIENT)
                dev.clothdollars.client.PriceScreen.open(cloth);
        } else if (!(entity instanceof StockTickerBlockEntity ticker && event.getLevel() instanceof ServerLevel level
                && Shop.collect(player, level, ticker))) return;
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
}
