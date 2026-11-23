package dev.clothdollars;

import java.math.BigInteger;
import com.simibubi.create.content.logistics.tableCloth.TableClothBlockEntity;
import fr.harmex.cobbledollars.common.utils.extensions.PlayerExtensionKt;
import net.minecraft.world.entity.player.Player;

public final class Prices {
    public interface Tag {
        String clothdollars$get();
        void clothdollars$set(String price);
    }
    public static String price(TableClothBlockEntity cloth) {
        return ((Tag) cloth).clothdollars$get();
    }
    public static boolean valid(String value) {
        return value.matches("[1-9][0-9]{0,63}");
    }
    public static BigInteger balance(Player player) {
        return PlayerExtensionKt.getCobbleDollars(player);
    }
    public static void balance(Player player, BigInteger value) {
        PlayerExtensionKt.setCobbleDollars(player, value);
    }
}
