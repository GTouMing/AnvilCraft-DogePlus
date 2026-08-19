package dev.anvilcraft.gtouming.doge_plus.client.gui.screen;

import dev.anvilcraft.gtouming.doge_plus.client.gui.widget.MobileSilencerButton;
import dev.anvilcraft.gtouming.doge_plus.item.MobileSilencer;
import dev.anvilcraft.gtouming.doge_plus.network.SilencerUpdatePacket;
import it.unimi.dsi.fastutil.Pair;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MobileSilencerScreen extends Screen {
    private static final int IMAGE_WIDTH = 256;
    private static final int IMAGE_HEIGHT = 166;
    private static final int SCROLL_BAR_HEIGHT = 120;
    private static final int SCROLL_BAR_TOP = 35;
    private static final int START_LEFT_X = 6;
    private static final int START_RIGHT_X = 132;
    private static final int SCROLL_BAR_LEFT_X = 120;
    private static final int SCROLL_BAR_RIGHT_X = 245;
    private static final int SCROLL_BAR_WIDTH = 5;
    private static final int SCROLLER_HEIGHT = 9;
    public static final int SOUND_FILTERED = 0;
    public static final int SOUND_MUTED = 1;

    private final ItemStack stack;
    private int leftScrollOff;
    private int rightScrollOff;
    @Getter
    private String filterText = "";
    private boolean isDraggingLeft;
    private boolean isDraggingRight;
    private final List<Pair<ResourceLocation, Component>> allSounds = new ArrayList<>();
    private final List<Pair<ResourceLocation, Component>> filteredSounds = new ArrayList<>();
    private final List<Pair<ResourceLocation, Component>> mutedSounds = new ArrayList<>();

    private int leftPos;
    private int topPos;

    public MobileSilencerScreen(ItemStack stack) {
        super(Component.translatable("item.anvilcraft_doge_plus.mobile_silencer"));
        this.stack = stack;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - IMAGE_WIDTH) / 2;
        this.topPos = (this.height - IMAGE_HEIGHT) / 2;

        // 从 ItemStack 加载已静音列表
        loadMutedFromStack();

        int buttonTop = topPos + SCROLL_BAR_TOP;
        for (int i = 0; i < 8; i++) {
            int idx = i;
            this.addRenderableWidget(new MobileSilencerButton(leftPos + START_LEFT_X, buttonTop, i, SOUND_FILTERED,
                    b -> onAllSoundButtonClick(idx), this, "add")).setWidth(112);
            buttonTop += 15;
        }
        buttonTop = topPos + SCROLL_BAR_TOP;
        for (int i = 0; i < 8; i++) {
            int idx = i;
            this.addRenderableWidget(new MobileSilencerButton(leftPos + START_RIGHT_X, buttonTop, i, SOUND_MUTED,
                    b -> onMutedSoundButtonClick(idx), this, "remove"));
            buttonTop += 15;
        }

        this.addRenderableWidget(new EditBox(Objects.requireNonNull(this.minecraft).font,
                leftPos + 78, topPos + 19, 100, 12, Component.empty()))
                .setResponder(this::onSearchTextChange);

        SoundManager manager = Minecraft.getInstance().getSoundManager();
        BuiltInRegistries.SOUND_EVENT.stream()
                .map(it -> Pair.of(it.getLocation(), manager.getSoundEvent(it.getLocation())))
                .filter(it -> it.second() != null)
                .filter(it -> it.second().getSubtitle() != null)
                .forEach(it -> allSounds.add(Pair.of(it.first(), it.second().getSubtitle())));
        filteredSounds.addAll(allSounds);
    }

    private void loadMutedFromStack() {
        SoundManager manager = Minecraft.getInstance().getSoundManager();
        for (ResourceLocation sound : MobileSilencer.getMutedSounds(stack)) {
            WeighedSoundEvents events = manager.getSoundEvent(sound);
            if (events == null || events.getSubtitle() == null) continue;
            mutedSounds.add(Pair.of(sound, events.getSubtitle()));
        }
    }

    @Override
    public void onClose() {
        // 关闭时同步到服务端
        var sounds = new ArrayList<ResourceLocation>();
        for (var pair : mutedSounds) {
            sounds.add(pair.left());
        }
        MobileSilencer.setMutedSounds(stack, sounds);
        PacketDistributor.sendToServer(new SilencerUpdatePacket(sounds));
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ===== 搜索 =====

    private void onSearchTextChange(String text) {
        leftScrollOff = 0;
        filteredSounds.clear();
        this.filterText = text;
        if (text.isEmpty()) {
            filteredSounds.addAll(allSounds);
            filteredSounds.removeAll(mutedSounds);
            return;
        }
        if (text.startsWith("#")) {
            String search = text.substring(1);
            allSounds.stream()
                    .filter(it -> it.left().toString().contains(search))
                    .filter(it -> mutedSounds.stream().noneMatch(m -> m.left().equals(it.first())))
                    .forEach(filteredSounds::add);
        } else if (text.startsWith("~")) {
            try {
                java.util.regex.Pattern search = java.util.regex.Pattern.compile(text.substring(1));
                allSounds.stream()
                        .filter(it -> search.matcher(it.left().toString()).matches())
                        .filter(it -> mutedSounds.stream().noneMatch(m -> m.left().equals(it.first())))
                        .forEach(filteredSounds::add);
            } catch (Exception ignored) {
            }
        } else {
            allSounds.stream()
                    .filter(it -> it.right().getString().contains(text))
                    .filter(it -> mutedSounds.stream().noneMatch(m -> m.left().equals(it.first())))
                    .forEach(filteredSounds::add);
        }
    }

    private void refreshSoundList() {
        onSearchTextChange(filterText);
    }

    // ===== 按钮 =====

    private void onAllSoundButtonClick(int selectedIndex) {
        int idx = selectedIndex + leftScrollOff;
        if (idx >= filteredSounds.size()) return;
        ResourceLocation sound = filteredSounds.get(idx).left();
        addMutedSound(sound);
        refreshSoundList();
    }

    private void onMutedSoundButtonClick(int selectedIndex) {
        int idx = selectedIndex + rightScrollOff;
        if (idx >= mutedSounds.size()) return;
        ResourceLocation sound = mutedSounds.get(idx).left();
        removeMutedSound(sound);
        refreshSoundList();
    }

    void addMutedSound(ResourceLocation sound) {
        SoundManager manager = Minecraft.getInstance().getSoundManager();
        WeighedSoundEvents event = manager.getSoundEvent(sound);
        if (event == null) return;
        mutedSounds.add(Pair.of(sound, event.getSubtitle() == null ? Component.empty() : event.getSubtitle()));
    }

    void removeMutedSound(ResourceLocation sound) {
        mutedSounds.removeIf(it -> it.left().equals(sound));
    }

    // ===== 查询 =====

    public Component getSoundTextAt(int index, int variant) {
        int idx = index + (variant == SOUND_FILTERED ? leftScrollOff : rightScrollOff);
        var list = variant == SOUND_FILTERED ? filteredSounds : mutedSounds;
        if (idx >= list.size()) return Component.empty();
        return list.get(idx).right();
    }

    @Nullable
    public ResourceLocation getSoundIdAt(int index, int variant) {
        int idx = index + (variant == SOUND_FILTERED ? leftScrollOff : rightScrollOff);
        var list = variant == SOUND_FILTERED ? filteredSounds : mutedSounds;
        if (idx >= list.size()) return null;
        return list.get(idx).left();
    }

    // ===== 输入 =====

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            return this.getFocused() != null && this.getFocused().keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseInLeft(mouseX, mouseY)) {
            if (filteredSounds.size() > 8)
                leftScrollOff = (int) Mth.clamp(leftScrollOff - scrollY, 0, filteredSounds.size() - 7);
        } else if (mouseInRight(mouseX, mouseY)) {
            if (mutedSounds.size() > 8)
                rightScrollOff = (int) Mth.clamp(rightScrollOff - scrollY, 0, mutedSounds.size() - 7);
        }
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        isDraggingLeft = false;
        isDraggingRight = false;
        if (mouseInLeftSlider(mouseX, mouseY) && filteredSounds.size() > 8) isDraggingLeft = true;
        if (mouseInRightSlider(mouseX, mouseY) && mutedSounds.size() > 8) isDraggingRight = true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDraggingLeft) return dragSlider(mouseY, filteredSounds.size(), true);
        if (isDraggingRight) return dragSlider(mouseY, mutedSounds.size(), false);
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    private boolean dragSlider(double mouseY, int total, boolean isLeft) {
        int j = topPos + SCROLL_BAR_TOP;
        int k = j + SCROLL_BAR_HEIGHT;
        int dragMax = Math.max(total - 7, 0);
        float scroll = (float) ((mouseY - j - 13.5F) / ((k - j) - 27.0F));
        scroll = scroll * dragMax + 0.5F;
        if (isLeft) leftScrollOff = Mth.clamp((int) scroll, 0, dragMax);
        else rightScrollOff = Mth.clamp((int) scroll, 0, dragMax);
        return true;
    }

    // ===== 渲染 =====

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderScroller(guiGraphics, leftPos + SCROLL_BAR_LEFT_X, topPos + SCROLL_BAR_TOP, filteredSounds.size(), leftScrollOff);
        renderScroller(guiGraphics, leftPos + SCROLL_BAR_RIGHT_X, topPos + SCROLL_BAR_TOP, mutedSounds.size(), rightScrollOff);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.blit(dev.dubhe.anvilcraft.constant.SharedTextures.bg("machine", "active_silencer"), leftPos, topPos, 0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
        guiGraphics.drawString(this.font, this.title, leftPos + (IMAGE_WIDTH - this.font.width(this.title)) / 2, dev.dubhe.anvilcraft.constant.Constant.SCREEN_TITLE_Y, 4210752, false);
    }

    private void renderScroller(GuiGraphics guiGraphics, int posX, int posY, int totalCount, int scrollOff) {
        int items = totalCount + 1 - 8;
        if (items > 1) {
            int maxY = posY + SCROLL_BAR_HEIGHT - SCROLLER_HEIGHT;
            int scrollY = (int) (posY + (scrollOff / (float) totalCount) * SCROLL_BAR_HEIGHT);
            scrollY = Mth.clamp(scrollY, posY, maxY);
            guiGraphics.blit(dev.dubhe.anvilcraft.constant.SharedTextures.SMALL_SLIDER, posX, scrollY, 0, 0, 5, 9, 10, 9);
        } else {
            guiGraphics.blit(dev.dubhe.anvilcraft.constant.SharedTextures.SMALL_SLIDER, posX, posY, 0, 0, 5, 9, 10, 9);
        }
    }

    // ===== 区域检测 =====

    private boolean mouseInLeft(double mx, double my) {
        return mx >= leftPos + START_LEFT_X && mx <= leftPos + SCROLL_BAR_LEFT_X + SCROLL_BAR_WIDTH
                && my >= topPos + SCROLL_BAR_TOP && my <= topPos + SCROLL_BAR_TOP + SCROLL_BAR_HEIGHT;
    }

    private boolean mouseInRight(double mx, double my) {
        return mx >= leftPos + START_RIGHT_X && mx <= leftPos + SCROLL_BAR_RIGHT_X + SCROLL_BAR_WIDTH
                && my >= topPos + SCROLL_BAR_TOP && my <= topPos + SCROLL_BAR_TOP + SCROLL_BAR_HEIGHT;
    }

    private boolean mouseInLeftSlider(double mx, double my) {
        return mx >= leftPos + SCROLL_BAR_LEFT_X && mx <= leftPos + SCROLL_BAR_LEFT_X + SCROLL_BAR_WIDTH
                && my >= topPos + SCROLL_BAR_TOP && my <= topPos + SCROLL_BAR_TOP + SCROLL_BAR_HEIGHT;
    }

    private boolean mouseInRightSlider(double mx, double my) {
        return mx >= leftPos + SCROLL_BAR_RIGHT_X && mx <= leftPos + SCROLL_BAR_RIGHT_X + SCROLL_BAR_WIDTH
                && my >= topPos + SCROLL_BAR_TOP && my <= topPos + SCROLL_BAR_TOP + SCROLL_BAR_HEIGHT;
    }
}
