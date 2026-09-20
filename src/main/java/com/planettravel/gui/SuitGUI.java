package com.planettravel.gui;

import com.planettravel.config.PlanetConfigManager;
import com.planettravel.config.SuitTier;
import com.planettravel.suit.SuitManager;
import com.planettravel.util.Textos;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Editor de TRAJES ESPACIALES. Dos pantallas:
 *   1) Lista de tiers (clic = editar ese tier, botones para anadir/quitar tier).
 *   2) Editor de un tier: material, nombre, color, custom model data,
 *      proteccion al frio/calor, resistencia a presion, y botones para recibir
 *      el casco o el traje completo.
 */
public class SuitGUI {

    public static final String TITULO_LISTA = "§0§lTrajes Espaciales";
    public static final String PREFIJO_TIER = "§0Traje tier ";

    /** Slots donde se colocan los tiers en la lista (maximo 8). */
    public static final int[] SLOTS_TIER = {10, 11, 12, 13, 14, 15, 16, 19};
    public static final int SLOT_ANADIR = 40;
    public static final int SLOT_QUITAR = 41;
    public static final int SLOT_VOLVER_LISTA = 49;

    // Editor de tier
    public static final int SLOT_MATERIAL = 10;
    public static final int SLOT_NOMBRE = 11;
    public static final int SLOT_COLOR = 12;
    public static final int SLOT_MODELO = 13;
    public static final int SLOT_FRIO = 14;
    public static final int SLOT_CALOR = 15;
    public static final int SLOT_PRESION = 16;
    public static final int SLOT_DAR_CASCO = 21;
    public static final int SLOT_DAR_TRAJE = 22;
    public static final int SLOT_VOLVER = 31;

    /** Colores predefinidos para trajes de cuero. */
    public static final int[] COLORES = {
            0xCFD8DC, 0xFFFFFF, 0x2196F3, 0xFF9800, 0xF44336,
            0x9C27B0, 0x4CAF50, 0x212121, 0xFFEB3B, 0x00BCD4
    };

    private final PlanetConfigManager config;
    private final SuitManager suits;

    public SuitGUI(PlanetConfigManager config, SuitManager suits) {
        this.config = config;
        this.suits = suits;
    }

    public static String tituloTier(int tier) {
        return PREFIJO_TIER + tier;
    }

    public static int tierDesdeTitulo(String titulo) {
        try {
            return Integer.parseInt(titulo.substring(PREFIJO_TIER.length()).trim());
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            return -1;
        }
    }

    // ------------------------------------------------------------
    //  Lista
    // ------------------------------------------------------------

    public void abrirLista(Player jugador) {
        Inventory inv = Bukkit.createInventory(null, 54, TITULO_LISTA);
        ItemStack relleno = PlanetEditorGUI.item(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, relleno);
        }

        inv.setItem(4, PlanetEditorGUI.item(Material.LEATHER_HELMET, "§6§lTrajes espaciales",
                "§7Cada planeta exige un tier minimo.",
                "§7Editar un tier afecta a todos los trajes",
                "§7de ese tier que ya existen."));

        int i = 0;
        for (SuitTier tier : config.getTrajes().values()) {
            if (i >= SLOTS_TIER.length) break;
            inv.setItem(SLOTS_TIER[i++], iconoTier(tier));
        }

