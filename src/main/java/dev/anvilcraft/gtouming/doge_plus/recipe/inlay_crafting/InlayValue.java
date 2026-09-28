package dev.anvilcraft.gtouming.doge_plus.recipe.inlay_crafting;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;

/**
 * 镶合产物里可用的数值：{@code 基数 × 乘数}。
 *
 * <p>基数是「被乘的东西」：一个固定值，或者附魔数量 {@code enchants_count}。
 * JSON 写法：</p>
 * <ul>
 *   <li>固定值直接写数字，如 {@code "count": 4}（等价基数 4、乘数 1）；</li>
 *   <li>随附魔数量变化时写对象，基数与乘数都写明，如
 *       {@code {"base": "enchants_count", "multiplier": 1}}（= n）、
 *       {@code {"base": "enchants_count", "multiplier": 3}}（= 3n）、
 *       {@code {"base": "enchants_count", "multiplier": 0.1}}（= 0.1n）。</li>
 * </ul>
 *
 * <p>{@code n} 取基材孔内材料的附魔条数（见
 * {@link InlayCraftingRecipe#enchantmentCount(net.minecraft.world.item.ItemStack)}）。</p>
 */
public record InlayValue(Base base, double multiplier) {

    /** 固定的 1。 */
    public static final InlayValue ONE = new InlayValue(new Base.Constant(1), 1);

    /** 固定值（乘数 1）。 */
    public static InlayValue of(double value) {
        return new InlayValue(new Base.Constant(value), 1);
    }

    /** 基数取附魔数量，乘数给定（如 {@code 3n}、{@code 0.1n}）。 */
    public static InlayValue perEnchantment(double multiplier) {
        return new InlayValue(Base.EnchantsCount.INSTANCE, multiplier);
    }

    /** 按附魔数量求值：{@code 基数(附魔数) × 乘数}。 */
    public double evaluate(int enchantsCount) {
        return base.resolve(enchantsCount) * multiplier;
    }

    /** 是否随附魔数量变化（基数为附魔数量）。 */
    public boolean isEnchantScaled() {
        return base instanceof Base.EnchantsCount;
    }

    /** 数值的基数：固定值，或附魔数量。 */
    public sealed interface Base permits Base.Constant, Base.EnchantsCount {

        /** 代表附魔数量的符号。 */
        String VARIABLE = "enchants_count";

        double resolve(int enchantsCount);

        /** 固定值。 */
        record Constant(double value) implements Base {
            @Override
            public double resolve(int enchantsCount) {
                return value;
            }
        }

        /** 附魔数量。 */
        record EnchantsCount() implements Base {

            public static final EnchantsCount INSTANCE = new EnchantsCount();

            @Override
            public double resolve(int enchantsCount) {
                return enchantsCount;
            }
        }

        Codec<Base> CODEC = Codec.either(
                NUMBER_CODEC.xmap(Constant::new, Constant::value),
                Codec.STRING.flatXmap(
                        name -> name.equals(VARIABLE)
                                ? DataResult.success(EnchantsCount.INSTANCE)
                                : DataResult.error(() -> "Unknown inlay value base: " + name),
                        base -> DataResult.success(VARIABLE))
        ).xmap(
                either -> either.left().<Base>map(constant -> constant)
                        .orElseGet(() -> either.right().orElseThrow()),
                base -> base instanceof Constant constant
                        ? Either.<Constant, EnchantsCount>left(constant)
                        : Either.<Constant, EnchantsCount>right((EnchantsCount) base));
    }

    // ==================== 编解码 ====================

    /**
     * 数字：读入一律取 {@code double}，写出时整数写成 {@code 3}、非整数写成 {@code 0.1}。
     *
     * <p>注意<b>不能</b>用 {@code Codec.either(Codec.INT, Codec.DOUBLE)}：{@code Codec.INT}
     * 会把 {@code 0.1} 直接截断成 {@code 0}（不再落到 double 分支），导致
     * {@code "multiplier": 0.1} 读成 0。</p>
     */
    private static final Codec<Double> NUMBER_CODEC = new Codec<>() {
        @Override
        public <T> DataResult<Pair<Double, T>> decode(DynamicOps<T> ops, T input) {
            return ops.getNumberValue(input).map(value -> Pair.of(value.doubleValue(), ops.empty()));
        }

        @Override
        public <T> DataResult<T> encode(Double input, DynamicOps<T> ops, T prefix) {
            double value = input;
            T encoded = !Double.isInfinite(value) && value == Math.rint(value)
                    ? ops.createInt((int) value)
                    : ops.createDouble(value);
            return ops.mergeToPrimitive(prefix, encoded);
        }
    };

    /** 完整写法：{@code {"base": 基数, "multiplier": 乘数}}（两字段都必填）。 */
    private static final MapCodec<InlayValue> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Base.CODEC.fieldOf("base").forGetter(InlayValue::base),
            NUMBER_CODEC.fieldOf("multiplier").forGetter(InlayValue::multiplier)
    ).apply(instance, InlayValue::new));

    /** 产物数量：固定正整数简写，或 {@code {"base":…, "multiplier":…}}。 */
    public static final Codec<InlayValue> COUNT_CODEC = Codec.either(ExtraCodecs.POSITIVE_INT, MAP_CODEC.codec()).xmap(
            either -> either.map(InlayValue::of, value -> value),
            InlayValue::splitForCount);

    /** 出现概率：固定浮点简写，或 {@code {"base":…, "multiplier":…}}。 */
    public static final Codec<InlayValue> CHANCE_CODEC = Codec.either(Codec.floatRange(0f, 1f), MAP_CODEC.codec()).xmap(
            either -> either.map(InlayValue::of, value -> value),
            InlayValue::splitForChance);

    private static Either<Integer, InlayValue> splitForCount(InlayValue value) {
        return isBareConstant(value)
                ? Either.left((int) Math.round(value.evaluate(0)))
                : Either.right(value);
    }

    private static Either<Float, InlayValue> splitForChance(InlayValue value) {
        return isBareConstant(value)
                ? Either.left((float) value.evaluate(0))
                : Either.right(value);
    }

    /** 固定值、乘数 1 时可退化成裸数字简写。 */
    private static boolean isBareConstant(InlayValue value) {
        return value.base() instanceof Base.Constant && value.multiplier() == 1.0;
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, InlayValue> STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> {
                buffer.writeBoolean(value.base() instanceof Base.EnchantsCount);
                buffer.writeDouble(value.base().resolve(0));
                buffer.writeDouble(value.multiplier());
            },
            buffer -> {
                boolean enchantsCount = buffer.readBoolean();
                double constant = buffer.readDouble();
                double multiplier = buffer.readDouble();
                return new InlayValue(
                        enchantsCount ? Base.EnchantsCount.INSTANCE : new Base.Constant(constant),
                        multiplier);
            });
}
