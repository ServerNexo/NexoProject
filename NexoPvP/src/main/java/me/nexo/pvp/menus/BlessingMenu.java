package me.nexo.pvp.menus;

import me.nexo.core.NexoCore;
import me.nexo.core.crossplay.CrossplayUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class BlessingMenu implements InventoryHolder {

    private final NexoCore core;
    private final Player player;
    private Inventory inventory;

    public BlessingMenu(NexoCore core, Player player) {
        this.core = core;
        this.player = player;
    }

    public void openMenu() {
        // Título en Magenta Eléctrico
        net.kyori.adventure.text.Component title = CrossplayUtils.parseCrossplay(player, "&#ff00ff🏛 Templo del Vacío");
        this.inventory = Bukkit.createInventory(this, 27, title);

        // 🌟 BENDICIÓN ESTÁNDAR (Economía In-Game)
        ItemStack standardBless = new ItemStack(Material.GOLD_NUGGET);
        ItemMeta metaStd = standardBless.getItemMeta();
        metaStd.displayName(CrossplayUtils.parseCrossplay(player, "&#00f5ff<bold>Bendición Menor</bold>"));
        List<net.kyori.adventure.text.Component> loreStd = new ArrayList<>();
        loreStd.add(CrossplayUtils.parseCrossplay(player, "&#1c0f2aProtege tu experiencia y equipo de 1 muerte."));
        loreStd.add(CrossplayUtils.parseCrossplay(player, " "));
        loreStd.add(CrossplayUtils.parseCrossplay(player, "&#ff00ffPrecio: 50,000 Monedas"));
        metaStd.lore(loreStd);
        metaStd.getPersistentDataContainer().set(new NamespacedKey(core, "action"), PersistentDataType.STRING, "buy_bless_coins");
        standardBless.setItemMeta(metaStd);

        // 🌟 BENDICIÓN PREMIUM (Monetización)
        ItemStack premiumBless = new ItemStack(Material.NETHER_STAR);
        ItemMeta metaPrem = premiumBless.getItemMeta();
        metaPrem.displayName(CrossplayUtils.parseCrossplay(player, "&#ff00ff<bold>Bendición del Vacío Absoluto</bold>"));
        List<net.kyori.adventure.text.Component> lorePrem = new ArrayList<>();
        lorePrem.add(CrossplayUtils.parseCrossplay(player, "&#1c0f2aProtección total garantizada por los dioses."));
        lorePrem.add(CrossplayUtils.parseCrossplay(player, " "));
        lorePrem.add(CrossplayUtils.parseCrossplay(player, "&#00f5ffPrecio: 150 Gemas"));
        metaPrem.lore(lorePrem);
        metaPrem.getPersistentDataContainer().set(new NamespacedKey(core, "action"), PersistentDataType.STRING, "buy_bless_premium");
        premiumBless.setItemMeta(metaPrem);

        // 🟪 FONDO VIVID VOID (Púrpura Profundo)
        ItemStack background = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta bgMeta = background.getItemMeta();
        bgMeta.displayName(net.kyori.adventure.text.Component.empty());
        background.setItemMeta(bgMeta);

        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, background);
        }

        inventory.setItem(11, standardBless);
        inventory.setItem(15, premiumBless);

        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() { return inventory; }
}