package me.nexo.clans.menu;

import me.nexo.clans.NexoClans;
import me.nexo.clans.core.NexoClan;
import me.nexo.core.NexoCore;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.menus.NexoMenu;
import me.nexo.core.user.NexoUser;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.stream.Collectors;

public class ClanMenu extends NexoMenu {

    private final NexoClans plugin;
    private final NexoCore core;
    private final NexoClan clan;
    private final NexoUser user;

    public ClanMenu(Player player, NexoClans plugin, NexoClan clan, NexoUser user) {
        super(player);
        this.plugin = plugin;
        this.core = NexoCore.getPlugin(NexoCore.class);
        this.clan = clan;
        this.user = user;
    }

    @Override
    public String getMenuName() {
        return core.getConfigManager().getMessage("clans_messages.yml", "menus.principal.titulo");
    }

    @Override
    public int getSlots() {
        return 36;
    }

    @Override
    public void setMenuItems() {
        setFillerGlass();

        // 🌟 Monolito
        ItemStack monolith = new ItemStack(Material.BEACON);
        ItemMeta monoMeta = monolith.getItemMeta();
        if (monoMeta != null) {
            monoMeta.displayName(CrossplayUtils.parseCrossplay(player, core.getConfigManager().getMessage("clans_messages.yml", "menus.principal.item-monolito")));
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
        inventory.setItem(13, monolith);

        // 🌟 Banco
        ItemStack bank = new ItemStack(Material.GOLD_INGOT);
        ItemMeta bankMeta = bank.getItemMeta();
        if (bankMeta != null) {
            bankMeta.displayName(CrossplayUtils.parseCrossplay(player, core.getConfigManager().getMessage("clans_messages.yml", "menus.principal.item-banco")));
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
        inventory.setItem(20, bank);

        // 🌟 Fuego Amigo
        ItemStack sword = new ItemStack(clan.isFriendlyFire() ? Material.IRON_SWORD : Material.WOODEN_SWORD);
        ItemMeta swordMeta = sword.getItemMeta();
        if (swordMeta != null) {
            swordMeta.displayName(CrossplayUtils.parseCrossplay(player, core.getConfigManager().getMessage("clans_messages.yml", "menus.principal.item-ff")));
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
        inventory.setItem(22, sword);

        // 🌟 Miembros
        ItemStack heads = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta headsMeta = heads.getItemMeta();
        if (headsMeta != null) {
            headsMeta.displayName(CrossplayUtils.parseCrossplay(player, core.getConfigManager().getMessage("clans_messages.yml", "menus.principal.item-miembros")));
            List<String> loreConfig = core.getConfigManager().getConfig("clans_messages.yml").getStringList("menus.principal.lore-miembros");
            List<net.kyori.adventure.text.Component> headsLore = loreConfig.stream()
                    .map(line -> CrossplayUtils.parseCrossplay(player, line.replace("%role%", user.getClanRole())))
                    .collect(Collectors.toList());
            headsMeta.lore(headsLore);
            heads.setItemMeta(headsMeta);
        }
        inventory.setItem(24, heads);
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        event.setCancelled(true); // Bloqueo absoluto

        int slot = event.getRawSlot();

        // Clic en Fuego Amigo
        if (slot == 22) {
            if (user.getClanRole().equals("LIDER") || user.getClanRole().equals("OFICIAL")) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f);
                plugin.getClanManager().toggleFriendlyFireAsync(clan, player, !clan.isFriendlyFire());
                player.closeInventory();
            }
        }
        // Clic en Miembros
        else if (slot == 24) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
            new ClanMembersMenu(player, plugin, clan, user).open();
        }
    }
}