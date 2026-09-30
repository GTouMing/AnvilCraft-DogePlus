package dev.anvilcraft.gtouming.doge_plus.data;

import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.InlayProperty;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 方块级镶嵌数据：材料列表 + 各面方向性属性（门种 / 搬运角色）+ 各面设定值 + 各面物流量与过滤。
 *
 * @param faces 各面方向性镶嵌属性（{@link FaceMode}）：普通载体由材料推导，逻辑 / 物流载体由轮盘 / 门链手势编程
 * @param values 各面逻辑门的设定值（输入 / 输出门与三种有状态门）；缺省按面属性取
 *               {@link FaceMode#defaultValue()}（锁存门 0，其余 15）
 * @param throughputs 各面物流量（物流载体的「存入」面单次搬运物品数上限）；缺省即 {@link #DEFAULT_THROUGHPUT}
 * @param filters 各面过滤物品（物流载体的「存入」面只放行能通过它的物品）；缺省即无过滤
 * @param channels 各面远程门的信道标识（{@link FaceMode#REMOTE}，物品类型 + 数字）；缺省 {@link RemoteChannel#DEFAULT}
 */
public record BlockInlays(
        Block block,
        List<InlayEntry> inlays,
        Map<Direction, FaceMode> faces,
        Map<Direction, Integer> values,
        Map<Direction, Integer> throughputs,
        Map<Direction, ItemStack> filters,
        Map<Direction, RemoteChannel> channels) {

    /** 逻辑门设定值的默认值（也是上限）。 */
    public static final int DEFAULT_VALUE = 15;

    /** 物流载体「存入」面物流量的默认值（正好一整叠）。 */
    public static final int DEFAULT_THROUGHPUT = 64;

    /**
     * 字段数超过 {@code StreamCodec.composite} 的 6 元重载上限，因此手写：逐字段交给各自的子 codec。
     *
     * <p>过滤用 {@link ItemStack#OPTIONAL_STREAM_CODEC}（物品组件需要注册表），它要求注册表感知的
     * 缓冲，故整体走 {@link RegistryFriendlyByteBuf}；其余子 codec 只用到 {@link ByteBuf}，
     * 在 RFBB 上调用完全成立。</p>
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, BlockInlays> STREAM_CODEC =
            StreamCodec.of(BlockInlays::write, BlockInlays::read);

    private static final StreamCodec<ByteBuf, Block> BLOCK_CODEC =
            ResourceLocation.STREAM_CODEC.map(BuiltInRegistries.BLOCK::get, BuiltInRegistries.BLOCK::getKey);
    private static final StreamCodec<ByteBuf, List<InlayEntry>> INLAY_LIST_CODEC =
            ByteBufCodecs.collection(ArrayList::new, InlayEntry.STREAM_CODEC);
    private static final StreamCodec<ByteBuf, Map<Direction, FaceMode>> FACE_MAP_CODEC =
            ByteBufCodecs.map(HashMap::new, Direction.STREAM_CODEC, FaceMode.STREAM_CODEC);
    private static final StreamCodec<ByteBuf, Map<Direction, Integer>> INT_MAP_CODEC =
            ByteBufCodecs.map(HashMap::new, Direction.STREAM_CODEC, ByteBufCodecs.VAR_INT);
    private static final StreamCodec<RegistryFriendlyByteBuf, Map<Direction, ItemStack>> FILTER_MAP_CODEC =
            ByteBufCodecs.map(HashMap::new, Direction.STREAM_CODEC, ItemStack.OPTIONAL_STREAM_CODEC);
    private static final StreamCodec<RegistryFriendlyByteBuf, Map<Direction, RemoteChannel>> CHANNEL_MAP_CODEC =
            ByteBufCodecs.map(HashMap::new, Direction.STREAM_CODEC, RemoteChannel.STREAM_CODEC);

    private static void write(RegistryFriendlyByteBuf buffer, BlockInlays value) {
        BLOCK_CODEC.encode(buffer, value.block());
        INLAY_LIST_CODEC.encode(buffer, value.inlays());
        FACE_MAP_CODEC.encode(buffer, value.faces());
        INT_MAP_CODEC.encode(buffer, value.values());
        INT_MAP_CODEC.encode(buffer, value.throughputs());
        FILTER_MAP_CODEC.encode(buffer, value.filters());
        CHANNEL_MAP_CODEC.encode(buffer, value.channels());
    }

    private static BlockInlays read(RegistryFriendlyByteBuf buffer) {
        return new BlockInlays(
                BLOCK_CODEC.decode(buffer),
                INLAY_LIST_CODEC.decode(buffer),
                FACE_MAP_CODEC.decode(buffer),
                INT_MAP_CODEC.decode(buffer),
                INT_MAP_CODEC.decode(buffer),
                FILTER_MAP_CODEC.decode(buffer),
                CHANNEL_MAP_CODEC.decode(buffer));
    }

    public static BlockInlays nulls() {
        return new BlockInlays(Blocks.AIR, List.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of());
    }

    /**
     * 从镶嵌材料列表生成方向-面属性映射。
     * 按 Direction 顺序遍历槽位，检测每个槽位的材料携带哪种方向性属性。
     *
     * <p>材料推导出的远程面一律使用默认信道（{@link RemoteChannel#DEFAULT}）——数据包给普通方块镶上
     * 远程门时无法设置信道，于是它们都落在同一条默认信道上。信道设置只对三种载体开放。</p>
     */
    public static BlockInlays fromInlays(Block block, List<InlayEntry> inlays) {
        List<Direction> directionOrder = List.of(Direction.values());

        Map<Direction, FaceMode> faces = new HashMap<>();

        for (Direction dir : directionOrder) {
            faces.put(dir, FaceMode.NONE);
        }

        // 遍历镶孔，填充对应方向的面属性
        for (int i = 0; i < Math.min(inlays.size(), directionOrder.size()); i++) {
            InlayEntry entry = inlays.get(i);
            Direction dir = directionOrder.get(i);

            // 空镶孔（取出过的槽位）不携带任何方向性属性
            if (entry.isEmpty()) {
                faces.put(dir, FaceMode.NONE);
                continue;
            }

            // 获取材料定义
            FaceMode mode = faceModeOf(entry);
            // 无论 mode 是否为 NONE，都更新到 Map 中
            faces.put(dir, mode);
        }

        return new BlockInlays(block, inlays, faces, Map.of(), Map.of(), Map.of(), Map.of());
    }

    /**
     * 检测材料携带的方向性属性。
     * 优先级：非门 &gt; 与门 &gt; 各状态门 &gt; 远程 &gt; 红石 &gt; 方向 &gt; 存入 &gt; 取出。
     *
     * <p>「这个镶孔属于哪一类面属性」的判定以此方法为唯一来源（镶合配方、门链手势等也复用它）；
     * 只关心「是不是红石门」的调用方再判 {@link FaceMode#isRedstone()}。</p>
     */
    public static FaceMode faceModeOf(InlayEntry entry) {
        if (entry.containsAttributes(InlayProperty.NOT_GATE)) {
            return FaceMode.NOT_GATE;
        }
        if (entry.containsAttributes(InlayProperty.AND_GATE)) {
            return FaceMode.AND_GATE;
        }
        if (entry.containsAttributes(InlayProperty.COUNTER_GATE)) {
            return FaceMode.COUNTER_GATE;
        }
        if (entry.containsAttributes(InlayProperty.LATCH_GATE)) {
            return FaceMode.LATCH_GATE;
        }
        if (entry.containsAttributes(InlayProperty.DELAY_GATE)) {
            return FaceMode.DELAY_GATE;
        }
        if (entry.containsAttributes(InlayProperty.DELAY_INPUT_GATE)) {
            return FaceMode.DELAY_INPUT_GATE;
        }
        if (entry.containsAttributes(InlayProperty.REMOTE)) {
            return FaceMode.REMOTE;
        }
        if (entry.containsAttributes(InlayProperty.OUTPUT)) {
            return FaceMode.OUTPUT;
        }
        if (entry.containsAttributes(InlayProperty.INPUT)) {
            return FaceMode.INPUT;
        }
        if (entry.containsAttributes(InlayProperty.INSERT)) {
            return FaceMode.INSERT;
        }
        if (entry.containsAttributes(InlayProperty.EXTRACT)) {
            return FaceMode.EXTRACT;
        }
        return FaceMode.NONE;
    }

    /**
     * 获取指定方向的面属性。
     */
    public FaceMode getFace(Direction direction) {
        return faces.getOrDefault(direction, FaceMode.NONE);
    }

    /**
     * 该记录是否没有任何内容：无镶嵌材料、无面编程、无设定值、无物流量与过滤。
     *
     * <p>判断「是否还需保留」时不能只看 {@link #inlays()}：逻辑载体与物流载体都没有镶孔，
     * 它们的面属性 / 物流量 / 过滤只存在于 {@link #faces} 等 map 里，
     * 只看镶嵌材料会把已编程的载体误判成空。同理漏掉任何一张 map 都会让对应记录被丢掉。</p>
     */
    public boolean isEmpty() {
        for (InlayEntry entry : inlays) {
            if (!entry.isEmpty()) return false;
        }
        if (!values.isEmpty()) return false;
        for (FaceMode mode : faces.values()) {
            if (mode != FaceMode.NONE) return false;
        }
        if (!throughputs.isEmpty()) return false;
        for (ItemStack filter : filters.values()) {
            if (!filter.isEmpty()) return false;
        }
        if (!channels.isEmpty()) return false;
        return true;
    }

    /** 指定面逻辑门的设定值；未设定时为该面面属性的缺省值（锁存门为 0，其余 15）。 */
    public int getValue(Direction direction) {
        return values.getOrDefault(direction, getFace(direction).defaultValue());
    }

    /** 返回把指定面设定值改为 {@code value} 的新实例；等于该面面属性的缺省值时移除该键。 */
    public BlockInlays withValue(Direction direction, int value) {
        Map<Direction, Integer> updated = new HashMap<>(values);
        // 缺省值按门类型算：锁存门记录 15 时必须留在表里，否则读取时会当成「没锁存过」的 0。
        if (value == getFace(direction).defaultValue()) {
            updated.remove(direction);
        } else {
            updated.put(direction, value);
        }
        return withValues(updated);
    }

    /** 返回把各面设定值整体替换为 {@code newValues}（缺省的面走默认值）的新实例。 */
    public BlockInlays withValues(Map<Direction, Integer> newValues) {
        return new BlockInlays(block, inlays, faces, Map.copyOf(newValues), throughputs, filters, channels);
    }

    /** 返回把各面信道标识整体替换为 {@code newChannels}（缺省的面走默认信道）的新实例。 */
    public BlockInlays withChannels(Map<Direction, RemoteChannel> newChannels) {
        return new BlockInlays(block, inlays, faces, values, throughputs, filters, Map.copyOf(newChannels));
    }

    /**
     * 返回把指定面属性改为 {@code mode} 的新实例；{@link FaceMode#NONE} 时移除该键（该面未编程）。
     *
     * <p>供「逻辑载体 / 物流载体」在游戏内编程面属性使用：这类方块没有镶孔，{@code faces} 的值
     * 不是由 {@link #fromInlays} 从材料推导，而是编程结果的权威来源。</p>
     */
    public BlockInlays withFace(Direction direction, FaceMode mode) {
        Map<Direction, FaceMode> updated = new HashMap<>(faces);
        if (mode == FaceMode.NONE) {
            updated.remove(direction);
        } else {
            updated.put(direction, mode);
        }
        // 换面会改变面语义，旧面的信道标识对新面没有意义，一并清掉。
        return new BlockInlays(block, inlays, Map.copyOf(updated), values, throughputs, filters,
                withChannelRemoved(channels, direction));
    }

    /** 删除某面信道的副本（{@code withFace} 换面 / 清面时使用）。 */
    private static Map<Direction, RemoteChannel> withChannelRemoved(
            Map<Direction, RemoteChannel> channels, Direction direction) {
        if (!channels.containsKey(direction)) return channels;
        Map<Direction, RemoteChannel> updated = new HashMap<>(channels);
        updated.remove(direction);
        return Map.copyOf(updated);
    }

    /**
     * 指定面（物流「存入」面）的物流量：单次搬运的物品数上限；未设定时为 {@link #DEFAULT_THROUGHPUT}。
     */
    public int getThroughput(Direction direction) {
        return throughputs.getOrDefault(direction, DEFAULT_THROUGHPUT);
    }

    /** 返回把指定面物流量改为 {@code value} 的新实例；等于默认值时移除该键。 */
    public BlockInlays withThroughput(Direction direction, int value) {
        Map<Direction, Integer> updated = new HashMap<>(throughputs);
        if (value == DEFAULT_THROUGHPUT) {
            updated.remove(direction);
        } else {
            updated.put(direction, value);
        }
        return new BlockInlays(block, inlays, faces, values, Map.copyOf(updated), filters, channels);
    }

    /** 指定面（物流「存入」面）的过滤物品；未设置时为空栈。 */
    public ItemStack getFilter(Direction direction) {
        ItemStack filter = filters.get(direction);
        return filter == null ? ItemStack.EMPTY : filter.copyWithCount(1);
    }

    /** 返回把指定面过滤改为 {@code filter} 的新实例；空栈时移除该键（即清除过滤）。 */
    public BlockInlays withFilter(Direction direction, ItemStack filter) {
        Map<Direction, ItemStack> updated = new HashMap<>(filters);
        if (filter.isEmpty()) {
            updated.remove(direction);
        } else {
            updated.put(direction, filter.copyWithCount(1));
        }
        return new BlockInlays(block, inlays, faces, values, throughputs, Map.copyOf(updated), channels);
    }

    /** 指定面远程门的信道标识；未设定时为默认信道（{@link RemoteChannel#DEFAULT}）。 */
    public RemoteChannel getChannel(Direction direction) {
        return channels.getOrDefault(direction, RemoteChannel.DEFAULT);
    }

    /** 返回把指定面信道标识改为 {@code channel} 的新实例；等于默认信道时移除该键。 */
    public BlockInlays withChannel(Direction direction, RemoteChannel channel) {
        RemoteChannel normalized = channel.normalized();
        Map<Direction, RemoteChannel> updated = new HashMap<>(channels);
        if (normalized.isDefault()) {
            updated.remove(direction);
        } else {
            updated.put(direction, normalized);
        }
        return new BlockInlays(block, inlays, faces, values, throughputs, filters, Map.copyOf(updated));
    }
}
