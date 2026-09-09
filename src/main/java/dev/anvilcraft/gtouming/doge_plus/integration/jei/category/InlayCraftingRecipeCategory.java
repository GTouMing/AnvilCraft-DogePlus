package dev.anvilcraft.gtouming.doge_plus.integration.jei.category;

import dev.anvilcraft.gtouming.doge_plus.data.InlayEntry;
import dev.anvilcraft.gtouming.doge_plus.init.ModBlocks;
import dev.anvilcraft.gtouming.doge_plus.integration.jei.AnvilCraftDogePlusJeiPlugin;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay_crafting.InlayCraftingRecipe;
import dev.anvilcraft.gtouming.doge_plus.util.InlayUtil;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.integration.jei.drawable.DrawableBlockStateIcon;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.integration.jei.util.JeiSlotUtil;
import mezz.jei.api.gui.ITickTimer;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.item.armortrim.TrimMaterial;
import net.minecraft.world.item.armortrim.TrimMaterials;
import net.minecraft.world.item.armortrim.TrimPattern;
import net.minecraft.world.item.armortrim.TrimPatterns;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 镶合配方 JEI 界面：仿照 {@link InlayRecipeCategory} 的铁砧工艺布局，
 * 展示「已镶满的基材（单输入槽）→ 产物 + 空基材」。
 *
 * <p>输入槽为配方 base 施加全部 {@code inlays} 后的成品基材
 * （含 INLAY 组件；多物品/标签材料取笛卡尔组合循环展示）。
 * 输出两个槽：产物 与 返还的空基材。催化方块为镶嵌台与镶合台。</p>
 */
public class InlayCraftingRecipeCategory implements IRecipeCategory<RecipeHolder<InlayCraftingRecipe>> {

    private static final int WIDTH = 162;
    private static final int HEIGHT = 64;

    /** 原版「可纹饰装备」物品标签。 */
    private static final TagKey<Item> TRIM_ARMOR =
            TagKey.create(Registries.ITEM, ResourceLocation.withDefaultNamespace("trimmable_armor"));
    /** 原版「纹饰材料」物品标签。 */
    private static final TagKey<Item> TRIM_MATERIALS =
            TagKey.create(Registries.ITEM, ResourceLocation.withDefaultNamespace("trim_materials"));

    private final Component title;
    private final IDrawable icon;
    private final IDrawable slotDefault;
    private final IDrawable arrowIn;
    private final IDrawable arrowOutputFromBelow;
    private final ITickTimer timer;

    public InlayCraftingRecipeCategory(IGuiHelper helper) {
        this.title = Component.translatable("gui.anvilcraft_doge_plus.jei.inlay_crafting");
        this.icon = new DrawableBlockStateIcon(
                Blocks.ANVIL.defaultBlockState(),
                ModBlocks.INLAY_CRAFTING_TABLE.getDefaultState());
        this.slotDefault = JeiRenderHelper.getSlotDefault(helper);
        this.arrowIn = JeiRenderHelper.getArrowInput(helper);
        this.arrowOutputFromBelow = JeiRenderHelper.getArrowOutputFromBelow(helper);
        this.timer = helper.createTickTimer(30, 60, true);
    }

    @Override
    public RecipeType<RecipeHolder<InlayCraftingRecipe>> getRecipeType() {
        return AnvilCraftDogePlusJeiPlugin.INLAY_CRAFTING;
    }

