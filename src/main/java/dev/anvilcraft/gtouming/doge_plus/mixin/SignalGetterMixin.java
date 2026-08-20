package dev.anvilcraft.gtouming.doge_plus.mixin;

import dev.anvilcraft.gtouming.doge_plus.data.LogicGateManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SignalGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SignalGetter.class)
public interface SignalGetterMixin {

    /**
     * 完全接管 getDirectSignal（强信号）。
     */
    @Inject(
            method = "getDirectSignalTo",
            at = @At("RETURN"),
            cancellable = true
    )
    private void doge_plus$onGetDirectSignal(
            BlockPos pos, CallbackInfoReturnable<Integer> cir
    ) {
        Level level = (Level) this;
        if (level.isClientSide()) return;

        LogicGateManager manager = LogicGateManager.get(level);
        if (manager == null) return;

        if (!manager.hasLogicGate(pos)) return;

        cir.setReturnValue(0);
    }
}