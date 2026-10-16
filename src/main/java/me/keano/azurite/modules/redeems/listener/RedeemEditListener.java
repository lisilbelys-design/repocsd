package me.keano.azurite.modules.redeems.listener;

import me.keano.azurite.modules.framework.Module;
import me.keano.azurite.modules.redeems.EditorState;
import me.keano.azurite.modules.redeems.Redeem;
import me.keano.azurite.modules.redeems.RedeemManager;
import me.keano.azurite.modules.redeems.menu.RedeemEditorMenu;
import me.keano.azurite.modules.redeems.menu.RedeemIndividualMenu;
import me.keano.azurite.utils.CC;
import me.keano.azurite.utils.Tasks;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class RedeemEditListener extends Module<RedeemManager> {

    public RedeemEditListener(RedeemManager manager) {
        super(manager);
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();
        EditorState state = getManager().getEditorState(player.getUniqueId());

        if (state.getState() == EditorState.State.NONE) return;

        e.setCancelled(true);
        String message = e.getMessage();

        if (message.equalsIgnoreCase("cancel")) {
            state.reset();
            player.sendMessage(CC.t("&cCancelled."));
            reopenEditor(player, state);
            return;
        }

        switch (state.getState()) {
            case CREATING_REDEEM:
                handleCreateRedeem(player, state, message);
                break;

            case ENTERING_TEXTURE:
                handleTexture(player, state, message);
                break;

            case ENTERING_COMMAND:
                handleCommand(player, state, message);
                break;

            case ENTERING_LORE:
                handleLore(player, state, message);
                break;

            case ENTERING_ITEM_NAME:
                handleItemName(player, state, message);
                break;

            case ENTERING_MENU_ROWS:
                handleMenuRows(player, state, message);
                break;

            case EDITING_LORE_LINE:
                handleEditLoreLine(player, state, message);
                break;

            default:
                state.reset();
                break;
        }
    }

    private void handleCreateRedeem(Player player, EditorState state, String message) {
        String id = message.toLowerCase().replace(" ", "_");
        if (getManager().getRedeem(id) != null) {
            player.sendMessage(CC.t("&cA redeem with that name already exists!"));
            state.reset();
            Tasks.execute(getManager(), () -> new RedeemEditorMenu(getInstance().getMenuManager(), player, getManager()).open());
            return;
        }
        Redeem redeem = new Redeem(id);
        redeem.setName(message);
        getManager().getRedeems().put(id, redeem);
        getManager().saveRedeem(redeem);
        player.sendMessage(CC.t("&aCreated redeem: &e" + message));
        state.reset();
        Tasks.execute(getManager(), () -> new RedeemEditorMenu(getInstance().getMenuManager(), player, getManager()).open());
    }

    private void handleTexture(Player player, EditorState state, String message) {
        if (message.equalsIgnoreCase("Done")) {
            Redeem redeem = getManager().getRedeem(state.getEditingRedeemId());
            if (redeem == null) {
                state.reset();
                player.sendMessage(CC.t("&cRedeem no longer exists."));
                return;
            }
            String texture = state.getTextureBuffer().toString();
            redeem.setTexture(texture);
            redeem.setTextureEnabled(true);
            getManager().saveRedeem(redeem);
            player.sendMessage(CC.t("&aTexture set."));
            state.reset();
            Tasks.execute(getManager(), () -> new RedeemIndividualMenu(getInstance().getMenuManager(), player, getManager(), redeem).open());
        } else {
            state.getTextureBuffer().append(message);
            player.sendMessage(CC.t("&7This is all? Put Done or keep writing"));
        }
    }

    private void handleCommand(Player player, EditorState state, String message) {
        Redeem redeem = getManager().getRedeem(state.getEditingRedeemId());
        if (redeem == null) {
            state.reset();
            player.sendMessage(CC.t("&cRedeem no longer exists."));
            return;
        }
        redeem.getCommands().add(message);
        getManager().saveRedeem(redeem);
        player.sendMessage(CC.t("&aCommand added: &f" + message));
        state.reset();
        Tasks.execute(getManager(), () -> new RedeemIndividualMenu(getInstance().getMenuManager(), player, getManager(), redeem).open());
    }

    private void handleLore(Player player, EditorState state, String message) {
        Redeem redeem = getManager().getRedeem(state.getEditingRedeemId());
        if (redeem == null) {
            state.reset();
            player.sendMessage(CC.t("&cRedeem no longer exists."));
            return;
        }
        redeem.getLore().add(message);
        getManager().saveRedeem(redeem);
        player.sendMessage(CC.t("&aLore line added: &f" + CC.t(message)));
        state.reset();
        Tasks.execute(getManager(), () -> new RedeemIndividualMenu(getInstance().getMenuManager(), player, getManager(), redeem).open());
    }

    private void handleEditLoreLine(Player player, EditorState state, String message) {
        Redeem redeem = getManager().getRedeem(state.getEditingRedeemId());
        if (redeem == null) {
            state.reset();
            player.sendMessage(CC.t("&cRedeem no longer exists."));
            return;
        }
        int index = state.getSelectedLoreIndex();
        if (index >= 0 && index < redeem.getLore().size()) {
            redeem.getLore().set(index, message);
            getManager().saveRedeem(redeem);
            player.sendMessage(CC.t("&aLore line updated."));
        }
        state.reset();
        Tasks.execute(getManager(), () -> new RedeemIndividualMenu(getInstance().getMenuManager(), player, getManager(), redeem).open());
    }

    private void handleItemName(Player player, EditorState state, String message) {
        Redeem redeem = getManager().getRedeem(state.getEditingRedeemId());
        if (redeem == null) {
            state.reset();
            player.sendMessage(CC.t("&cRedeem no longer exists."));
            return;
        }
        redeem.setName(message);
        getManager().saveRedeem(redeem);
        player.sendMessage(CC.t("&aName set to: &f" + CC.t(message)));
        state.reset();
        Tasks.execute(getManager(), () -> new RedeemIndividualMenu(getInstance().getMenuManager(), player, getManager(), redeem).open());
    }

    private void handleMenuRows(Player player, EditorState state, String message) {
        try {
            int rows = Integer.parseInt(message);
            if (rows < 1 || rows > 6) {
                player.sendMessage(CC.t("&cValue must be between 1 and 6."));
                state.reset();
                return;
            }
            getManager().setMenuRows(rows);
            getManager().saveMenuRows();
            player.sendMessage(CC.t("&aMenu rows set to: &e" + rows));
        } catch (NumberFormatException ex) {
            player.sendMessage(CC.t("&cInvalid number. Value must be between 1 and 6."));
        }
        state.reset();
        Tasks.execute(getManager(), () -> new RedeemEditorMenu(getInstance().getMenuManager(), player, getManager()).open());
    }

    private void reopenEditor(Player player, EditorState state) {
        if (state.getEditingRedeemId() != null) {
            Redeem redeem = getManager().getRedeem(state.getEditingRedeemId());
            if (redeem != null) {
                Tasks.execute(getManager(), () -> new RedeemIndividualMenu(getInstance().getMenuManager(), player, getManager(), redeem).open());
                return;
            }
        }
        Tasks.execute(getManager(), () -> new RedeemEditorMenu(getInstance().getMenuManager(), player, getManager()).open());
    }
}
