package com.planettravel.gui;

import com.planettravel.config.PlanetConfigManager;
import com.planettravel.config.PlanetData;
import com.planettravel.config.PlanetEnvironment;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * MAPA ESTELAR: GUI que muestra todos los planetas configurados con su icono,
 * distancia desde la posicion actual del jugador y un resumen de sus peligros.
 *
 * Es de solo lectura para jugadores normales. Los admins pueden hacer clic
 * para abrir el editor de ese planeta ({@link PlanetEditorGUI}).
 */
public class StarMapGUI {

    public static final String TITULO = "§0§lMapa Estelar";

    private final PlanetConfigManager configManager;

    public StarMapGUI(PlanetConfigManager configManager) {
        this.configManager = configManager;
    }

    public void abrir(Player jugador) {
        // Tamanio dinamico: filas de 9, minimo 1, maximo 6.
        int planetas = configManager.getPlanetas().size();
        int filas = Math.max(1, Math.min(6, (int) Math.ceil(planetas / 9.0) + 1));
        Inventory inv = Bukkit.createInventory(null, filas * 9, TITULO);

        int slot = 0;
        for (PlanetData planeta : configManager.getPlanetas().values()) {
            if (slot >= filas * 9 - 1) break;
            inv.setItem(slot++, crearIcono(jugador, planeta));
        }

        // Item informativo en la ultima ranura
        ItemStack info = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = info.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§b§lSistema estelar");
            List<String> lore = new ArrayList<>();
            lore.add("§7Planetas conocidos: §f" + planetas);
            lore.add("§7Mundo espacio: §f" + configManager.getMundoEspacio());
            lore.add("");
            lore.add("§8Vuela hacia una esfera para aterrizar.");
            lore.add("§8Sube sobre la altura de escape para despegar.");
            if (jugador.hasPermission("planettravel.admin")) {
                lore.add("");
                lore.add("§e▶ Clic en un planeta para editarlo");
            }
            meta.setLore(lore);
            info.setItemMeta(meta);
        }
        inv.setItem(filas * 9 - 1, info);

        jugador.openInventory(inv);
    }

    /** Construye el item que representa a un planeta, con toda su info en el lore. */
    private ItemStack crearIcono(Player jugador, PlanetData planeta) {
        ItemStack item = new ItemStack(planeta.getIcono());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.setDisplayName("§b§l" + planeta.getNombreVisible());

        List<String> lore = new ArrayList<>();

        if (!planeta.getDescripcion().isEmpty()) {
            lore.add("§7" + planeta.getDescripcion());
            lore.add("");
        }

        // Distancia: solo tiene sentido si el jugador esta en el mundo espacio.
        if (jugador.getWorld().getName().equals(configManager.getMundoEspacio())) {
            double distancia = planeta.distanciaAlCentro(
                    jugador.getLocation().getX(),
                    jugador.getLocation().getY(),
                    jugador.getLocation().getZ());
            double alBorde = Math.max(0, distancia - planeta.getRadio());
            lore.add("§7Distancia: §f" + String.format("%.0f", alBorde) + " §7bloques");
        } else {
            lore.add("§8Distancia: §7(solo visible desde el espacio)");
        }

        lore.add("§7Coordenadas: §f" + String.format("%.0f, %.0f, %.0f",
                planeta.getSpaceX(), planeta.getSpaceY(), planeta.getSpaceZ()));
        lore.add("§7Radio de entrada: §f" + String.format("%.0f", planeta.getRadio()));
        lore.add("");

        // --- Resumen de peligros ---
        PlanetEnvironment env = planeta.getEntorno();
        lore.add("§6§lCondiciones");

        if (env.isRequiereCasco()) {
            lore.add("§c ✖ Sin atmosfera respirable");
            lore.add("§8   Requiere: §7" + formatearMaterial(env.getMaterialCasco()));
            if (env.isRequiereTrajeCompleto()) {
                lore.add("§8   Y traje completo");
            }
        } else {
            lore.add("§a ✔ Atmosfera respirable");
        }

        if (env.isAguaToxica()) {
            lore.add("§2 ☣ Agua corrosiva" + (env.isAguaEnvenena() ? " y venenosa" : ""));
        }

        if (env.getGravedad() < 0.9) {
            lore.add("§d ⬇ Gravedad baja §7(" + String.format("%.1f", env.getGravedad()) + "x)");
        } else if (env.getGravedad() > 1.1) {
            lore.add("§4 ⬇ Gravedad aplastante §7(" + String.format("%.1f", env.getGravedad()) + "x)");
        }

        if (env.isTemperaturaActiva()) {
            lore.add("§e 🌡 Temperatura base: §f" + String.format("%.0f°C", env.getTemperaturaBase()));
            lore.add("§8   Seguro entre §7" + String.format("%.0f°C", env.getUmbralFrio())
                    + " §8y §7" + String.format("%.0f°C", env.getUmbralCalor()));
        }

        if (!env.getEfectosPermanentes().isEmpty()) {
            lore.add("§5 ✦ Efectos constantes:");
            for (var e : env.getEfectosPermanentes().entrySet()) {
                lore.add("§8   • §7" + formatearMaterial(e.getKey()) + " " + e.getValue());
            }
        }

        if (jugador.hasPermission("planettravel.admin")) {
            lore.add("");
            lore.add("§e▶ Clic para editar este planeta");
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /** Convierte SNAKE_CASE en "Snake case" para que se lea bien en la GUI. */
    public static String formatearMaterial(String nombre) {
        String limpio = nombre.toLowerCase().replace('_', ' ');
        return Character.toUpperCase(limpio.charAt(0)) + limpio.substring(1);
    }
}
