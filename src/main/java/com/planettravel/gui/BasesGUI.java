package com.planettravel.gui;

import com.planettravel.space.SpaceBase;
import com.planettravel.space.SpaceBaseManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Lista de BASES ESPACIALES. Cada base es un item; los clics hacen:
 *   izq = teletransportarte   |  der = oxigeno on/off
 *   shift+izq = temperatura +1 | shift+der = temperatura -1
 *   Q = redefinir su zona con tu seleccion | Ctrl+Q = borrar
 * Boton inferior: crear una base nueva con la seleccion actual.
 */
public class BasesGUI {

    public static final String TITULO = "§0§lBases Espaciales";
    public static final int SLOT_VOLVER = 45;
    public static final int SLOT_CREAR = 49;
    public static final int SLOT_INFO = 53;
    public static final int MAX_BASES = 45;

    private final SpaceBaseManager bases;

    public BasesGUI(SpaceBaseManager bases) {
        this.bases = bases;
    }

    /** Bases en el orden en que aparecen en los slots 0..44. */
    public List<SpaceBase> listar() {
        List<SpaceBase> lista = new ArrayList<>(bases.getBases());
        return lista.size() > MAX_BASES ? lista.subList(0, MAX_BASES) : lista;
    }

    public void abrir(Player jugador) {
        Inventory inv = Bukkit.createInventory(null, 54, TITULO);

        int slot = 0;
        for (SpaceBase b : listar()) {
            inv.setItem(slot++, crearIcono(b));
        }

        ItemStack relleno = PlanetEditorGUI.item(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, relleno);
        }
        inv.setItem(SLOT_VOLVER, PlanetEditorGUI.item(Material.ARROW, "§7« Menu del espacio"));
        inv.setItem(SLOT_CREAR, PlanetEditorGUI.item(Material.EMERALD_BLOCK, "§a§l+ Crear base con mi seleccion",
                "§7Marca la zona con el hacha (o con WorldEdit)",
                "§7y haz clic aqui. Te pedire el nombre por chat.",
                "",
                "§eClic izq §7usar el hacha de PlanetTravel",
                "§eShift + izq §7usar la seleccion de WorldEdit"));
        inv.setItem(SLOT_INFO, PlanetEditorGUI.item(Material.BOOK, "§bComo funcionan",
                "§7Dentro de la zona hay oxigeno: se respira",
                "§7sin traje y la temperatura es la de la base.",
                "§7Fuera rigen las reglas del espacio.",
                "",
                "§8Bases: " + bases.getBases().size()));

        jugador.openInventory(inv);
    }

    private ItemStack crearIcono(SpaceBase b) {
        boolean aire = b.tieneOxigeno();
        List<String> lore = new ArrayList<>();
        lore.add("§8id: " + b.getId());
        lore.add("§7Mundo: §f" + b.getMundo());
        lore.add("§7Tamanio: §f" + b.getAnchoX() + "x" + b.getAltoY() + "x" + b.getAnchoZ());
        lore.add("§7Oxigeno: " + (b.isOxigeno() ? "§a✔ activo" : "§c✖ apagado"));
        lore.add("§7Temperatura: §f" + String.format("%.0f°C", b.getTemperatura()));
        lore.add("§7Gravedad artificial: " + (b.isGravedadArtificial() ? "§a✔" : "§c✖"));
        lore.add("§7Tipo: §f" + b.getTipo().name().toLowerCase());
        if (b.isRequiereFuel()) {
            lore.add("§7Combustible: §f" + String.format("%.0f / %.0f", b.getFuelActual(), b.getFuelMax()));
        }
        lore.add("");
        lore.add("§eClic izq §7teletransportarte");
        lore.add("§eClic der §7oxigeno on/off");
        lore.add("§eShift izq/der §7temperatura +1 / -1");
        lore.add("§eQ §7redefinir zona con mi seleccion");
        lore.add("§cCtrl+Q §7borrar");
        return PlanetEditorGUI.item(aire ? Material.GLASS : Material.BARRIER, "§a§l" + b.getNombre(),
                lore.toArray(new String[0]));
    }
}
