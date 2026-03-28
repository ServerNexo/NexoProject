package me.nexo.clans.menu;

import me.nexo.clans.core.NexoClan;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.user.NexoUser;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ClanMenu {

    public static final String TITLE_MENU = "&#1c0f2a<bold>»</bold> &#00f5ffCentral Logística del Clan";
    private static final String ITEM_BANK_NAME = "&#ff00ff<bold>💰 Reserva Logística</bold>";
    private static final String ITEM_FF_NAME = "&#ff00ff<bold>⚔️ Protocolo de Fuego</bold>";
    private static final String ITEM_MEMBERS_NAME = "&#ff00ff<bold>👥 Base de Operarios</bold>";

    public static void abrirMenu(Player player, NexoClan clan, NexoUser user) {
        Inventory inv = Bukkit.createInventory(null, 36, CrossplayUtils.parseCrossplay(player, TITLE_MENU));

        ItemStack panel = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta panelMeta = panel.getItemMeta();
        if (panelMeta != null) {
            panelMeta.displayName(CrossplayUtils.parseCrossplay(player, " "));
            panel.setItemMeta(panelMeta);
        }
        for (int i = 0; i < 36; i++) {
            inv.setItem(i, panel);
        }

        ItemStack monolith = new ItemStack(Material.BEACON);
        ItemMeta monoMeta = monolith.getItemMeta();
        if (monoMeta != null) {
            monoMeta.displayName(CrossplayUtils.parseCrossplay(player, "&#ff00ff<bold>🏛️ Monolito de </bold>" + clan.getName()));
            List<net.kyori.adventure.text.Component> monoLore = new ArrayList<>();
            monoLore.add(CrossplayUtils.parseCrossplay(player, "&#1c0f2aTag Operativo: &#ff00ff[" + clan.getTag() + "]"));
            monoLore.add(CrossplayUtils.parseCrossplay(player, "&#1c0f2aNivel de Núcleo: &#00f5ff" + clan.getMonolithLevel()));
            monoLore.add(CrossplayUtils.parseCrossplay(player, "&#1c0f2aEnergía (EXP): &#00f5ff" + clan.getMonolithExp()));
            if (user.getClanRole().equals("LIDER")) {
                monoLore.add(CrossplayUtils.parseCrossplay(player, " "));
                monoLore.add(CrossplayUtils.parseCrossplay(player, "&#ff00ff<bold>SISTEMAS DE GESTIÓN (LÍDER):</bold>"));
                monoLore.add(CrossplayUtils.parseCrossplay(player, "&#1c0f2a/clan invite <user> &#1c0f2a- Reclutar"));
                monoLore.add(CrossplayUtils.parseCrossplay(player, "&#1c0f2a/clan kick <user> &#1c0f2a- Expulsar"));
                monoLore.add(CrossplayUtils.parseCrossplay(player, "&#1c0f2a/clan sethome &#1c0f2a- Reubicar Monolito"));
                monoLore.add(CrossplayUtils.parseCrossplay(player, "&#8b0000/clan disband &#8b0000- Destruir Núcleo"));
            }
            monoMeta.lore(monoLore);
            monolith.setItemMeta(monoMeta);
        }

        ItemStack bank = new ItemStack(Material.GOLD_INGOT);
        ItemMeta bankMeta = bank.getItemMeta();
        if (bankMeta != null) {
            bankMeta.displayName(CrossplayUtils.parseCrossplay(player, ITEM_BANK_NAME));
            List<net.kyori.adventure.text.Component> bankLore = new ArrayList<>();
            bankLore.add(CrossplayUtils.parseCrossplay(player, "&#1c0f2aFondos Disponibles: &#ff00ff🪙 " + clan.getBankBalance()));
            bankLore.add(CrossplayUtils.parseCrossplay(player, " "));
            bankLore.add(CrossplayUtils.parseCrossplay(player, "&#1c0f2a/clan deposit <monto>"));
            if (user.getClanRole().equals("LIDER") || user.getClanRole().equals("OFICIAL")) {
                bankLore.add(CrossplayUtils.parseCrossplay(player, "&#1c0f2a/clan withdraw <monto>"));
            }
            bankMeta.lore(bankLore);
            bank.setItemMeta(bankMeta);
        }

        ItemStack sword = new ItemStack(clan.isFriendlyFire() ? Material.IRON_SWORD : Material.WOODEN_SWORD);
        ItemMeta swordMeta = sword.getItemMeta();
        if (swordMeta != null) {
            swordMeta.displayName(CrossplayUtils.parseCrossplay(player, ITEM_FF_NAME));
            List<net.kyori.adventure.text.Component> swordLore = new ArrayList<>();
            String status = clan.isFriendlyFire() ? "&#8b0000<bold>ACTIVADO</bold>" : "&#00f5ff<bold>APAGADO</bold>";
            swordLore.add(CrossplayUtils.parseCrossplay(player, "&#1c0f2aEstado Actual: " + status));
            if (user.getClanRole().equals("LIDER") || user.getClanRole().equals("OFICIAL")) {
                swordLore.add(CrossplayUtils.parseCrossplay(player, " "));
                swordLore.add(CrossplayUtils.parseCrossplay(player, "&#00f5ff► Clic para alternar protocolo"));
            }
            swordMeta.lore(swordLore);
            sword.setItemMeta(swordMeta);
        }

        ItemStack heads = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta headsMeta = heads.getItemMeta();
        if (headsMeta != null) {
            headsMeta.displayName(CrossplayUtils.parseCrossplay(player, ITEM_MEMBERS_NAME));
            List<net.kyori.adventure.text.Component> headsLore = new ArrayList<>();
            headsLore.add(CrossplayUtils.parseCrossplay(player, "&#1c0f2aRango Personal: &#00f5ff" + user.getClanRole()));
            headsLore.add(CrossplayUtils.parseCrossplay(player, " "));
            headsLore.add(CrossplayUtils.parseCrossplay(player, "&#00f5ff► Clic para inspeccionar base de datos"));
            headsMeta.lore(headsLore);
            heads.setItemMeta(headsMeta);
        }

        inv.setItem(13, monolith);
        inv.setItem(20, bank);
        inv.setItem(22, sword);
        inv.setItem(24, heads);

        player.openInventory(inv);
    }
}