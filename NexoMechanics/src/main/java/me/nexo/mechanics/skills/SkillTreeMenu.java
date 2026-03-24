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

    // Almacenamiento temporal en caché de los permisos inyectados en la sesión actual
    private static final Map<UUID, PermissionAttachment> perms = new HashMap<>();

    /**
     * Intenta desbloquear un nodo tecnológico (permiso) cobrando Knowledge Points.
     */
    public static void desbloquearNodo(Player p, String permiso, int costo) {
        NexoUser user = NexoAPI.getInstance().getUserLocal(p.getUniqueId());

        if (user == null) {
            p.sendMessage(NexoColor.parse("<red>Tus datos no han cargado. Intenta de nuevo."));
            return;
        }

        // Validación de moneda (Puntos de conocimiento)
        // (Asegúrate de que tu clase NexoUser tenga getKnowledgePoints() y removeKnowledgePoints())
        if (user.getKnowledgePoints() >= costo) {
            user.removeKnowledgePoints(costo);

            // Inyectamos el permiso usando Bukkit API nativa
            PermissionAttachment attachment = perms.computeIfAbsent(p.getUniqueId(),
                    k -> p.addAttachment(me.nexo.core.NexoCore.getPlugin(me.nexo.core.NexoCore.class)));

            attachment.setPermission(permiso, true);

            p.sendMessage(NexoColor.parse("<green>¡Has desbloqueado una nueva tecnología industrial!"));
            p.sendMessage(NexoColor.parse("<gray>Puntos restantes: <aqua>" + user.getKnowledgePoints()));

            // 💾 Aquí delegas a un CompletableFuture para guardar el nuevo permiso en tu BD Supabase

        } else {
            p.sendMessage(NexoColor.parse("<red>Conocimiento insuficiente. Obtén más puntos farmeando/minando."));
        }
    }

    /**
     * Debe llamarse en el PlayerQuitEvent para evitar fugas de memoria
     */
    public static void limpiarCachePermisos(UUID uuid) {
        perms.remove(uuid);
    }
}