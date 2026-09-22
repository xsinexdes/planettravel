package com.planettravel.selection;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Seleccion de zonas con un HACHA, igual que la de WorldEdit: clic izquierdo en
 * un bloque = posicion 1, clic derecho = posicion 2.
 *
 * Es un hacha PROPIA de PlanetTravel (marcada con una etiqueta invisible), no
 * el hacha de madera normal, para no chocar con WorldEdit ni con quien use un
 * hacha de verdad para talar. Si prefieres seleccionar con la de WorldEdit,
 * usa "/planeta area <id> we" o "/espacio base crear ... we": leen su seleccion.
 */
public class SelectionManager {

    /** A que se usa el PROXIMO clic del hacha: marcar un punto concreto de un planeta o del espacio. */
    public enum TipoPunto { ENTRADA, SALIDA, SOL, DETECCION }

    /** Modo de "marcar un punto" activo para un jugador (distinto de la seleccion de area pos1/pos2). */
    public record ModoPunto(TipoPunto tipo, String idPlaneta) { }

    private final NamespacedKey claveHacha;
    private final Map<UUID, Location> posicion1 = new HashMap<>();
    private final Map<UUID, Location> posicion2 = new HashMap<>();
    private final Map<UUID, ModoPunto> modoPunto = new HashMap<>();

    public SelectionManager(JavaPlugin plugin) {
        this.claveHacha = new NamespacedKey(plugin, "selection_wand");
    }

    public ItemStack crearHacha() {
        ItemStack item = new ItemStack(Material.WOODEN_AXE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§b§lHacha de seleccion §7(PlanetTravel)");
            meta.setLore(Arrays.asList(
                    "§eClic izquierdo §7en un bloque: posicion 1",
                    "§eClic derecho §7en un bloque: posicion 2",
                    "",
                    "§8Sirve para marcar el area de un planeta",
                    "§8o una base espacial."));
            meta.getPersistentDataContainer().set(claveHacha, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    public boolean esHacha(ItemStack item) {
        if (item == null || item.getType() != Material.WOODEN_AXE || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(claveHacha, PersistentDataType.BYTE);
    }

    public void setPos1(Player jugador, Location loc) {
        posicion1.put(jugador.getUniqueId(), loc);
    }

    public void setPos2(Player jugador, Location loc) {
        posicion2.put(jugador.getUniqueId(), loc);
    }

    public Location getPos1(Player jugador) { return posicion1.get(jugador.getUniqueId()); }
    public Location getPos2(Player jugador) { return posicion2.get(jugador.getUniqueId()); }

    /** La seleccion completa del jugador, o null si le falta algun punto o estan en mundos distintos. */
    public Seleccion getSeleccion(Player jugador) {
        Location a = getPos1(jugador);
        Location b = getPos2(jugador);
        if (a == null || b == null || a.getWorld() == null || !a.getWorld().equals(b.getWorld())) {
            return null;
        }
        return Seleccion.de(a.getWorld(),
                a.getBlockX(), a.getBlockY(), a.getBlockZ(),
                b.getBlockX(), b.getBlockY(), b.getBlockZ());
    }

    /**
     * Seleccion "de donde sea": si el jugador pide 'we', la de WorldEdit; si no,
     * la del hacha propia. Lanza IllegalStateException con un mensaje listo para mostrar.
     */
    public Seleccion obtener(Player jugador, boolean deWorldEdit) {
        if (deWorldEdit) {
            return WorldEditBridge.obtenerSeleccion(jugador);
        }
        Seleccion s = getSeleccion(jugador);
        if (s == null) {
            throw new IllegalStateException("No tienes una seleccion completa. Usa el hacha (/espacio hacha): "
                    + "clic izquierdo = pos 1, clic derecho = pos 2.");
        }
        return s;
    }

    public void limpiar(UUID uuid) {
        posicion1.remove(uuid);
        posicion2.remove(uuid);
        modoPunto.remove(uuid);
    }

    // ---------------- Modo "marcar un punto" (entrada / salida / sol) ----------------

    /**
     * Activa el modo de marcado de punto: el PROXIMO clic (izq. o der.) del
     * hacha en el mundo correcto fija ese punto exacto, en vez de pos1/pos2.
     * Esto es lo que hace obligatorio usar el hacha para fijar la entrada o
     * la salida de un planeta: no hay forma de fijarlos sin pasar por aqui.
     */
    public void activarModoPunto(Player jugador, TipoPunto tipo, String idPlaneta) {
        modoPunto.put(jugador.getUniqueId(), new ModoPunto(tipo, idPlaneta));
    }

    public ModoPunto getModoPunto(Player jugador) {
        return modoPunto.get(jugador.getUniqueId());
    }

    public void limpiarModoPunto(UUID uuid) {
        modoPunto.remove(uuid);
    }
}
