package com.planettravel.suit;

import com.planettravel.config.PlanetConfigManager;
import com.planettravel.config.PlanetEnvironment;
import com.planettravel.config.SuitTier;
import com.planettravel.util.Textos;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Crea y reconoce los TRAJES ESPACIALES.
 *
 * Un traje es una armadura normal con dos etiquetas invisibles (tier y pieza)
 * guardadas en su PersistentDataContainer. Las etiquetas no se pueden copiar
 * renombrando un yunque ni con comandos vanilla, y sobreviven a cualquier
 * cambio de nombre o encantamiento.
 *
 * Las definiciones (proteccion, material...) viven en {@link SuitTier}, dentro
 * de PlanetConfigManager, y se leen en vivo: editar un tier afecta a todos los
 * trajes de ese tier que ya existan.
 */
public class SuitManager {

    public static final List<String> PIEZAS = List.of("HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS");

    private final PlanetConfigManager config;
    private final NamespacedKey claveTier;
    private final NamespacedKey clavePieza;

    public SuitManager(JavaPlugin plugin, PlanetConfigManager config) {
        this.config = config;
        this.claveTier = new NamespacedKey(plugin, "suit_tier");
        this.clavePieza = new NamespacedKey(plugin, "suit_piece");
    }

    // ------------------------------------------------------------
    //  Creacion de items
    // ------------------------------------------------------------

    public ItemStack crearPieza(SuitTier tier, String pieza) {
        ItemStack item = new ItemStack(tier.materialPieza(pieza));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.setDisplayName(Textos.color(tier.getNombre()) + " §7- " + nombrePieza(pieza));

        List<String> lore = new ArrayList<>();
        lore.add("§8Traje espacial - Tier " + tier.getId());
        lore.add("");
        lore.add("§bProteccion al frio: §f+" + formato(tier.getProteccionFrio()) + "°C §7por pieza");
        lore.add("§cProteccion al calor: §f+" + formato(tier.getProteccionCalor()) + "°C §7por pieza");
        lore.add("§dResistencia a presion: §f" + formato(tier.getResistenciaPresion()) + " atm §7(traje completo)");
        lore.add("");
        lore.add("§7Necesario para respirar donde no hay aire.");
        meta.setLore(lore);

        if (meta instanceof LeatherArmorMeta cuero && tier.usaCuero()) {
            cuero.setColor(Color.fromRGB(tier.getColor()));
        }
        if (tier.getCustomModelData() > 0) {
            meta.setCustomModelData(tier.getCustomModelData());
        }

        meta.getPersistentDataContainer().set(claveTier, PersistentDataType.INTEGER, tier.getId());
        meta.getPersistentDataContainer().set(clavePieza, PersistentDataType.STRING, pieza);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    public List<ItemStack> crearTrajeCompleto(SuitTier tier) {
        List<ItemStack> piezas = new ArrayList<>();
        for (String pieza : PIEZAS) {
            piezas.add(crearPieza(tier, pieza));
        }
        return piezas;
    }

    public static String nombrePieza(String pieza) {
        return switch (pieza) {
            case "HELMET" -> "Casco";
            case "CHESTPLATE" -> "Pechera";
            case "LEGGINGS" -> "Pantalones";
            case "BOOTS" -> "Botas";
            default -> pieza;
        };
    }

    private static String formato(double v) {
        return (v == Math.floor(v)) ? String.valueOf((int) v) : String.format("%.1f", v);
    }

    // ------------------------------------------------------------
    //  Lectura de items
    // ------------------------------------------------------------

    /** Tier del item (0 si no es un traje, o si su tier ya no existe en la config). */
    public int getTier(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
            return 0;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return 0;
        }
        Integer tier = meta.getPersistentDataContainer().get(claveTier, PersistentDataType.INTEGER);
        if (tier == null || config.getTraje(tier) == null) {
            return 0;
        }
        return tier;
    }

    /** Definicion del tier de una pieza, o null si no es un traje. */
    public SuitTier getDefinicion(ItemStack item) {
        int tier = getTier(item);
        return (tier > 0) ? config.getTraje(tier) : null;
    }

    public int getTierCasco(Player jugador) {
        return getTier(jugador.getInventory().getHelmet());
    }

    /** Tier de la pieza PEOR de las cuatro (0 si falta alguna o no es traje). */
    public int getTierMinimoTraje(Player jugador) {
        int minimo = Integer.MAX_VALUE;
        for (ItemStack pieza : jugador.getInventory().getArmorContents()) {
            minimo = Math.min(minimo, getTier(pieza));
        }
        return (minimo == Integer.MAX_VALUE) ? 0 : minimo;
    }

    /**
     * Presion (atm) que aguanta el jugador: la del traje completo mas debil.
     * Si no lleva las cuatro piezas de traje, 0.
     */
    public double getResistenciaPresion(Player jugador) {
        double minima = Double.MAX_VALUE;
        for (ItemStack pieza : jugador.getInventory().getArmorContents()) {
            SuitTier def = getDefinicion(pieza);
            if (def == null) {
                return 0;
            }
            minima = Math.min(minima, def.getResistenciaPresion());
        }
        return (minima == Double.MAX_VALUE) ? 0 : minima;
    }

    // ------------------------------------------------------------
    //  Requisitos de un planeta
    // ------------------------------------------------------------

    /** Tier exigido por el entorno, ajustado a los tiers que existen de verdad. */
    public int tierRequerido(PlanetEnvironment env) {
        int requerido = env.getTierCasco();
        if (requerido <= 0) {
            return 0;
        }
        return Math.min(requerido, Math.max(1, config.getMaxTierDefinido()));
    }

    /**
     * Devuelve cuanto le falta al jugador para estar protegido, entre 0 y 1:
     *   0   = protegido del todo
     *   1   = sin proteccion (asfixia completa)
     *   0.x = lleva un traje, pero de un tier inferior al exigido
     */
    public double factorAsfixia(Player jugador, PlanetEnvironment env) {
        int requerido = tierRequerido(env);

        if (requerido > 0) {
            int puesto = env.isRequiereTrajeCompleto() ? getTierMinimoTraje(jugador) : getTierCasco(jugador);
            if (puesto >= requerido) {
                return 0.0;
            }
            if (puesto <= 0) {
                return 1.0;
            }
            return Math.max(0.25, 1.0 - (double) puesto / requerido);
        }

        // Modo clasico (tier 0): un material de casco concreto.
        Material exigido = Material.matchMaterial(env.getMaterialCasco());
        if (exigido == null) {
            return 0.0; // material mal configurado: no castigamos al jugador
        }
        ItemStack casco = jugador.getInventory().getHelmet();
        if (casco == null || casco.getType() != exigido) {
            return 1.0;
        }
        if (env.isRequiereTrajeCompleto()) {
            for (ItemStack pieza : jugador.getInventory().getArmorContents()) {
                if (pieza == null || pieza.getType() == Material.AIR) {
                    return 1.0;
                }
            }
        }
        return 0.0;
    }

    /** Texto corto de lo que exige un entorno, para mensajes y GUIs. */
    public String describirRequisito(PlanetEnvironment env) {
        int requerido = tierRequerido(env);
        String completo = env.isRequiereTrajeCompleto() ? " completo" : "";
        if (requerido > 0) {
            SuitTier def = config.getTraje(requerido);
            String nombre = (def == null) ? "" : " (" + Textos.color(def.getNombre()) + "§7)";
            return "Traje" + completo + " tier " + requerido + nombre;
        }
        return Textos.legible(env.getMaterialCasco()) + (env.isRequiereTrajeCompleto() ? " + traje completo" : "");
    }
}
