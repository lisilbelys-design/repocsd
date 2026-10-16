package me.keano.azurite.modules.redeems.menu;

import me.keano.azurite.modules.framework.menu.Menu;
import me.keano.azurite.modules.framework.menu.MenuManager;
import me.keano.azurite.modules.framework.menu.button.Button;
import me.keano.azurite.modules.redeems.Redeem;
import me.keano.azurite.modules.redeems.RedeemManager;
import me.keano.azurite.utils.CC;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class RedeemMenuEditMode extends Menu {

    private final RedeemManager redeemManager;

    public RedeemMenuEditMode(MenuManager manager, Player player, RedeemManager redeemManager) {
        super(manager, player, CC.t("&c&lEdit Redeem Menu"), redeemManager.getMenuSize(), false);
        this.redeemManager = redeemManager;
        this.allowInteract = true;
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();

        for (Redeem redeem : redeemManager.getRedeemsList()) {
            if (redeem.getSlot() < 0 || redeem.getSlot() >= size) continue;

            ItemStack item = redeemManager.createRedeemItem(redeem);
            final int slot = redeem.getSlot();

            buttons.put(slot + 1, new Button() {
                @Override
                public void onClick(InventoryClickEvent e) {
                    // Redeem items are locked - cannot be moved by normal click
                    e.setCancelled(true);
                    player.sendMessage(CC.t("&cThis redeem item is locked. Edit it through the Individual Redeem Editor."));
                }

                @Override
                public ItemStack getItemStack() {
                    return item.clone();
                }
            });
        }

        return buttons;
    }

    @Override
    public void onClick(InventoryClickEvent e) {
        // Allow free interaction except on locked redeem slots
        Map<Integer, Button> btns = getButtons();
        if (e.getClickedInventory() != null && e.getClickedInventory() != player.getInventory()) {
            Button btn = btns.get(e.getSlot() + 1);
            if (btn != null) {
                e.setCancelled(true);
                btn.onClick(e);
            }
        }
    }
}
