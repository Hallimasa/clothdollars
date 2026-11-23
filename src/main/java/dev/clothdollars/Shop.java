package dev.clothdollars;

import java.math.BigInteger;
import java.util.ArrayList;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.content.logistics.packagerLink.LogisticsManager;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBehaviour.RequestType;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;
import com.simibubi.create.content.logistics.tableCloth.ShoppingListItem;
import com.simibubi.create.content.logistics.tableCloth.ShoppingListItem.ShoppingList;
import com.simibubi.create.content.logistics.tableCloth.TableClothBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

public final class Shop {
    private static final String QUOTE = "clothdollars_quote";
    public static String quote(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString(QUOTE);
    }
    static void status(Player p, String message) {
        p.displayClientMessage(Component.literal(message), true);
    }
    /** Return true only when the bridge handles this click. */
    public static boolean add(TableClothBlockEntity cloth, Player player) {
        ItemStack hand = player.getMainHandItem();
        int slot = player.getInventory().selected;
        if (ShoppingListItem.getList(hand) == null) {
            for (int i = 0; i < 9; i++) {
                if (ShoppingListItem.getList(player.getInventory().getItem(i)) != null) {
                    slot = i;
                    break;
                }
            }
        }
        ItemStack previous = player.getInventory().getItem(slot);
        ShoppingList list = ShoppingListItem.getList(previous);
        String old = quote(previous);
        String price = Prices.price(cloth);
        if (price.isEmpty() && old.isEmpty()) return false;
        if (list != null && price.isEmpty() != old.isEmpty()) {
            status(player, "Use one payment type per shopping list.");
            return true;
        }
        if (!hand.isEmpty() && ShoppingListItem.getList(hand) == null) {
            status(player, "Use an empty hand or your shopping list.");
            return true;
        }
        BlockPos pos = cloth.getBlockPos();
        if (!(cloth.getLevel().getBlockEntity(cloth.requestData.targetOffset().offset(pos)) instanceof StockTickerBlockEntity ticker)
                || !ticker.isKeeperPresent() || cloth.owner == null) {
            status(player, "Shopkeeper unavailable.");
            return true;
        }
        if (list == null) list = new ShoppingList(new ArrayList<>(), cloth.owner, ticker.behaviour.freqId);
        if (!cloth.owner.equals(list.shopOwner()) || !ticker.behaviour.freqId.equals(list.shopNetwork())
                || list.getPurchases(pos) >= cloth.getStockLevelForTrade(list)) {
            status(player, "Check this shop's network and available stock.");
            return true;
        }
        var next = new ShoppingList.Mutable(list.duplicate());
        next.addPurchases(pos, 1);
        var result = ShoppingListItem.saveList(AllItems.SHOPPING_LIST.asStack(), next.toImmutable(), cloth.requestData.encodedTargetAddress());
        BigInteger total = new BigInteger(price).add(old.isEmpty() ? BigInteger.ZERO : new BigInteger(old));
        CustomData.update(DataComponents.CUSTOM_DATA, result, tag -> tag.putString(QUOTE, total.toString()));
        player.getInventory().setItem(slot, ItemStack.EMPTY);
        player.setItemInHand(InteractionHand.MAIN_HAND, result);
        status(player, "Shopping list total: $" + total);
        return true;
    }

    public static boolean checkout(Player player, Level level, BlockPos pos, ItemStack stack) {
        String quoted = quote(stack);
        ShoppingList list = ShoppingListItem.getList(stack);
        if (!(level instanceof ServerLevel server) || list == null) return false;
        if (quoted.isEmpty()) {
            // Create resolves item prices live. Refuse an old emerald list whose tag was cleared.
            for (var line : list.purchases()) {
                if (level.getBlockEntity(line.getValue()) instanceof TableClothBlockEntity cloth && !Prices.price(cloth).isEmpty()) {
                    status(player, "The payment type changed. Start a new shopping list.");
                    return true;
                }
            }
            return false;
        }
        if (!(level.getBlockEntity(pos) instanceof StockTickerBlockEntity ticker)
                || !ticker.behaviour.freqId.equals(list.shopNetwork())) {
            status(player, "This list belongs to another shop network.");
            return true;
        }
        BigInteger cost = new BigInteger(quoted);
        BigInteger balance = Prices.balance(player);
        if (cost.signum() <= 0 || balance.compareTo(cost) < 0) {
            status(player, "Insufficient CobbleDollars.");
            return true;
        }
        for (var line : list.purchases()) {
            if (!(level.getBlockEntity(line.getValue()) instanceof TableClothBlockEntity cloth)
                    || !cloth.isShop() || !list.shopOwner().equals(cloth.owner)
                    || !(level.getBlockEntity(cloth.requestData.targetOffset().offset(line.getValue())) instanceof StockTickerBlockEntity source)
                    || !list.shopNetwork().equals(source.behaviour.freqId)) {
                status(player, "A listed offer is unavailable.");
                return true;
            }
        }
        var order = list.bakeEntries(level, null).getFirst().getStacksByCount();
        ticker.getAccurateSummary();
        var requests = LogisticsManager.findPackagersForRequest(list.shopNetwork(),
            PackageOrderWithCrafts.simple(order), null, ShoppingListItem.getAddress(stack));
        InventorySummary available = new InventorySummary();
        for (var request : requests.values()) available.add(request.item(), request.getCount());
        if (order.isEmpty() || order.stream().anyMatch(line -> line.count <= 0 || available.getCountOf(line.stack) != line.count)
                || requests.keySet().stream().anyMatch(packager -> packager.isTooBusyFor(RequestType.PLAYER))) {
            status(player, "The packager cannot fill this order right now.");
            return true;
        }
        Earnings earnings = Earnings.get(server);
        BigInteger pending = earnings.amount(list.shopNetwork(), list.shopOwner());
        LogisticsManager.performPackageRequests(requests);
        Prices.balance(player, balance.subtract(cost));
        earnings.put(list.shopNetwork(), list.shopOwner(), pending.add(cost));
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        AllSoundEvents.STOCK_TICKER_TRADE.playOnServer(level, pos);
        return true;
    }
    static boolean collect(Player player, ServerLevel level, StockTickerBlockEntity ticker) {
        Earnings ledger = Earnings.get(level);
        var network = ticker.behaviour.freqId;
        var seller = player.getUUID();
        var pending = ledger.amount(network, seller);
        if (pending.signum() == 0 || !ticker.behaviour.mayInteract(player)) return false;
        Prices.balance(player, Prices.balance(player).add(pending));
        ledger.put(network, seller, BigInteger.ZERO);
        status(player, "Collected $" + pending);
        return true;
    }
}