    @Override
    public Component getTitle() {
        return this.title;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(
            IRecipeLayoutBuilder builder,
            RecipeHolder<InlayCraftingRecipe> recipeHolder,
            IFocusGroup focuses) {
        InlayCraftingRecipe recipe = recipeHolder.value();

        // 输入：已镶满的基材（base + 全部 inlays），单槽
        List<ItemStack> inlaidBases = filterFocused(buildInlaidBases(recipe), focuses);
        if (!inlaidBases.isEmpty()) {
            builder.addInputSlot(JeiSlotUtil.INPUT_X, JeiSlotUtil.DEFAULT_Y)
                    .addItemStacks(inlaidBases);
        }

        // 输出：产物（固定 result 或纹饰示例）+ 返还的空基材
        int[] resultPos = gridPos(0);
        int[] basePos = gridPos(1);
        builder.addOutputSlot(resultPos[0], resultPos[1])
                .addItemStack(primaryOutput(recipe));
        builder.addOutputSlot(basePos[0], basePos[1])
                .addItemStack(new ItemStack(recipe.getBaseItem()));
    }

    /** 配方主产物：固定 result，或（纹饰类）模板推导的示例装备。 */
    private static ItemStack primaryOutput(InlayCraftingRecipe recipe) {
        if (!recipe.derivesResult()) {
            return new ItemStack(recipe.getResultItem());
        }
        var level = Minecraft.getInstance().level;
        if (level == null) return new ItemStack(recipe.getBaseItem());
        var access = level.registryAccess();

        Optional<Holder.Reference<TrimPattern>> pattern =
                TrimPatterns.getFromTemplate(access, new ItemStack(recipe.getBaseItem()));
        Optional<Holder.Reference<TrimMaterial>> material =
                access.lookupOrThrow(Registries.TRIM_MATERIAL).get(TrimMaterials.REDSTONE);
        if (pattern.isPresent() && material.isPresent()) {
            ItemStack armor = new ItemStack(Items.IRON_CHESTPLATE);
            armor.set(DataComponents.TRIM, new ArmorTrim(material.get(), pattern.get()));
            return armor;
        }
        return new ItemStack(recipe.getBaseItem());
    }

    /**
     * 仿锻造页：输入槽逐帧步进（镶满基材组合），此处按“当前展示的基材”现算纹饰产物，
     * 用单物品 override 覆盖输出槽 —— 产物由展示中的装备拷贝而来，tooltip/耐久等与基底一致。
     */
    @Override
    public void onDisplayedIngredientsUpdate(
            RecipeHolder<InlayCraftingRecipe> recipeHolder,
            List<IRecipeSlotDrawable> recipeSlots,
            IFocusGroup focuses) {
        if (recipeSlots.size() < 3) return;
        InlayCraftingRecipe recipe = recipeHolder.value();
        // 固定产物无需逐帧重算
        if (!recipe.derivesResult()) return;
        // 聚焦输出时保留 JEI 默认行为
        if (focuses.getFocuses(RecipeIngredientRole.OUTPUT).findAny().isPresent()) return;

        ItemStack inlaid = recipeSlots.getFirst().getDisplayedItemStack().orElse(ItemStack.EMPTY);
        if (inlaid.isEmpty()) return;
        ItemStack trimmed = deriveTrimResult(inlaid);
        if (trimmed.isEmpty()) return;
        recipeSlots.get(1).createDisplayOverrides().addItemStack(trimmed);
    }

    /** 按当前展示的镶满模板推导纹饰产物（装备拷贝 + TRIM 组件）。 */
    private static ItemStack deriveTrimResult(ItemStack inlaidTemplate) {
        var level = Minecraft.getInstance().level;
        if (level == null) return ItemStack.EMPTY;

        ItemStack armor = ItemStack.EMPTY;
        ItemStack material = ItemStack.EMPTY;
        for (InlayEntry entry : InlayUtil.getInlays(inlaidTemplate)) {
            if (entry.isEmpty()) continue;
            ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(entry.id()));
            if (stack.isEmpty()) continue;
            if (stack.is(TRIM_ARMOR)) {
                armor = stack;
            } else if (stack.is(TRIM_MATERIALS)) {
                material = stack;
            }
        }
        if (armor.isEmpty() || material.isEmpty()) return ItemStack.EMPTY;

        var access = level.registryAccess();
        Optional<Holder.Reference<TrimPattern>> pattern = TrimPatterns.getFromTemplate(access, inlaidTemplate);
        Optional<Holder.Reference<TrimMaterial>> trimMaterial = TrimMaterials.getFromIngredient(access, material);
        if (pattern.isEmpty() || trimMaterial.isEmpty()) return ItemStack.EMPTY;

        ItemStack result = armor.copyWithCount(1);
        result.set(DataComponents.TRIM, new ArmorTrim(trimMaterial.get(), pattern.get()));
        return result;
    }

    /** 每个镶孔在 JEI 中最多展示的候选物品数（避免纹饰 tag 组合爆炸）。 */
    private static final int MAX_INLAY_CHOICES = 8;

