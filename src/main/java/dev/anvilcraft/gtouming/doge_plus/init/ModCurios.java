package dev.anvilcraft.gtouming.doge_plus.init;

import dev.anvilcraft.gtouming.doge_plus.api.curios.ICurios;
import dev.anvilcraft.gtouming.doge_plus.curios.CuriosDisappear;
import dev.anvilcraft.gtouming.doge_plus.curios.CuriosExist;
import net.neoforged.fml.ModList;

public class ModCurios {
    public static ICurios ICURIOS;

    public static void register() {
        ICURIOS = ModList.get().isLoaded("curios") ? new CuriosExist() : new CuriosDisappear();
        ICURIOS.register();
    }
}
