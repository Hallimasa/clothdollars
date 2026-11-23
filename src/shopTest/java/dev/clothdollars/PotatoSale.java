package dev.clothdollars;

import java.math.BigInteger;
import java.util.List;
import java.util.UUID;
import com.mojang.authlib.GameProfile;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.packager.PackagerBlock;
import com.simibubi.create.content.logistics.packagerLink.PackagerLinkBlockEntity;
import com.simibubi.create.content.logistics.redstoneRequester.AutoRequestData;
import com.simibubi.create.content.logistics.stockTicker.*;
import com.simibubi.create.content.logistics.tableCloth.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.*;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("clothdollars")
@PrefixGameTestTemplate(false)
public final class PotatoSale {
    @GameTest(template="shop", timeoutTicks=180)
    public static void potatoSale(GameTestHelper test) {
        var level = test.getLevel();
        BlockPos chest = new BlockPos(1,1,1), pack = chest.east(), link = pack.above();
        BlockPos tickerPos = pack.south(2), table = tickerPos.east(2);
        test.setBlock(chest, Blocks.CHEST);
        ((ChestBlockEntity)test.getBlockEntity(chest)).setItem(0,new ItemStack(Items.POTATO,2));
        test.setBlock(pack,AllBlocks.PACKAGER.getDefaultState().setValue(PackagerBlock.FACING,Direction.EAST));
        test.setBlock(link,AllBlocks.STOCK_LINK.getDefaultState().setValue(BlockStateProperties.ATTACH_FACE,AttachFace.FLOOR));
        var stockLink = (PackagerLinkBlockEntity)test.getBlockEntity(link);
        test.setBlock(tickerPos,AllBlocks.STOCK_TICKER.getDefaultState());
        var ticker = (StockTickerBlockEntity)test.getBlockEntity(tickerPos);
        ticker.behaviour.freqId = stockLink.behaviour.freqId;
        var seat = tickerPos.west();
        test.setBlock(seat,AllBlocks.SEATS.get(DyeColor.GREEN).getDefaultState());
        var keeper = test.spawn(net.minecraft.world.entity.EntityType.VILLAGER,seat);
        com.simibubi.create.content.contraptions.actors.seat.SeatBlock.sitDown(level,test.absolutePos(seat),keeper);
        test.setBlock(table,AllBlocks.TABLE_CLOTHS.get(DyeColor.BLACK).getDefaultState().setValue(TableClothBlock.HAS_BE,true));
        var cloth = (TableClothBlockEntity)test.getBlockEntity(table);
        var buyer = FakePlayerFactory.get(level,new GameProfile(UUID.randomUUID(),"Buyer"));
        var seller = FakePlayerFactory.get(level,new GameProfile(UUID.randomUUID(),"Seller"));
        cloth.owner = seller.getUUID();
        cloth.requestData = new AutoRequestData(PackageOrderWithCrafts.simple(List.of(new BigItemStack(new ItemStack(Items.POTATO),1))),
            "",ticker.getBlockPos().subtract(cloth.getBlockPos()),level.dimension().location().toString(),true);
        test.runAfterDelay(45,() -> {
            ((Prices.Tag)cloth).clothdollars$set("1000");
            cloth.useShop(buyer);
            test.assertTrue(Shop.quote(buyer.getMainHandItem()).equals("1000"),"first quote: keeper="+ticker.isKeeperPresent()+" stock="+cloth.getStockLevelForTrade(null)+" linked="+(stockLink.getPackager()!=null)+" hand="+buyer.getMainHandItem());
            ((Prices.Tag)cloth).clothdollars$set("5000");
            cloth.useShop(buyer);
            test.assertTrue(Shop.quote(buyer.getMainHandItem()).equals("6000"),"second quote");
            var rival = FakePlayerFactory.get(level,new GameProfile(UUID.randomUUID(),"SecondBuyer"));
            cloth.useShop(rival);
            Prices.balance(rival,BigInteger.valueOf(5000));
            // Item components must survive inventory save/load with the exact quote.
            var restored = ItemStack.parseOptional(level.registryAccess(),
                (net.minecraft.nbt.CompoundTag)buyer.getMainHandItem().save(level.registryAccess()));
            buyer.setItemInHand(InteractionHand.MAIN_HAND,restored);
            test.assertTrue(Shop.quote(restored).equals("6000"),"saved quote");
            ((Prices.Tag)cloth).clothdollars$set("");
            cloth.priceTag.setFilter(new ItemStack(Items.EMERALD));
            cloth.useShop(buyer);
            test.assertTrue(buyer.getMainHandItem() == restored,"mixed payment keeps the original list");
            Prices.balance(buyer,BigInteger.ZERO);
            StockTickerInteractionHandler.interactWithLogisticsManagerAt(buyer,level,ticker.getBlockPos());
            test.assertTrue(!buyer.getMainHandItem().isEmpty(),"failed payment retains list");
            Prices.balance(buyer,BigInteger.valueOf(7000));
            StockTickerInteractionHandler.interactWithLogisticsManagerAt(buyer,level,ticker.getBlockPos());
            test.assertTrue(buyer.getMainHandItem().isEmpty() && Prices.balance(buyer).intValueExact()==1000,"checkout");
            test.assertTrue(((ChestBlockEntity)test.getBlockEntity(chest)).getItem(0).isEmpty(),"packager extracted potatoes");
            StockTickerInteractionHandler.interactWithLogisticsManagerAt(rival,level,ticker.getBlockPos());
            test.assertTrue(Prices.balance(rival).intValueExact()==5000 && !rival.getMainHandItem().isEmpty(),"unavailable stock preserves payment and list");
            var before = Prices.balance(seller);
            test.assertTrue(!Shop.collect(buyer,level,ticker) && Shop.collect(seller,level,ticker),"owner collection");
            test.assertTrue(Prices.balance(seller).subtract(before).intValueExact()==6000 && !Shop.collect(seller,level,ticker),"single payout");
            test.succeed();
        });
    }
}
