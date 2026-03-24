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
    public static final String TITLE_MENU = "&#434343<bold>»</bold> &#fbd72bCentral Logística del Clan";

    // 🎨 PALETA HEX - ÍTEMS DE LA TERMINAL
    private static final String ITEM_MONOLITH_NAME = "&#fbd72b<bold>🏛️ Monolito de %clan%</bold>";
    private static final String ITEM_BANK_NAME = "&#a8ff78<bold>💰 Reserva Logística</bold>";
    private static final String ITEM_FF_NAME = "&#ff4b2b<bold>⚔️ Protocolo de Fuego</bold>";
    private static final String ITEM_MEMBERS_NAME = "&#00fbff<bold>👥 Base de Operarios</bold>";

    public static void abrirMenu(Player player, NexoClan clan, NexoUser user) {
        // Creamos la terminal usando el Componente nativo de Paper
        Inventory inv = Bukkit.createInventory(null, 36, NexoColor.parse(TITLE_MENU));

        // ⬛ Cristal de fondo (Gris Carbón para reducir fatiga visual)
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
            monoMeta.displayName(NexoColor.parse(ITEM_MONOLITH_NAME.replace("%clan%", clan.getName())));
            List<net.kyori.adventure.text.Component> monoLore = new ArrayList<>();
            monoLore.add(NexoColor.parse("&#434343Tag Operativo: &#fbd72b[" + clan.getTag() + "]"));
            monoLore.add(NexoColor.parse("&#434343Nivel de Núcleo: &#a8ff78" + clan.getMonolithLevel()));
            monoLore.add(NexoColor.parse("&#434343Energía (EXP): &#00fbff" + clan.getMonolithExp()));
            monoMeta.lore(monoLore);
            monolith.setItemMeta(monoMeta);
        }
        inv.setItem(13, monolith);

        // 💰 Banco (Izquierda - Slot 11)
        ItemStack bank = new ItemStack(Material.EMERALD);
        ItemMeta bankMeta = bank.getItemMeta();
        if (bankMeta != null) {
            bankMeta.displayName(NexoColor.parse(ITEM_BANK_NAME));
            List<net.kyori.adventure.text.Component> bankLore = new ArrayList<>();
            bankLore.add(NexoColor.parse("&#434343Balance Disponible: &#fbd72b🪙 " + clan.getBankBalance()));
            bankLore.add(NexoColor.parse(" "));
            bankLore.add(NexoColor.parse("&#a8ff78▶ Escribe &#fbd72b/clan deposit <cantidad>"));
            bankLore.add(NexoColor.parse("&#ff4b2b▶ Escribe &#fbd72b/clan withdraw <cantidad>"));
            bankMeta.lore(bankLore);
            bank.setItemMeta(bankMeta);
        }
        inv.setItem(11, bank);

        // 🛡️ Fuego Amigo (Derecha - Slot 15)
        ItemStack ff = new ItemStack(clan.isFriendlyFire() ? Material.IRON_SWORD : Material.SHIELD);
        ItemMeta ffMeta = ff.getItemMeta();
        if (ffMeta != null) {
            ffMeta.displayName(NexoColor.parse(ITEM_FF_NAME));
            List<net.kyori.adventure.text.Component> ffLore = new ArrayList<>();
            ffLore.add(NexoColor.parse("&#434343Estado del Seguro: " + (clan.isFriendlyFire() ? "&#ff4b2b<bold>RIESGO ACTIVO</bold>" : "&#a8ff78<bold>PROTEGIDO</bold>")));
            ffLore.add(NexoColor.parse(" "));
            if (user.getClanRole().equals("LIDER") || user.getClanRole().equals("OFICIAL")) {
                ffLore.add(NexoColor.parse("&#fbd72b▶ Clic para reconfigurar sistema"));
            } else {
                ffLore.add(NexoColor.parse("&#ff4b2bAcceso Denegado."));
                ffLore.add(NexoColor.parse("&#434343Requiere autorización de Oficial."));
            }
            ffMeta.lore(ffLore);
            ff.setItemMeta(ffMeta);
        }
        inv.setItem(15, ff);

        // 👥 Miembros (Abajo - Slot 22)
        ItemStack members = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta memMeta = members.getItemMeta();
        if (memMeta != null) {
            memMeta.displayName(NexoColor.parse(ITEM_MEMBERS_NAME));
            List<net.kyori.adventure.text.Component> memLore = new ArrayList<>();
            memLore.add(NexoColor.parse("&#434343Rango Operativo: &#00fbff" + user.getClanRole()));
            memLore.add(NexoColor.parse(" "));
            memLore.add(NexoColor.parse("&#fbd72b▶ Clic para abrir Base de Datos"));
            memMeta.lore(memLore);
            members.setItemMeta(memMeta);
        }
        inv.setItem(22, members);

        // Abrimos la terminal
        player.openInventory(inv);
    }
}