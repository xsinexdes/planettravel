package com.planettravel.gui;

import com.planettravel.config.PlanetConfigManager;
import com.planettravel.config.PlanetData;
import com.planettravel.util.Textos;
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

    /** Construye el item que representa a un planeta, con su descripcion y su ficha. */
    private ItemStack crearIcono(Player jugador, PlanetData planeta) {
        ItemStack item = new ItemStack(planeta.getIcono());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.setDisplayName("§b§l" + planeta.getNombreVisible());

        List<String> lore = new ArrayList<>();

        if (!planeta.getClasificacion().isEmpty()) {
            lore.add("§3" + Textos.color(planeta.getClasificacion()));
        }
        List<String> descripcion = PlanetInfo.descripcion(planeta);
        if (!descripcion.isEmpty()) {
            lore.addAll(descripcion);
        }
        if (!lore.isEmpty()) {
            lore.add("");
        }

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
        lore.add("");

        lore.addAll(PlanetInfo.ficha(planeta, configManager));

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