    /**
     * 构造「已镶满」的基材（与 JEI 帧同步）：不展开笛卡尔积，而是按全局帧 i 从各镶孔
     * 候选列表同步取 {@code choices[slot][i % choices[slot].size()]}，相邻帧的镶孔组合
     * 交错变化（两镶孔各 2/3 候选时形如 11,22,13,21,12,23），避免步进时组合单调堆积。
     * 条目不含属性/额外数据（展示用）。
     */
    private static List<ItemStack> buildInlaidBases(InlayCraftingRecipe recipe) {
        List<List<ItemStack>> choices = new ArrayList<>();
        for (Ingredient ingredient : recipe.inlays()) {
            List<ItemStack> items = new ArrayList<>();
            for (ItemStack stack : ingredient.getItems()) {
                if (items.size() >= MAX_INLAY_CHOICES) break;
                if (stack.isEmpty() || items.stream().anyMatch(e -> e.is(stack.getItem()))) continue;
                items.add(stack);
            }
            if (items.isEmpty()) return List.of();
            choices.add(items);
        }

        int frames = 1;
        for (List<ItemStack> choice : choices) {
            frames *= choice.size();
        }

        List<ItemStack> results = new ArrayList<>(frames);
        for (int i = 0; i < frames; i++) {
            ItemStack base = new ItemStack(recipe.getBaseItem());
            for (List<ItemStack> choice : choices) {
                base = InlayUtil.withAddedInlay(base, entryOf(choice.get(i % choice.size())));
            }
            results.add(base);
        }
        return results;
    }

    /** 由物品生成无属性镶孔条目（镶合展示用，与「锻造材料无需属性」语义一致）。 */
    private static InlayEntry entryOf(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id.equals(BuiltInRegistries.ITEM.getDefaultKey()) || stack.getItem() == Items.AIR) {
            return InlayEntry.nulls();
        }
        return new InlayEntry(id, List.of(), List.of());
    }

    /** 与 {@link JeiSlotUtil#drawDefaultOutputSlots} 一致的网格排布，保证图标与槽框对齐。 */
    private static int[] gridPos(int index) {
        int cols = (int) Math.ceil(Math.sqrt(2));
        int rows = Math.ceilDiv(2, cols);
        int startX = JeiSlotUtil.OUTPUT_X - (cols - 1) * JeiSlotUtil.OFFSET / 2;
        int startY = JeiSlotUtil.DEFAULT_Y - (rows - 1) * JeiSlotUtil.OFFSET / 2;
        return new int[] {startX + (index % cols) * JeiSlotUtil.OFFSET, startY + (index / cols) * JeiSlotUtil.OFFSET};
    }

    /** JEI 聚焦过滤：与当前聚焦物品匹配时只保留命中项，否则显示全部。 */
    private static List<ItemStack> filterFocused(List<ItemStack> stacks, IFocusGroup focuses) {
        List<ItemStack> focused = focuses.getItemStackFocuses()
                .map(f -> f.getTypedValue().getIngredient())
                .toList();
        if (focused.isEmpty()) return stacks;
        List<ItemStack> filtered = stacks.stream()
                .filter(i -> focused.stream().anyMatch(p -> ItemStack.isSameItemSameComponents(p, i)))
                .toList();
        return filtered.isEmpty() ? stacks : filtered;
    }

    @Override
    public void draw(
            RecipeHolder<InlayCraftingRecipe> recipeHolder,
            IRecipeSlotsView slotsView,
            GuiGraphics guiGraphics,
            double mouseX,
            double mouseY) {
        float anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(timer);
        RenderSupport.renderBlock(
                guiGraphics,
                Blocks.ANVIL.defaultBlockState(),
                81,
                22 + anvilYOffset,
                20,
                12,
                RenderSupport.SINGLE_BLOCK);
        RenderSupport.renderBlock(
                guiGraphics,
                ModBlocks.INLAY_CRAFTING_TABLE.getDefaultState(),
                81,
                40,
                0,
                12,
                RenderSupport.SINGLE_BLOCK);

        this.arrowIn.draw(guiGraphics, 54, 30);
        this.arrowOutputFromBelow.draw(guiGraphics, 92, 29);
        JeiSlotUtil.drawDefaultInputSlots(guiGraphics, this.slotDefault, 1);
        JeiSlotUtil.drawDefaultOutputSlots(guiGraphics, this.slotDefault, 2);
    }
}
