package dev.clothdollars;

import com.simibubi.create.content.logistics.stockTicker.StockTickerBlock;
import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

@WailaPlugin
public final class JadeShopPlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(Payments.INSTANCE, StockTickerBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(Payments.INSTANCE, StockTickerBlock.class);
    }

    private enum Payments implements IServerDataProvider<BlockAccessor>, IBlockComponentProvider {
        INSTANCE;

        private static final String KEY = "clothdollars_pending";
        private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("clothdollars", "pending_payments");

        @Override
        public ResourceLocation getUid() {
            return ID;
        }

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (!(accessor.getLevel() instanceof ServerLevel level)
                    || !(accessor.getBlockEntity() instanceof StockTickerBlockEntity ticker)
                    || !ticker.behaviour.mayInteract(accessor.getPlayer())) return;
            var pending = Earnings.get(level).amount(ticker.behaviour.freqId, accessor.getPlayer().getUUID());
            if (pending.signum() > 0) data.putString(KEY, pending.toString());
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            String pending = accessor.getServerData().getString(KEY);
            if (!pending.isEmpty()) {
                tooltip.add(Component.literal("CobbleDollars to collect: $" + pending).withStyle(ChatFormatting.GOLD));
            }
        }
    }
}
