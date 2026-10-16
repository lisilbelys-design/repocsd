package me.keano.azurite.modules.redeems;

import lombok.Getter;
import lombok.Setter;
import me.keano.azurite.HCF;
import me.keano.azurite.modules.framework.Manager;
import me.keano.azurite.modules.redeems.listener.RedeemEditListener;
import me.keano.azurite.utils.CC;
import me.keano.azurite.utils.configs.ConfigYML;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

@Getter
public class RedeemManager extends Manager {

    private final Map<String, Redeem> redeems;
    private final Map<UUID, List<String>> playerData;
    private final Map<UUID, EditorState> editorStates;

    private ConfigYML redeemsConfig;
    private ConfigYML redeemMenuConfig;
    private ConfigYML playerDataConfig;

    @Setter
    private int menuRows;

    public RedeemManager(HCF instance) {
        super(instance);
        this.redeems = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        this.playerData = new HashMap<>();
        this.editorStates = new HashMap<>();

        this.redeemsConfig = new ConfigYML(instance, "redeems");
        this.redeemMenuConfig = new ConfigYML(instance, "redeem_menu");
        this.playerDataConfig = new ConfigYML(instance, "player_data");

        this.menuRows = 4;
        this.load();
        new RedeemEditListener(this);
    }

    @Override
    public void reload() {
        redeems.clear();
        playerData.clear();
        this.redeemsConfig.reload();
        this.redeemMenuConfig.reload();
        this.playerDataConfig.reload();
        this.load();
    }

    private void load() {
        if (redeemMenuConfig.contains("rows")) {
            int rows = redeemMenuConfig.getInt("rows");
            if (rows >= 1 && rows <= 6) {
                this.menuRows = rows;
            }
        }

        if (redeemsConfig.contains("redeems")) {
            ConfigurationSection section = redeemsConfig.getConfigurationSection("redeems");
            if (section != null) {
                for (String key : section.getKeys(false)) {
                    String path = "redeems." + key + ".";
                    Redeem redeem = new Redeem(key);
                    redeem.setName(redeemsConfig.getString(path + "name"));
                    redeem.setTexture(redeemsConfig.getString(path + "texture"));
                    redeem.setTextureEnabled(redeemsConfig.getBoolean(path + "texture_enabled"));
                    redeem.setSlot(redeemsConfig.getInt(path + "slot"));
                    redeem.setCommands(redeemsConfig.getStringList(path + "commands"));
                    redeem.setLore(redeemsConfig.getStringList(path + "lore"));
                    redeem.setUses(redeemsConfig.getInt(path + "uses"));
                    redeems.put(key, redeem);
                }
            }
        }

        if (playerDataConfig.contains("players")) {
            ConfigurationSection players = playerDataConfig.getConfigurationSection("players");
            if (players != null) {
                for (String uuid : players.getKeys(false)) {
                    try {
                        UUID id = UUID.fromString(uuid);
                        List<String> redeemed = playerDataConfig.getStringList("players." + uuid + ".redeemed");
                        playerData.put(id, new ArrayList<>(redeemed));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
        }
    }

    public Redeem getRedeem(String id) {
        return redeems.get(id);
    }

    public List<Redeem> getRedeemsList() {
        return new ArrayList<>(redeems.values());
    }

    public void createRedeem(String id) {
        if (redeems.containsKey(id)) return;
        Redeem redeem = new Redeem(id);
        redeems.put(id, redeem);
        saveRedeem(redeem);
    }

    public void deleteRedeem(String id) {
        Redeem redeem = redeems.remove(id);
        if (redeem == null) return;
        redeemsConfig.set("redeems." + id, null);
        redeemsConfig.save();
        redeemsConfig.reloadCache();
    }

    public void saveRedeem(Redeem redeem) {
        String path = "redeems." + redeem.getId() + ".";
        redeemsConfig.set(path + "name", redeem.getName());
        redeemsConfig.set(path + "texture", redeem.getTexture());
        redeemsConfig.set(path + "texture_enabled", redeem.isTextureEnabled());
        redeemsConfig.set(path + "slot", redeem.getSlot());
        redeemsConfig.set(path + "commands", redeem.getCommands());
        redeemsConfig.set(path + "lore", redeem.getLore());
        redeemsConfig.set(path + "uses", redeem.getUses());
        redeemsConfig.save();
        redeemsConfig.reloadCache();
    }

    public void saveMenuRows() {
        redeemMenuConfig.set("rows", menuRows);
        redeemMenuConfig.save();
        redeemMenuConfig.reloadCache();
    }

    public boolean hasRedeemed(UUID uuid, String redeemId) {
        List<String> redeemed = playerData.get(uuid);
        return redeemed != null && redeemed.contains(redeemId);
    }

    public void markRedeemed(UUID uuid, String redeemId) {
        List<String> redeemed = playerData.computeIfAbsent(uuid, k -> new ArrayList<>());
        if (!redeemed.contains(redeemId)) {
            redeemed.add(redeemId);
        }
        playerDataConfig.set("players." + uuid.toString() + ".redeemed", redeemed);
        playerDataConfig.save();
        playerDataConfig.reloadCache();
    }

    public void resetPlayer(UUID uuid) {
        playerData.remove(uuid);
        playerDataConfig.set("players." + uuid.toString(), null);
        playerDataConfig.save();
        playerDataConfig.reloadCache();
    }

    public void resetPlayerRedeem(UUID uuid, String redeemId) {
        List<String> redeemed = playerData.get(uuid);
        if (redeemed == null) return;
        redeemed.remove(redeemId);
        playerDataConfig.set("players." + uuid.toString() + ".redeemed", redeemed);
        playerDataConfig.save();
        playerDataConfig.reloadCache();
    }

    public void executeRedeem(Player player, Redeem redeem) {
        redeem.setUses(redeem.getUses() + 1);
        saveRedeem(redeem);
        markRedeemed(player.getUniqueId(), redeem.getId());

        for (String cmd : redeem.getCommands()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    cmd.replace("%player%", player.getName()));
        }

        player.sendMessage(CC.t("&aYou have redeemed &e" + CC.t(redeem.getName()) + "&a!"));
    }

    public ItemStack createRedeemItem(Redeem redeem) {
        if (redeem.isTextureEnabled() && !redeem.getTexture().isEmpty()) {
            return RedeemSkullUtils.createSkull(redeem.getTexture());
        } else {
            return new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        }
    }

    public ItemStack createCreeperHead() {
        return new ItemStack(Material.SKULL_ITEM, 1, (short) 4);
    }

    public EditorState getEditorState(UUID uuid) {
        return editorStates.computeIfAbsent(uuid, EditorState::new);
    }

    public void clearEditorState(UUID uuid) {
        editorStates.remove(uuid);
    }

    public int getMenuSize() {
        return menuRows * 9;
    }
}
