package dev.anvilcraft.gtouming.doge_plus.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.stream.Stream;

/**
 * 镶嵌条目携带的额外数据。
 *
 * <p>镶嵌会消耗掉材料物品本身，凡是「之后还要用得上」的信息都必须跟着镶孔留下来。
 * 目前有两类：</p>
 * <ul>
 *   <li>{@link Potion} 药水效果——镶嵌药水后仍能还原出对应药水；</li>
 *   <li>{@link Enchantments} 附魔条数——中子锭按条数决定镶合产物。</li>
 * </ul>
 *
 * <p><b>扩展方式</b>：新增一个 record 实现本接口，自带 {@code CODEC} / {@code write} /
 * {@code read}，然后在 {@link #codecFor} 与 {@link #read} 的 switch 里各加一行即可
 * （sealed 接口 + 两处 switch，编译器会盯住遗漏）。JSON 形如
 * {@code {"type": "enchantments", "count": 3}}，网络按 {@code type} 字符串判别。</p>
 */
public sealed interface InlayExtra {

    /** JSON / 网络判别字段的取值。 */
    String type();

    /** 写入网络缓冲（不含 {@code type}，由 {@link #STREAM_CODEC} 负责）。 */
    void write(ByteBuf buffer);

    /**
     * 单个额外数据的 JSON 编解码：按 {@code type} 字段判别分发到各分支。
     *
     * <p>这里手写而非 {@code Codec#dispatch}，是为了在 sealed 接口上得到确定的泛型推断；
     * 新增分支只需在 {@link #codecFor} 与 {@link #read} 各加一行。</p>
     */
    MapCodec<InlayExtra> MAP_CODEC = new MapCodec<>() {
        @Override
        public <T> Stream<T> keys(DynamicOps<T> ops) {
            return Stream.of(ops.createString("type"));
        }

        @Override
        public <T> DataResult<InlayExtra> decode(DynamicOps<T> ops, MapLike<T> input) {
            T typeTag = input.get("type");
            if (typeTag == null) {
                return DataResult.error(() -> "Missing inlay extra type");
            }
            return ops.getStringValue(typeTag)
                    .flatMap(type -> codecFor(type).decode(ops, input));
        }

        @Override
        public <T> RecordBuilder<T> encode(InlayExtra input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
            prefix.add("type", ops.createString(input.type()));
            return codecFor(input.type()).encode(input, ops, prefix);
        }
    };

    /** 单个额外数据的 Codec 形态。 */
    Codec<InlayExtra> CODEC = MAP_CODEC.codec();

    /** 列表 JSON 编解码，供 {@code InlayEntry} 直接使用。 */
    Codec<List<InlayExtra>> LIST_CODEC = CODEC.listOf();

    /**
     * 单个额外数据的网络编解码。
     *
     * <p>读端按 {@code type} 判别串分派，因此写端必须<b>对称</b>地先把 {@code type} 写出去；
     * 漏写的话解码方会把各分支的数据当成判别串来读——例如附魔条数 {@code 15} 会被当成
     * 长度 15 的 UTF 串，抛 {@code Utf8String} 的「Not enough bytes in buffer」。</p>
     */
    StreamCodec<ByteBuf, InlayExtra> STREAM_CODEC = StreamCodec.of(
            (buffer, extra) -> {
                ByteBufCodecs.STRING_UTF8.encode(buffer, extra.type());
                extra.write(buffer);
            },
            InlayExtra::read);

    /** 列表网络编解码。 */
    StreamCodec<ByteBuf, List<InlayExtra>> LIST_STREAM_CODEC = STREAM_CODEC.apply(ByteBufCodecs.list());

    @SuppressWarnings("unchecked")
    private static MapCodec<InlayExtra> codecFor(String type) {
        if (Potion.TYPE.equals(type)) return (MapCodec<InlayExtra>) (MapCodec<?>) Potion.CODEC;
        if (Enchantments.TYPE.equals(type)) return (MapCodec<InlayExtra>) (MapCodec<?>) Enchantments.CODEC;
        throw new IllegalArgumentException("Unknown inlay extra type: " + type);
    }

    private static InlayExtra read(ByteBuf buffer) {
        String type = ByteBufCodecs.STRING_UTF8.decode(buffer);
        return switch (type) {
            case Potion.TYPE -> Potion.read(buffer);
            case Enchantments.TYPE -> Enchantments.read(buffer);
            default -> throw new IllegalArgumentException("Unknown inlay extra type: " + type);
        };
    }

    /** 药水效果：记录药水 id，取出镶嵌时据此还原药水。 */
    record Potion(ResourceLocation potion) implements InlayExtra {

        public static final String TYPE = "potion";

        public static final MapCodec<Potion> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("potion").forGetter(Potion::potion)
        ).apply(instance, Potion::new));

        @Override
        public String type() {
            return TYPE;
        }

        @Override
        public void write(ByteBuf buffer) {
            ResourceLocation.STREAM_CODEC.encode(buffer, potion);
        }

        private static Potion read(ByteBuf buffer) {
            return new Potion(ResourceLocation.STREAM_CODEC.decode(buffer));
        }
    }

    /** 附魔条数：只记条数（产物只跟条数有关，无需保留具体附魔）。 */
    record Enchantments(int count) implements InlayExtra {

        public static final String TYPE = "enchantments";

        public static final MapCodec<Enchantments> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.INT.fieldOf("count").forGetter(Enchantments::count)
        ).apply(instance, Enchantments::new));

        @Override
        public String type() {
            return TYPE;
        }

        @Override
        public void write(ByteBuf buffer) {
            ByteBufCodecs.VAR_INT.encode(buffer, count);
        }

        private static Enchantments read(ByteBuf buffer) {
            return new Enchantments(ByteBufCodecs.VAR_INT.decode(buffer));
        }
    }
}
