package dev.clothdollars;

import java.math.BigInteger;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Network identity survives ticker movement. Each seller owns a separate balance. */
final class Earnings extends SavedData {
    private final CompoundTag accounts;
    private Earnings(CompoundTag accounts) {
        this.accounts = accounts;
    }
    static Earnings get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
            new Factory<>(() -> new Earnings(new CompoundTag()), (tag, lookup) -> new Earnings(tag.copy())), "clothdollars_earnings");
    }
    BigInteger amount(UUID network, UUID seller) {
        String text = accounts.getString(network + "/" + seller);
        return text.isEmpty() ? BigInteger.ZERO : new BigInteger(text);
    }
    void put(UUID network, UUID seller, BigInteger value) {
        String key = network + "/" + seller;
        if (value.signum() == 0) accounts.remove(key);
        else accounts.putString(key, value.toString());
        setDirty();
    }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider lookup) {
        return tag.merge(accounts);
    }
}
