package me.nexo.clans.menu;

import me.nexo.clans.NexoClans;
import me.nexo.clans.core.NexoClan;
import me.nexo.core.NexoCore;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.user.NexoUser;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.List;
import java.util.stream.Collectors;

public class ClanMenu {

    public static void abrirMenu(Player player, NexoClan clan, NexoUser user, NexoClans plugin) {
        NexoCore core = NexoCore.getPlugin(NexoCore.class);
        Inventory inv = Bukkit.createInventory(null, 36, CrossplayUtils.parseCrossplay(player, core.getConfigManager().getMessage("clans_messages.yml", "menus.principal.titulo")));

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
            // 🌟 CORRECCIÓN 1: Llamamos a getConfig() en lugar de getMessages()
            List<String> loreConfig = core.getConfigManager().getConfig("clans_messages.yml").getStringList("menus.principal.lore-monolito");
            List<net.kyori.adventure.text.Component> monoLore = loreConfig.stream()
                    .map(line -> CrossplayUtils.parseCrossplay(player, line
                            .replace("%clan_name%", clan.getName())
                            .replace("%tag%", clan.getTag())
                            .replace("%level%", String.valueOf(clan.getMonolithLevel()))
                            .replace("%exp%", String.valueOf(clan.getMonolithExp()))))
                    .collect(Collectors.toList());

            if (!user.getClanRole().equals("LIDER")) {
                monoLore.removeIf(line -> PlainTextComponentSerializer.plainText().serialize(line).contains("/clan"));
            }
            monoMeta.lore(monoLore);
            monolith.setItemMeta(monoMeta);
        }

        ItemStack bank = new ItemStack(Material.GOLD_INGOT);
        ItemMeta bankMeta = bank.getItemMeta();
        if (bankMeta != null) {
            bankMeta.displayName(CrossplayUtils.parseCrossplay(player, core.getConfigManager().getMessage("clans_messages.yml", "menus.principal.item-banco")));
            // 🌟 CORRECCIÓN 2
            List<String> loreConfig = core.getConfigManager().getConfig("clans_messages.yml").getStringList("menus.principal.lore-banco");
            List<net.kyori.adventure.text.Component> bankLore = loreConfig.stream()
                    .map(line -> CrossplayUtils.parseCrossplay(player, line.replace("%balance%", clan.getBankBalance().toString())))
                    .collect(Collectors.toList());
            if (!user.getClanRole().equals("LIDER") && !user.getClanRole().equals("OFICIAL")) {
                bankLore.removeIf(line -> PlainTextComponentSerializer.plainText().serialize(line).contains("withdraw"));
            }
            bankMeta.lore(bankLore);
            bank.setItemMeta(bankMeta);
        }

        ItemStack sword = new ItemStack(clan.isFriendlyFire() ? Material.IRON_SWORD : Material.WOODEN_SWORD);
        ItemMeta swordMeta = sword.getItemMeta();
        if (swordMeta != null) {
            swordMeta.displayName(CrossplayUtils.parseCrossplay(player, core.getConfigManager().getMessage("clans_messages.yml", "menus.principal.item-ff")));
            // 🌟 CORRECCIÓN 3
            List<String> loreConfig = core.getConfigManager().getConfig("clans_messages.yml").getStringList("menus.principal.lore-ff");
            String status = clan.isFriendlyFire() ? "&#8b0000<bold>ACTIVADO</bold>" : "&#00f5ff<bold>APAGADO</bold>";
            List<net.kyori.adventure.text.Component> swordLore = loreConfig.stream()
                    .map(line -> CrossplayUtils.parseCrossplay(player, line.replace("%status%", status)))
                    .collect(Collectors.toList());
            if (!user.getClanRole().equals("LIDER") && !user.getClanRole().equals("OFICIAL")) {
                swordLore.removeIf(line -> PlainTextComponentSerializer.plainText().serialize(line).contains("Clic"));
            }
            swordMeta.lore(swordLore);
            sword.setItemMeta(swordMeta);
        }

        ItemStack heads = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta headsMeta = heads.getItemMeta();
        if (headsMeta != null) {
            headsMeta.displayName(CrossplayUtils.parseCrossplay(player, core.getConfigManager().getMessage("clans_messages.yml", "menus.principal.item-miembros")));
            // 🌟 CORRECCIÓN 4
            List<String> loreConfig = core.getConfigManager().getConfig("clans_messages.yml").getStringList("menus.principal.lore-miembros");
            List<net.kyori.adventure.text.Component> headsLore = loreConfig.stream()
                    .map(line -> CrossplayUtils.parseCrossplay(player, line.replace("%role%", user.getClanRole())))
                    .collect(Collectors.toList());
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