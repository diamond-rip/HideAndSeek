package rip.diamond.hideandseek;

import lombok.RequiredArgsConstructor;
import me.goodestenglish.api.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

@RequiredArgsConstructor
public enum Items {

    TRANSFORM_TOOL(new ItemBuilder(Material.REDSTONE).name("<yellow>變身").lore("<gray>變身為真實方塊").build()),
    TELEPORT_TOOL(new ItemBuilder(Material.CLOCK).name("<yellow>固定").lore("<gray>把你固定在您當前的位置", "<gray>請注意: 其他玩家依然可以走過你的偽裝").build()),
    KNOCKBACK_STICK(new ItemBuilder(Material.STICK).name("<yellow>擊退棒").lore("<gray>擊退附近的玩家").enchantment(Enchantment.KNOCKBACK, 1).build()),
    DIRECTION_CHANGE(new ItemBuilder(Material.DIAMOND).name("<yellow>轉換方向").lore("<gray>轉換方塊的方向").build()),
    NETHERITE_SWORD(new ItemBuilder(Material.NETHERITE_SWORD).unbreakable().build()),
    ;

    private final ItemStack item;

    public ItemStack getItem() {
        return item.clone();
    }
}
