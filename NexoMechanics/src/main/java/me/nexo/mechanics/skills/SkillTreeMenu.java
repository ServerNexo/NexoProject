package me.nexo.mechanics.skills;

import me.nexo.core.user.NexoAPI;
import me.nexo.core.user.NexoUser;
import me.nexo.core.utils.NexoColor;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SkillTreeMenu {

    private static final Map<UUID, PermissionAttachment> perms = new HashMap<>();

    public static void desbloquearNodo(Player p, String permiso, int costo) {
        NexoUser user = NexoAPI.getInstance().getUserLocal(p.getUniqueId());

        if (user == null) {
            p.sendMessage(NexoColor.parse("&#8b0000[!] Sincronización neuronal incompleta. Aguarda un momento."));
            return;
        }

        if (user.getKnowledgePoints() >= costo) {
            user.removeKnowledgePoints(costo);

            PermissionAttachment attachment = perms.computeIfAbsent(p.getUniqueId(),
                    k -> p.addAttachment(me.nexo.core.NexoCore.getPlugin(me.nexo.core.NexoCore.class)));

            attachment.setPermission(permiso, true);

            p.sendMessage(NexoColor.parse("&#00f5ff[✓] <bold>NODO DESBLOQUEADO:</bold> &#1c0f2aNueva tecnología industrial integrada a tu sistema."));
            p.sendMessage(NexoColor.parse("&#1c0f2aPuntos de Conocimiento restantes: &#00f5ff" + user.getKnowledgePoints()));

        } else {
            p.sendMessage(NexoColor.parse("&#8b0000[!] Conocimiento Insuficiente. &#1c0f2aRequieres más experiencia práctica para procesar este nodo."));
        }
    }

    public static void limpiarCachePermisos(UUID uuid) {
        perms.remove(uuid);
    }
}