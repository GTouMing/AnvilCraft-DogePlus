package dev.anvilcraft.gtouming.doge_plus.datagen.lang;

import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

/**
 * 简体中文（zh_cn）语言文件生成器。
 * <p>Registrum 的 {@code RegistrumLangProvider} 只输出 en_us/en_ud，故 zh_cn 走 vanilla
 * {@link LanguageProvider}，输出 {@code assets/anvilcraft_doge_plus/lang/zh_cn.json}。</p>
 */
public class ZhCnLanguageProvider extends LanguageProvider {

    public ZhCnLanguageProvider(PackOutput packOutput) {
        super(packOutput, AnvilCraftDogePlus.MOD_ID, "zh_cn");
    }

    @Override
    protected void addTranslations() {
        // ===== 方块 =====
        add("block.anvilcraft_doge_plus.chute_dispenser", "溜槽发射器");
        add("block.anvilcraft_doge_plus.chute_dropper", "溜槽投掷器");
        add("block.anvilcraft_doge_plus.magnetic_chute_dispenser", "磁性溜槽发射器");
        add("block.anvilcraft_doge_plus.magnetic_chute_dropper", "磁性溜槽投掷器");
        add("block.anvilcraft_doge_plus.doge_anvil", "Doge 砧");
        add("block.anvilcraft_doge_plus.giant_doge_anvil", "巨型 Doge 砧");
        add("block.anvilcraft_doge_plus.inlay_table", "镶嵌台");
        add("block.anvilcraft_doge_plus.inlay_crafting_table", "镶合台");
        add("block.anvilcraft_doge_plus.doge_steel_block", "Doge 钢块");

        // ===== 容器标题 =====
        add("container.anvilcraft_doge_plus.chute_dispenser", "溜槽发射器");
        add("container.anvilcraft_doge_plus.chute_dropper", "溜槽投掷器");
        add("container.anvilcraft_doge_plus.magnetic_chute_dispenser", "磁性溜槽发射器");
        add("container.anvilcraft_doge_plus.magnetic_chute_dropper", "磁性溜槽投掷器");

        // ===== GUI =====
        add("gui.anvilcraft_doge_plus.jei.inlay", "镶嵌");
        add("gui.anvilcraft_doge_plus.jei.inlay_crafting", "镶合");

        // ===== Tooltips =====
        add("tooltip.anvilcraft_doge_plus.inlay_property.fire_proof", "耐火：不会被烧毁");
        add("tooltip.anvilcraft_doge_plus.inlay_property.magnetic", "磁性：具有引力，激活时具有斥力");
        add("tooltip.anvilcraft_doge_plus.inlay_property.high_temp", "高温：在熔岩或火中越久，累加伤害越高；攻击时消耗累加伤害");
        add("tooltip.anvilcraft_doge_plus.inlay_property.high_temp_amount", "高温：当前累加伤害 %s");
        add("tooltip.anvilcraft_doge_plus.inlay_property.cold_forged", "冷锻：在水中或细雪中缓慢回复耐久");
        add("tooltip.anvilcraft_doge_plus.inlay_property.eternal", "永恒：无法破坏，免疫火焰、爆炸、仙人掌、时间与虚空");
        add("tooltip.anvilcraft_doge_plus.inlay_property.nirvana", "涅槃：死亡时触发图腾，然后该镶嵌材料碎裂");
        add("tooltip.anvilcraft_doge_plus.inlay_property.defense", "防御：手持或装备时提升 2 点盔甲值");
        add("tooltip.anvilcraft_doge_plus.inlay_property.life", "生命：手持或装备时提升 2 点生命上限");
        add("tooltip.anvilcraft_doge_plus.inlay_property.attack", "攻击：手持或装备时提升 2 点攻击力");
        add("tooltip.anvilcraft_doge_plus.inlay_property.enchant", "附魔：镶嵌时合并附魔，移除时提取附魔");
        add("tooltip.anvilcraft_doge_plus.inlay_property.effect", "效果：手持、装备或放置时提供药水效果");
        add("tooltip.anvilcraft_doge_plus.inlay_property.direction", "方向：使镶孔具有方向性，镶孔数为 6 时生效");
        add("tooltip.anvilcraft_doge_plus.inlay_property.output", "输出：该面输出红石信号");
        add("tooltip.anvilcraft_doge_plus.inlay_property.input", "输入：该面输入红石信号");
        add("tooltip.anvilcraft_doge_plus.inlay_property.not_gate", "非门：该面输出反面输入的反信号");
        add("tooltip.anvilcraft_doge_plus.inlay_property.and_gate", "与门：该面输出邻面输入的与信号（按序查找）");
        add("tooltip.anvilcraft_doge_plus.inlay_property.or_gate", "或门：该面输出邻面输入的或信号（按序查找）");
        add("tooltip.anvilcraft_doge_plus.inlay_property.generator", "发电：放置后产生 512 kW 电力");
        add("tooltip.anvilcraft_doge_plus.inlay_property.resonance", "共鸣：增强其他镶孔的部分材料属性");
        add("tooltip.anvilcraft_doge_plus.inlay_property.resonance.cold_forged", "✦冷锻：在水中或细雪中较快回复耐久（仅耐久物品生效）");
        add("tooltip.anvilcraft_doge_plus.inlay_property.resonance.high_temp", "✦高温：在熔岩或火中越久，累加的伤害越高；攻击时缓慢消耗累加的伤害");
        add("tooltip.anvilcraft_doge_plus.inlay_property.resonance.nirvana", "✦涅槃：死亡时触发图腾，然后该材料 50% 的概率碎裂");
        add("tooltip.anvilcraft_doge_plus.inlay_property.resonance.defense", "✦防御：手持或装备时提升 4 点盔甲值");
        add("tooltip.anvilcraft_doge_plus.inlay_property.resonance.life", "✦生命：手持或装备时提升 4 点生命上限");
        add("tooltip.anvilcraft_doge_plus.inlay_property.resonance.attack", "✦攻击：手持或装备时提升 4 点攻击力");
        add("tooltip.anvilcraft_doge_plus.inlay_property.resonance.enchant", "✦附魔：镶嵌时合并附魔并有 50% 概率提升 1 级，移除时 50% 概率提取附魔");
        add("tooltip.anvilcraft_doge_plus.inlay_details", "按住 [Shift] 查看镶嵌信息");
        add("tooltip.anvilcraft_doge_plus.material_attributes", "镶嵌属性：");

        // ===== 消息 =====
        add("message.anvilcraft_doge_plus.doge_anvil.growth", "成长值 %1$s/%2$s");
        add("message.anvilcraft_doge_plus.doge_anvil.no_space", "空间不足，无法成长为巨型 Doge 砧");

        // ===== 物品栏标签页 =====
        add("itemGroup.anvilcraft_doge_plus.doge_plus", "铁砧工艺：Doge+");

        // ===== 物品 =====
        add("item.anvilcraft_doge_plus.doge_steel_ingot", "Doge 钢锭");
        add("item.anvilcraft_doge_plus.doge_magnet", "手持 Doge 磁铁");
        add("item.anvilcraft_doge_plus.mobile_silencer", "移动式消音器");

        // ===== 实体 =====
        add("entity.anvilcraft_doge_plus.flying_anvil", "飞行铁砧");
        add("entity.anvilcraft_doge_plus.doge_node", "Doge 节点");

        // ===== 按键 =====
        add("key.categories.anvilcraft_doge_plus", "铁砧工艺：Doge+");
        add("key.anvilcraft_doge_plus.open_silencer", "打开消音器");
    }
}
