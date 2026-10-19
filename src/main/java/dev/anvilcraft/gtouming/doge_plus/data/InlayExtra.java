package dev.anvilcraft.gtouming.doge_plus.data;

import com.mojang.datafixers.util.Pair;
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

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * 镶嵌条目携带的额外数据。
 *
 * <p>镶嵌会消耗掉材料物品本身，凡是「之后还要用得上」的信息都必须跟着镶孔留下来。
 * 目前有三类：</p>
 * <ul>
 *   <li>{@link Potion} 药水效果——镶嵌药水后仍能还原出对应药水；</li>
 *   <li>{@link Enchantments} 附魔条数——中子锭按条数决定镶合产物；</li>
 *   <li>{@link Nesting} 潜影嵌套计数（已镶潜影盒数 + 虚空物质块数）——按「3 潜影盒 + 1 虚空物质块」镶合出空间超压器。</li>
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
            return ops.getStringValue(typeTag).flatMap(type -> {
                MapCodec<InlayExtra> codec = codecFor(type);
                return codec == null
                        ? DataResult.error(() -> "Unknown inlay extra type: " + type)
                        : codec.decode(ops, input);
            });
        }

        @Override
        public <T> RecordBuilder<T> encode(InlayExtra input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
            prefix.add("type", ops.createString(input.type()));
            return codecFor(input.type()).encode(input, ops, prefix);
        }
    };

    /** 单个额外数据的 Codec 形态。 */
    Codec<InlayExtra> CODEC = MAP_CODEC.codec();

    /**
     * 列表 JSON 编解码，供 {@code InlayEntry} 直接使用。
     *
     * <p>逐条解析、<b>丢弃解析失败的条目</b>：额外数据的类型名一旦变动（改名 / 移除），老存档里残留的
     * 旧条目就不会让整件物品（乃至玩家数据、方块镶嵌数据）反序列化失败——存档照样进得去，只是那条
     * 额外数据没了。</p>
     */
    Codec<List<InlayExtra>> LIST_CODEC = new Codec<>() {
        @Override
        public <T> DataResult<Pair<List<InlayExtra>, T>> decode(DynamicOps<T> ops, T input) {
            return ops.getList(input).map(elements -> {
                List<InlayExtra> extras = new ArrayList<>();
                elements.accept(element -> CODEC.parse(ops, element).result().ifPresent(extras::add));
                return Pair.of(extras, ops.empty());
            });
        }

        @Override
        public <T> DataResult<T> encode(List<InlayExtra> input, DynamicOps<T> ops, T prefix) {
            return CODEC.listOf().encode(input, ops, prefix);
        }
    };

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

    @Nullable
    @SuppressWarnings("unchecked")
    private static MapCodec<InlayExtra> codecFor(String type) {
        return switch (type) {
            case Potion.TYPE -> (MapCodec<InlayExtra>) (MapCodec<?>) Potion.CODEC;
            case Enchantments.TYPE -> (MapCodec<InlayExtra>) (MapCodec<?>) Enchantments.CODEC;
            case Nesting.TYPE -> (MapCodec<InlayExtra>) (MapCodec<?>) Nesting.CODEC;
            default -> null;
        };
    }

    private static InlayExtra read(ByteBuf buffer) {
        String type = ByteBufCodecs.STRING_UTF8.decode(buffer);
        return switch (type) {
            case Potion.TYPE -> Potion.read(buffer);
            case Enchantments.TYPE -> Enchantments.read(buffer);
            case Nesting.TYPE -> Nesting.read(buffer);
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

    /** 潜影嵌套计数：已镶进本物品的潜影盒数与虚空物质块数（不区分先后）。 */
    record Nesting(int shulkers, int voidMatter) implements InlayExtra {

        public static final String TYPE = "nesting";

        /** 全零（未嵌套）。 */
        public static final Nesting EMPTY = new Nesting(0, 0);

        public static final MapCodec<Nesting> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.INT.fieldOf("shulkers").forGetter(Nesting::shulkers),
                Codec.INT.fieldOf("void_matter").forGetter(Nesting::voidMatter)
        ).apply(instance, Nesting::new));

        @Override
        public String type() {
            return TYPE;
        }

        @Override
        public void write(ByteBuf buffer) {
            ByteBufCodecs.VAR_INT.encode(buffer, shulkers);
            ByteBufCodecs.VAR_INT.encode(buffer, voidMatter);
        }

        private static Nesting read(ByteBuf buffer) {
            return new Nesting(
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer));
        }
    }
}
