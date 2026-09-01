package dev.anvilcraft.gtouming.doge_plus.entity;

import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.init.ModEntities;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus.CONFIG;

/**
 * 由 MagnetItem 发射的铁砧投射物。
 * <p>
 * 命中实体造成物理伤害（{@code generic()}），伤害 = 基础 + 目标标记数 × 加成。
 * 命中实体后持续飞行，直到命中方块落地并放置为铁砧方块。</p>
 */
public class FlyingAnvilEntity extends ThrowableProjectile {

    private static final EntityDataAccessor<String> BLOCK_ID =
            SynchedEntityData.defineId(FlyingAnvilEntity.class, EntityDataSerializers.STRING);

    @Nullable
    private UUID ownerUuid;
    /** 已命中过的实体 ID，避免重复伤害。 */
    private final Set<Integer> hitEntities = new HashSet<>();

    public FlyingAnvilEntity(EntityType<? extends ThrowableProjectile> type, Level level) {
        super(type, level);
    }

    public FlyingAnvilEntity(Level level) {
        this(ModEntities.FLYING_ANVIL.get(), level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(BLOCK_ID, "minecraft:anvil");
    }

    /**
     * 初始化发射参数。
     *
     * @param owner   发射者
     * @param anvilId 铁砧方块 ID
     */
    public void init(Player owner, ResourceLocation anvilId) {
        this.ownerUuid = owner.getUUID();
        this.getEntityData().set(BLOCK_ID, anvilId.toString());
        this.setPos(owner.getEyePosition());
        Vec3 look = owner.getLookAngle();
        this.shoot(look.x, look.y, look.z, (float) AnvilCraftDogePlus.CONFIG.anvilSpeed, 1.0F);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;
        if (this.tickCount <= CONFIG.flyLifetime) return;
        this.dropAnvilItem();
        this.discard();
    }

    @Override
    protected void onHit(HitResult result) {
        if (result.getType() == HitResult.Type.ENTITY) {
            // 命中实体：根据目标标记数造成伤害，继续飞行
            EntityHitResult ehr = (EntityHitResult) result;
            if (ehr.getEntity() instanceof LivingEntity target && this.hitEntities.add(target.getId())) {
                Player owner = this.ownerUuid != null ? this.level().getPlayerByUUID(this.ownerUuid) : null;
                if (target == owner) return;
                int damage = CONFIG.baseDamage;
                DamageSource source = owner != null
                        ? owner.damageSources().source(DamageTypes.FALLING_ANVIL)
                        : this.level().damageSources().source(DamageTypes.FALLING_ANVIL);
                target.hurt(source, damage);
            }
        }
        else if (result instanceof BlockHitResult) {
            this.dropAnvilItem();
        }
    }

    /** 掉落铁砧物品。 */
    private void dropAnvilItem() {
        if (this.level().isClientSide) return;
        ItemStack anvilItem = new ItemStack(this.getAnvilBlockState().getBlock().asItem());
        if (!anvilItem.isEmpty()) {
            this.level().addFreshEntity(new ItemEntity(this.level(), this.getX(), this.getY(), this.getZ(), anvilItem));
        }
    }

    // ===== Getter =====

    @Nullable
    public ResourceLocation getAnvilId() {
        return ResourceLocation.tryParse(this.getEntityData().get(BLOCK_ID));
    }

    public BlockState getAnvilBlockState() {
        ResourceLocation id = this.getAnvilId();
        if (id != null) {
            Block block = BuiltInRegistries.BLOCK.get(id);
            if (block != Blocks.AIR) return block.defaultBlockState();
        }
        return Blocks.ANVIL.defaultBlockState();
    }
}
