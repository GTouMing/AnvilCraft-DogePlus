package dev.anvilcraft.gtouming.doge_plus.mixin;

import dev.anvilcraft.gtouming.doge_plus.api.entity.ICaptured;
import dev.anvilcraft.gtouming.doge_plus.init.ModDataComponentTypes;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.InlayProperty;
import dev.anvilcraft.gtouming.doge_plus.data.BlockInlayManager;
import dev.anvilcraft.gtouming.doge_plus.data.ClientBlockInlayData;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.InlayUtil;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.entity.MagnetizedNodeEntity;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.minecraft.network.syncher.SynchedEntityData.*;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin extends Entity implements ICaptured {

    @Unique
    private static final EntityDataAccessor<Boolean> doge_plus$CAPTURED;
    @Unique
    private static final EntityDataAccessor<Integer> doge_plus$INDEX;

    @Shadow
    public abstract ItemStack getItem();

    @Shadow
    public int lifespan;

    public ItemEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * 镶嵌「耐火」性质：免疫火焰与岩浆伤害。
     */
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void doge_plus$fireProofInlay(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        ItemStack stack = this.getItem();
        if (!stack.isEmpty()
                && source.is(DamageTypeTags.IS_FIRE)
                && InlayUtil.hasProperty(stack, InlayProperty.FIRE_PROOF)) {
            cir.setReturnValue(false);
        }
    }

    @Override
    public boolean doge_plus$isCaptured() {
        return this.getEntityData().get(doge_plus$CAPTURED);
    }

    @Override
    public void doge_plus$setCaptured(boolean captured) {
        this.getEntityData().set(doge_plus$CAPTURED, captured);
    }

    @Override
    public int doge_plus$getIndex() {
        return this.getEntityData().get(doge_plus$INDEX);
    }

    @Override
    public void doge_plus$setIndex(int index) {
        this.getEntityData().set(doge_plus$INDEX, index);
    }

    @Inject(method = "defineSynchedData", at = @At(value = "TAIL"))
    protected void doge_plus$defineSynchedData(Builder builder, CallbackInfo ci) {
        builder.define(doge_plus$CAPTURED, false).define(doge_plus$INDEX, 0);
    }

    /**
     * 重定向 move 调用，被捕获时执行自定义移动
     */
    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/item/ItemEntity;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"
            )
    )
    private void doge_plus$redirectMove(ItemEntity itemEntity, MoverType moverType, Vec3 vec3) {
        if (!doge_plus$isCaptured()) itemEntity.move(moverType, vec3);
        //move方法中热方块点燃原油锅的必要检测
        if (!this.isRemoved()) this.tryCheckInsideBlocks();
    }

    @Inject(method = "tick", at = @At(value = "HEAD"))
    private void onTickRemove(CallbackInfo ci) {
        if (this.level().isClientSide) return;
        if (!this.doge_plus$isCaptured()) return;
        if (this.level().getEntitiesOfClass(MagnetizedNodeEntity.class,
                new AABB(this.position(), this.position()).inflate(0.6)).isEmpty()) {
            this.doge_plus$setCaptured(false);
            ((ItemEntity) Util.cast(this)).setNoPickUpDelay();
        }
    }
    @Unique private static final Map<String, Double> MATERIAL_MAP = new HashMap<>();
    @Unique private static final Map<String, String> SPECIAL_MAP = new HashMap<>();
    @Unique private static final List<String> SPECIAL_BLACKLIST = List.of("spawn_egg", "waxed");

    static {
        // 1. 定义材质关键词及其减速 (数值越小越慢)
        MATERIAL_MAP.put("iron", 0.50);
        MATERIAL_MAP.put("magnet", 0.50);
        MATERIAL_MAP.put("steel", 0.75);

        MATERIAL_MAP.put("silver", 0.25);
        MATERIAL_MAP.put("copper", 0.27);
        MATERIAL_MAP.put("gold", 0.28);
        MATERIAL_MAP.put("netherite", 0.30);
        MATERIAL_MAP.put("ember", 0.30);
        MATERIAL_MAP.put("aluminum", 0.30);
        MATERIAL_MAP.put("tungsten", 0.38);
        MATERIAL_MAP.put("zinc", 0.40);
        MATERIAL_MAP.put("brass", 0.42);
        MATERIAL_MAP.put("bronze", 0.45);
        MATERIAL_MAP.put("royal", 0.50);
        MATERIAL_MAP.put("tin", 0.55);
        MATERIAL_MAP.put("lead", 0.65);
        MATERIAL_MAP.put("uranium", 0.80);
        MATERIAL_MAP.put("titanium", 0.88);
        MATERIAL_MAP.put("frost_metal", 0.90);
        MATERIAL_MAP.put("plutonium", 0.99);
        // 在这里继续添加材料...

        // 2. 将不含关键词的物品映射到上述材质
        SPECIAL_MAP.put("lightning_rod", "copper");
        SPECIAL_MAP.put("bucket", "iron");
        SPECIAL_MAP.put("hopper", "iron");
        SPECIAL_MAP.put("shears", "iron");
        SPECIAL_MAP.put("anvil", "iron");
        SPECIAL_MAP.put("minecart", "iron");
        SPECIAL_MAP.put("tripwire_hook", "iron");
        SPECIAL_MAP.put("chain", "iron");
        SPECIAL_MAP.put("chute", "iron");
        SPECIAL_MAP.put("compass", "iron");
        // 在这里继续添加特判...
    }

    @SuppressWarnings("checkstyle:NeedBraces")
    @Unique
    private @Nullable String doge_plus$getMaterialKey(ItemStack stack) {
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        for (String black : SPECIAL_BLACKLIST) if (id.contains(black)) return null; // 黑名单检查
        if (SPECIAL_MAP.containsKey(id)) return SPECIAL_MAP.get(id); // 别名/特判检查
        for (String key : MATERIAL_MAP.keySet()) { // 关键词匹配
            if (id.contains(key)) return key;
        }
        return null;
    }

    @Unique
    private boolean doge_plus$isTouchingBlock() {
        AABB box = this.getBoundingBox().inflate(0.01);
        return BlockPos.betweenClosedStream(box).anyMatch(p -> {
            BlockState s = this.level().getBlockState(p);
            return BlockInlayManager.hasProperty(this.level(), p, InlayProperty.MAGNETIC)
                    && !s.getCollisionShape(this.level(), p).isEmpty()
                    && s.getCollisionShape(this.level(), p).toAabbs().stream().anyMatch(b -> b.move(p).intersects(box));
        });
    }

    /** 计算指向最近带磁性镶嵌方块的吸附向量。 */
    @Unique
    private Vec3 doge_plus$findInlaiedBlockAttraction() {
        Vec3 center = this.getBoundingBox().getCenter();
        AABB area = this.getBoundingBox().inflate(0.5);
        Object[] result = {null, Double.MAX_VALUE};
        BlockPos.betweenClosedStream(area).forEach(pos -> {
            if (!BlockInlayManager.hasProperty(this.level(), pos, InlayProperty.MAGNETIC)) return;
            for (AABB box : this.level().getBlockState(pos).getCollisionShape(this.level(), pos).toAabbs()) {
                AABB wb = box.move(pos);
                Vec3 p = new Vec3(
                        Mth.clamp(center.x, wb.minX, wb.maxX),
                        Mth.clamp(center.y, wb.minY, wb.maxY),
                        Mth.clamp(center.z, wb.minZ, wb.maxZ)
                );
                double dist = center.distanceToSqr(p);
                if (dist < (double) result[1]) {
                    result[1] = dist;
                    result[0] = p;
                }
            }
        });
        return result[0] != null && (double) result[1] > 1.0E-7
                ? ((Vec3) result[0]).subtract(center).normalize().scale(0.05)
                : Vec3.ZERO;
    }

    /**
     * 磁性方块吸附（双端一致执行）：客户端也执行相同的吸附逻辑，
     * 配合同步的磁性方块数据（{@link ClientBlockInlayData}），
     * 使客户端物品本地预测与服务端一致，避免双端不同步导致的抖动/瞬移。
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void doge_plus$magnetLogic(CallbackInfo ci) {
        if (this.isRemoved()) return;

        ItemStack stack = this.getItem();
        String matKey = doge_plus$getMaterialKey(stack);
        // 不是金属直接跳过
        if (matKey == null) return;
        // 1. 空芯磁铁块转化 本体负责
        if ("iron".equals(matKey) || "magnet".equals(matKey)) {
            // 2. 吸铁石就要吸铁
            if (doge_plus$isTouchingBlock()) {
                this.setDeltaMovement(Vec3.ZERO);
                this.setNoGravity(true);
                this.setOnGround(true);
            } else {
                if (this.isNoGravity() && !stack.has(ModComponents.ETERNAL)) this.setNoGravity(false);
                if (doge_plus$findInlaiedBlockAttraction().lengthSqr() > 0)
                    this.addDeltaMovement(doge_plus$findInlaiedBlockAttraction());
            }
        }
        // 3. 涡流减速 本体负责
    }

    /** 镶嵌「高温」性质：在熔岩或火中累加伤害值。 */
    @Inject(method = "tick", at = @At(value = "HEAD"))
    private void doge_plus$highTempTick(CallbackInfo ci) {
        if (this.tickCount % 20 != 0) return;
        ItemStack stack = this.getItem();
        if (stack.isEmpty() || !InlayUtil.hasProperty(stack, InlayProperty.HIGH_TEMP)) return;
        if (this.isInLava() || this.isOnFire()) {
            // 共鸣：累加更快
            int add = InlayUtil.hasProperty(stack, InlayProperty.RESONANCE) ? 2 : 1;
            stack.set(ModDataComponentTypes.HEAT, Math.min(stack.getOrDefault(ModDataComponentTypes.HEAT, 0) + add, 100));
        }
    }

    /** 镶嵌「冷锻」性质：在水中或细雪中缓慢回复耐久（仅耐久物品）。 */
    @Inject(method = "tick", at = @At(value = "HEAD"))
    private void doge_plus$coldForgedTick(CallbackInfo ci) {
        if (this.tickCount % 20 != 0) return;
        ItemStack stack = this.getItem();
        if (stack.isEmpty() || !InlayUtil.hasProperty(stack, InlayProperty.COLD_FORGED)) return;
        if (stack.getMaxDamage() <= 0 || stack.getDamageValue() <= 0) return;
        if (!this.isInWater() && !this.isInPowderSnow) return;
        // 共鸣：回复更快
        int repair = InlayUtil.hasProperty(stack, InlayProperty.RESONANCE) ? 2 : 1;
        stack.setDamageValue(Math.max(0, stack.getDamageValue() - repair));
    }

    /** 镶嵌「永恒」性质：免疫火焰、爆炸、仙人掌与虚空伤害。 */
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void doge_plus$eternalHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        ItemStack stack = this.getItem();
        if (stack.isEmpty() || !InlayUtil.hasProperty(stack, InlayProperty.ETERNAL)) return;
        if (source.is(DamageTypeTags.IS_FIRE)
                || source.is(DamageTypeTags.IS_EXPLOSION)
                || source.is(DamageTypes.CACTUS)
                || source.is(DamageTypes.FELL_OUT_OF_WORLD)) {
            cir.setReturnValue(false);
        }
    }

    /** 镶嵌「永恒」性质：时间免疫（不消失）与虚空免疫（漂浮）。 */
    @Inject(method = "tick", at = @At(value = "HEAD"))
    private void doge_plus$eternalTick(CallbackInfo ci) {
        ItemStack stack = this.getItem();
        if (stack.isEmpty() || !InlayUtil.hasProperty(stack, InlayProperty.ETERNAL)) return;
        this.lifespan = Integer.MAX_VALUE;
        if (this.getY() < this.level().getMinBuildHeight() + 5) {
            this.addDeltaMovement(new Vec3(0.0, 0.05, 0.0));
        }
    }

    static {
         doge_plus$CAPTURED = defineId(ItemEntity.class, EntityDataSerializers.BOOLEAN);
         doge_plus$INDEX = defineId(ItemEntity.class, EntityDataSerializers.INT);
    }
}
