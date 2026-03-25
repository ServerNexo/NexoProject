package me.nexo.clans.menu;

import me.nexo.clans.core.NexoClan;
import me.nexo.core.user.NexoUser;
import me.nexo.core.utils.NexoColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ClanMenu {

    // 🎨 CONSTANTE PÚBLICA PARA EL LISTENER
    public static final String TITLE_MENU = "&#434343<bold>»</bold> &#FFAA00Central Logística del Clan";

    // 🎨 PALETA HEX - ÍTEMS DE LA TERMINAL
    private static final String ITEM_BANK_NAME = "&#55FF55<bold>💰 Reserva Logística</bold>";
    private static final String ITEM_FF_NAME = "&#FF5555<bold>⚔️ Protocolo de Fuego</bold>";
    private static final String ITEM_MEMBERS_NAME = "&#00E5FF<bold>👥 Base de Operarios</bold>";

    public static void abrirMenu(Player player, NexoClan clan, NexoUser user) {
        Inventory inv = Bukkit.createInventory(null, 36, NexoColor.parse(TITLE_MENU));

        ItemStack panel = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta panelMeta = panel.getItemMeta();
        if (panelMeta != null) {
            panelMeta.displayName(NexoColor.parse(" "));
            panel.setItemMeta(panelMeta);
        }
        for (int i = 0; i < 36; i++) {
            inv.setItem(i, panel);
        }

        // 🏛️ Monolito (Centro - Slot 13)
        ItemStack monolith = new ItemStack(Material.BEACON);
        ItemMeta monoMeta = monolith.getItemMeta();
        if (monoMeta != null) {
            // 🌟 INYECCIÓN HEX: Ahora parsea el nombre del clan completo, incluyendo sus códigos HEX
            monoMeta.displayName(NexoColor.parse("&#FFAA00<bold>🏛️ Monolito de </bold>" + clan.getName()));
            List<net.kyori.adventure.text.Component> monoLore = new ArrayList<>();
            monoLore.add(NexoColor.parse("&#AAAAAATag Operativo: &#FFAA00[" + clan.getTag() + "]"));
            monoLore.add(NexoColor.parse("&#AAAAAANivel de Núcleo: &#55FF55" + clan.getMonolithLevel()));
            monoLore.add(NexoColor.parse("&#AAAAAAEnergía (EXP): &#00E5FF" + clan.getMonolithExp()));

            if (user.getClanRole().equals("LIDER")) {
                monoLore.add(NexoColor.parse(" "));
                monoLore.add(NexoColor.parse("&#FFAA00<bold>SISTEMAS DE GESTIÓN (LÍDER):</bold>"));
                monoLore.add(NexoColor.parse("&#AAAAAA/clan invite <user> &#555555- Reclutar"));
                monoLore.add(NexoColor.parse("&#AAAAAA/clan kick <user> &#555555- Expulsar"));
                monoLore.add(NexoColor.parse("&#AAAAAA/clan sethome &#555555- Reubicar Monolito"));
                monoLore.add(NexoColor.parse("&#FF5555/clan disband &#555555- Destruir Núcleo"));
            }
            monoMeta.lore(monoLore);
            monolith.setItemMeta(monoMeta);
        }

        // 💰 Banco Logístico (Slot 20)
        ItemStack bank = new ItemStack(Material.GOLD_INGOT);
        ItemMeta bankMeta = bank.getItemMeta();
        if (bankMeta != null) {
            bankMeta.displayName(NexoColor.parse(ITEM_BANK_NAME));
            List<net.kyori.adventure.text.Component> bankLore = new ArrayList<>();
            bankLore.add(NexoColor.parse("&#AAAAAAFondos Disponibles: &#FFAA00🪙 " + clan.getBankBalance()));
            bankLore.add(NexoColor.parse(" "));
            bankLore.add(NexoColor.parse("&#AAAAAA/clan deposit <monto>"));
            if (user.getClanRole().equals("LIDER") || user.getClanRole().equals("OFICIAL")) {
                bankLore.add(NexoColor.parse("&#AAAAAA/clan withdraw <monto>"));
            }
            bankMeta.lore(bankLore);
            bank.setItemMeta(bankMeta);
        }

        // ⚔️ Fuego Amigo (Slot 22)
        ItemStack sword = new ItemStack(clan.isFriendlyFire() ? Material.IRON_SWORD : Material.WOODEN_SWORD);
        ItemMeta swordMeta = sword.getItemMeta();
        if (swordMeta != null) {
            swordMeta.displayName(NexoColor.parse(ITEM_FF_NAME));
            List<net.kyori.adventure.text.Component> swordLore = new ArrayList<>();
            String status = clan.isFriendlyFire() ? "&#FF5555<bold>ACTIVADO</bold>" : "&#55FF55<bold>APAGADO</bold>";
            swordLore.add(NexoColor.parse("&#AAAAAAEstado Actual: " + status));
            if (user.getClanRole().equals("LIDER") || user.getClanRole().equals("OFICIAL")) {
                swordLore.add(NexoColor.parse(" "));
                swordLore.add(NexoColor.parse("&#FFAA00Click para alternar protocolo."));
            }
            swordMeta.lore(swordLore);
            sword.setItemMeta(swordMeta);
        }

        // 👥 Miembros (Slot 24)
        ItemStack heads = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta headsMeta = heads.getItemMeta();
        if (headsMeta != null) {
            headsMeta.displayName(NexoColor.parse(ITEM_MEMBERS_NAME));
            List<net.kyori.adventure.text.Component> headsLore = new ArrayList<>();
            headsLore.add(NexoColor.parse("&#AAAAAARango Personal: &#00E5FF" + user.getClanRole()));
            headsLore.add(NexoColor.parse(" "));
            headsLore.add(NexoColor.parse("&#FFAA00Click para inspeccionar base de datos."));
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