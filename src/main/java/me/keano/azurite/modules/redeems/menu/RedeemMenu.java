package me.keano.azurite.modules.redeems.menu;

import me.keano.azurite.modules.framework.menu.Menu;
import me.keano.azurite.modules.framework.menu.MenuManager;
import me.keano.azurite.modules.framework.menu.button.Button;
import me.keano.azurite.modules.redeems.Redeem;
import me.keano.azurite.modules.redeems.RedeemManager;
import me.keano.azurite.utils.CC;
import me.keano.azurite.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class RedeemMenu extends Menu {

    private final RedeemManager redeemManager;

    public RedeemMenu(MenuManager manager, Player player, RedeemManager redeemManager) {
        super(manager, player, CC.t("&c&lRedeems"), redeemManager.getMenuSize(), false);
        this.redeemManager = redeemManager;
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();

        for (Redeem redeem : redeemManager.getRedeemsList()) {
            if (redeem.getSlot() < 0 || redeem.getSlot() >= size) continue;

            boolean redeemed = redeemManager.hasRedeemed(player.getUniqueId(), redeem.getId());
            ItemStack item = buildRedeemItem(redeem, redeemed);

            buttons.put(redeem.getSlot() + 1, new Button() {
                @Override
                public void onClick(InventoryClickEvent e) {
                    e.setCancelled(true);
                    if (redeemed) {
                        player.sendMessage(CC.t("&cYou have already redeemed this!"));
                        return;
                    }
                    player.closeInventory();
                    redeemManager.executeRedeem(player, redeem);
                }

                @Override
                public ItemStack getItemStack() {
                    return item.clone();
                }
            });
        }

        return buttons;
    }

    @SuppressWarnings("deprecation")
    private ItemStack buildRedeemItem(Redeem redeem, boolean redeemed) {
        ItemStack base = redeemManager.createRedeemItem(redeem);
        ItemBuilder builder = new ItemBuilder(base);

        String displayName = CC.t(redeem.getName());
        if (redeemed) {
            displayName = CC.t("&a&l\u2713 &r") + displayName;
        }
        builder.setName(displayName);

        List<String> lore = new ArrayList<>();
        if (redeemed) {
            lore.add("&aAlready redeemed!");
        } else {
            lore.add("&7Click to redeem this reward!");
        }
        lore.add("");
        if (redeem.getLore() != null && !redeem.getLore().isEmpty()) {
            for (String line : redeem.getLore()) {
                lore.add(line.replace("%uses%", String.valueOf(redeem.getUses())));
            }
            lore.add("");
        }
        lore.add("&7Uses: &e" + redeem.getUses());

        builder.setLore(lore);
        return builder.toItemStack();
    }
}
