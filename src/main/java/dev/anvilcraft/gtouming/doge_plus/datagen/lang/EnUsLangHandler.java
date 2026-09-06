package dev.anvilcraft.gtouming.doge_plus.datagen.lang;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumLangProvider;

/**
 * 英文（en_us）语言键。经 {@code REGISTRUM.addDataGenerator(ProviderType.LANG, ...)} 注册，
 * 由 {@link RegistrumLangProvider} 输出 {@code assets/anvilcraft_doge_plus/lang/en_us.json}
 * （并自动派生 en_ud）。
 *
 * <p>方块 / 物品 / 实体的显示名已由 Registrum 注册链自动生成
 * （如 {@code block.anvilcraft_doge_plus.chute_dispenser -> Chute Dispenser}），这里只补充
 * 非条目键（tooltip / container / gui / message / itemGroup / key）。</p>
 */
public class EnUsLangHandler {

    private EnUsLangHandler() {
    }

    public static void init(RegistrumLangProvider provider) {
        // ===== Containers =====
        provider.add("container.anvilcraft_doge_plus.chute_dispenser", "Chute Dispenser");
        provider.add("container.anvilcraft_doge_plus.chute_dropper", "Chute Dropper");
        provider.add("container.anvilcraft_doge_plus.magnetic_chute_dispenser", "Magnetic Chute Dispenser");
        provider.add("container.anvilcraft_doge_plus.magnetic_chute_dropper", "Magnetic Chute Dropper");

        // ===== GUI =====
        provider.add("gui.anvilcraft_doge_plus.jei.inlay", "Inlay");

        // ===== Tooltips =====
        provider.add("tooltip.anvilcraft_doge_plus.inlay_property.fire_proof", "Fire-proof: cannot be burned");
        provider.add("tooltip.anvilcraft_doge_plus.inlay_property.magnetic", "Magnetic: attracted by magnets");
        provider.add("tooltip.anvilcraft_doge_plus.inlay_property.high_temp", "High Temp: the longer it stays in lava or fire, the more accumulated damage; attacking consumes the accumulated damage");
        provider.add("tooltip.anvilcraft_doge_plus.inlay_property.high_temp_amount", "High Temp: current accumulated damage %s");
        provider.add("tooltip.anvilcraft_doge_plus.inlay_property.cold_forged", "Cold Forged: slowly repairs durability in water or powder snow");
        provider.add("tooltip.anvilcraft_doge_plus.inlay_property.eternal", "Eternal: indestructible, immune to fire, explosion, cactus, time and the void");
        provider.add("tooltip.anvilcraft_doge_plus.inlay_property.nirvana", "Nirvana: triggers a totem on death, then the inlay material shatters");
        provider.add("tooltip.anvilcraft_doge_plus.inlay_property.defense", "Defense: grants +2 armor when held or equipped");
        provider.add("tooltip.anvilcraft_doge_plus.inlay_property.life", "Life: grants +2 max health when held or equipped");
        provider.add("tooltip.anvilcraft_doge_plus.inlay_property.attack", "Attack: grants +2 attack damage when held or equipped");
        provider.add("tooltip.anvilcraft_doge_plus.inlay_property.enchant", "Enchant: merges enchantments when inlaid, extracts them when removed");
        provider.add("tooltip.anvilcraft_doge_plus.inlay_property.effect", "Effect: grants potion effects when held or equipped");
        provider.add("tooltip.anvilcraft_doge_plus.inlay_property.direction", "Direction: makes sockets directional, active when there are 6 sockets");
        provider.add("tooltip.anvilcraft_doge_plus.inlay_property.output", "Output: outputs redstone signals from this face");
        provider.add("tooltip.anvilcraft_doge_plus.inlay_property.input", "Input: inputs redstone signals from this face");
        provider.add("tooltip.anvilcraft_doge_plus.inlay_property.not_gate", "NOT Gate: outputs the inverted signal of the opposite face from this face");
        provider.add("tooltip.anvilcraft_doge_plus.inlay_property.and_gate", "AND Gate: outputs the AND of adjacent inputs from this face (in order)");
        provider.add("tooltip.anvilcraft_doge_plus.inlay_property.or_gate", "OR Gate: outputs the OR of adjacent inputs from this face (in order)");
        provider.add("tooltip.anvilcraft_doge_plus.inlay_property.generator", "Generator: produces 512 kW of power once placed");
        provider.add("tooltip.anvilcraft_doge_plus.inlay_property.resonance", "Resonance: enhances some properties of materials in other sockets");
        provider.add("tooltip.anvilcraft_doge_plus.inlay_property.resonance.cold_forged", "✦ Cold Forged: repairs durability faster in water or powder snow (durable items only)");
        provider.add("tooltip.anvilcraft_doge_plus.inlay_property.resonance.high_temp", "✦ High Temp: the longer it stays in lava or fire, the more damage accumulated; attacking slowly consumes the accumulated damage");
        provider.add("tooltip.anvilcraft_doge_plus.inlay_property.resonance.nirvana", "✦ Nirvana: triggers a totem on death, then the material has a 50% chance to shatter");
        provider.add("tooltip.anvilcraft_doge_plus.inlay_property.resonance.defense", "✦ Defense: grants +4 armor when held or equipped");
        provider.add("tooltip.anvilcraft_doge_plus.inlay_property.resonance.life", "✦ Life: grants +4 max health when held or equipped");
        provider.add("tooltip.anvilcraft_doge_plus.inlay_property.resonance.attack", "✦ Attack: grants +4 attack damage when held or equipped");
        provider.add("tooltip.anvilcraft_doge_plus.inlay_property.resonance.enchant", "✦ Enchant: merges enchantments with a 50% chance to boost the level by 1, and a 50% chance to extract them when removed");
        provider.add("tooltip.anvilcraft_doge_plus.inlay_details", "Hold [Shift] for inlay info");
        provider.add("tooltip.anvilcraft_doge_plus.material_attributes", "Inlay attributes:");

        // ===== Messages =====
        provider.add("message.anvilcraft_doge_plus.doge_anvil.growth", "Growth %1$s/%2$s");
        provider.add("message.anvilcraft_doge_plus.doge_anvil.no_space", "Not enough space to grow into a Giant Doge Anvil");

        // ===== Key mappings =====
        provider.add("key.categories.anvilcraft_doge_plus", "AnvilCraft: Doge+");
        provider.add("key.anvilcraft_doge_plus.open_silencer", "Open Silencer");
    }
}