        if (config.getTrajes().size() < SuitTier.MAX_TIERS) {
            inv.setItem(SLOT_ANADIR, PlanetEditorGUI.item(Material.EMERALD, "§a+ Anadir tier",
                    "§7Crea el tier " + (config.getMaxTierDefinido() + 1) + " copiando el ultimo."));
        }
        if (config.getTrajes().size() > 1) {
            inv.setItem(SLOT_QUITAR, PlanetEditorGUI.item(Material.REDSTONE, "§c- Quitar ultimo tier",
                    "§7Elimina el tier " + config.getMaxTierDefinido() + ".",
                    "§8Shift + clic para confirmar."));
        }
        inv.setItem(SLOT_VOLVER_LISTA, PlanetEditorGUI.item(Material.ARROW, "§7« Menu del espacio"));
        jugador.openInventory(inv);
    }

    private ItemStack iconoTier(SuitTier tier) {
        ItemStack item = suits.crearPieza(tier, "HELMET");
        var meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add("§8Tier " + tier.getId());
            lore.add("§bFrio: §f+" + fmt(tier.getProteccionFrio()) + "°C §7por pieza");
            lore.add("§cCalor: §f+" + fmt(tier.getProteccionCalor()) + "°C §7por pieza");
            lore.add("§dPresion: §f" + fmt(tier.getResistenciaPresion()) + " atm");
            lore.add("§7Material: §f" + Textos.legible(tier.getMaterial()));
            lore.add("");
            lore.add("§eClic §7para editar");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        item.setAmount(tier.getId());
        return item;
    }

    // ------------------------------------------------------------
    //  Editor de un tier
    // ------------------------------------------------------------

    public void abrirTier(Player jugador, SuitTier t) {
        Inventory inv = Bukkit.createInventory(null, 36, tituloTier(t.getId()));
        ItemStack relleno = PlanetEditorGUI.item(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 36; i++) {
            inv.setItem(i, relleno);
        }

        inv.setItem(4, iconoTier(t));

        inv.setItem(SLOT_MATERIAL, PlanetEditorGUI.item(t.materialPieza("HELMET"), "§6Material",
                "§7Actual: §f" + Textos.legible(t.getMaterial()),
                "", "§eClic izq/der §7cambiar material",
                "§8Solo afecta a trajes nuevos."));
        inv.setItem(SLOT_NOMBRE, PlanetEditorGUI.item(Material.NAME_TAG, "§6Nombre",
                "§7Actual: §f" + Textos.color(t.getNombre()),
                "", "§eClic §7para cambiarlo por chat", "§8Admite colores con &"));
        inv.setItem(SLOT_COLOR, PlanetEditorGUI.item(Material.LEATHER_CHESTPLATE, "§6Color (solo cuero)",
                "§7Actual: §f#" + String.format("%06X", t.getColor()),
                t.usaCuero() ? "§eClic izq/der §7cambiar color" : "§8El material actual no es cuero.",
                "§8Solo afecta a trajes nuevos."));
        inv.setItem(SLOT_MODELO, PlanetEditorGUI.item(Material.PAINTING, "§6Custom Model Data",
                "§7Valor: §f" + t.getCustomModelData() + (t.getCustomModelData() == 0 ? " §8(ninguno)" : ""),
                "", "§eClic izq §7subir  §c| §eClic der §7bajar", "§8Shift = +10. Para tu resource pack.",
                "§8Solo afecta a trajes nuevos."));
        inv.setItem(SLOT_FRIO, PlanetEditorGUI.item(Material.BLUE_ICE, "§bProteccion al frio",
                "§7Valor: §f+" + fmt(t.getProteccionFrio()) + "°C §7por pieza",
                "", "§eClic izq §7subir  §c| §eClic der §7bajar", "§8Shift = paso x5. Afecta a todos los trajes."));
        inv.setItem(SLOT_CALOR, PlanetEditorGUI.item(Material.MAGMA_BLOCK, "§cProteccion al calor",
                "§7Valor: §f+" + fmt(t.getProteccionCalor()) + "°C §7por pieza",
                "", "§eClic izq §7subir  §c| §eClic der §7bajar", "§8Shift = paso x5. Afecta a todos los trajes."));
        inv.setItem(SLOT_PRESION, PlanetEditorGUI.item(Material.GLASS_BOTTLE, "§dResistencia a presion",
                "§7Valor: §f" + fmt(t.getResistenciaPresion()) + " atm §7(traje completo)",
                "", "§eClic izq §7subir  §c| §eClic der §7bajar", "§8Shift = paso x5. Afecta a todos los trajes."));

        inv.setItem(SLOT_DAR_CASCO, PlanetEditorGUI.item(Material.LEATHER_HELMET, "§aRecibir casco tier " + t.getId()));
        inv.setItem(SLOT_DAR_TRAJE, PlanetEditorGUI.item(Material.LEATHER_CHESTPLATE, "§aRecibir traje completo tier " + t.getId()));
        inv.setItem(SLOT_VOLVER, PlanetEditorGUI.item(Material.ARROW, "§7« Lista de trajes"));
        jugador.openInventory(inv);
    }

    private static String fmt(double v) {
        return (v == Math.floor(v)) ? String.valueOf((int) v) : String.format("%.1f", v);
    }
}
