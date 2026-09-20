package com.planettravel.config;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Definicion de UN tier de traje espacial. El admin puede editarlas desde
 * /espacio -> Trajes, o en config.yml (seccion "trajes").
 *
 * Los items que reciben los jugadores solo guardan el NUMERO de tier (en su
 * PersistentDataContainer); todos los valores de proteccion se leen de aqui en
 * cada comprobacion. Asi, si cambias la proteccion del tier 3, TODOS los trajes
 * tier 3 que ya existen cambian a la vez, sin tener que regalarlos otra vez.
 * (El nombre, material y color del item si quedan fijados al crearlo.)
 */
public class SuitTier {

    public static final List<String> FAMILIAS =
            List.of("LEATHER", "CHAINMAIL", "IRON", "GOLDEN", "DIAMOND", "NETHERITE");

    /** Maximo de tiers que se pueden definir. */
    public static final int MAX_TIERS = 8;

    private final int id;
    private String nombre;
    private String material = "IRON";
    private int color = 0xCFD8DC;
    private int customModelData = 0;

    /** Grados de proteccion contra el frio que aporta CADA pieza puesta. */
    private double proteccionFrio = 10;
    /** Grados de proteccion contra el calor que aporta CADA pieza puesta. */
    private double proteccionCalor = 10;
    /** Presion (atm) que aguanta el traje completo. */
    private double resistenciaPresion = 2;

    public SuitTier(int id) {
        this.id = id;
        this.nombre = "&fTraje espacial T" + id;
    }

    public static SuitTier desdeConfig(int id, ConfigurationSection s) {
        SuitTier t = new SuitTier(id);
        if (s == null) {
            return t;
        }
        t.nombre = s.getString("nombre", t.nombre);
        t.material = s.getString("material", "IRON").toUpperCase(Locale.ROOT);
        t.color = parseColor(s.getString("color", "CFD8DC"));
        t.customModelData = Math.max(0, s.getInt("custom-model-data", 0));
        t.proteccionFrio = s.getDouble("proteccion-frio", 10);
        t.proteccionCalor = s.getDouble("proteccion-calor", 10);
        t.resistenciaPresion = s.getDouble("resistencia-presion", 2);
        return t;
    }

    public void guardarEn(ConfigurationSection s) {
        s.set("nombre", nombre);
        s.set("material", material);
        s.set("color", String.format("%06X", color));
        s.set("custom-model-data", customModelData);
        s.set("proteccion-frio", proteccionFrio);
        s.set("proteccion-calor", proteccionCalor);
        s.set("resistencia-presion", resistenciaPresion);
    }

    private static int parseColor(String texto) {
        try {
            return Integer.parseInt(texto.replace("#", "").trim(), 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            return 0xCFD8DC;
        }
    }

    /** Tiers que se crean si el config.yml no define ninguno. */
    public static Map<Integer, SuitTier> porDefecto() {
        Map<Integer, SuitTier> m = new LinkedHashMap<>();
        m.put(1, crear(1, "&fTraje Espacial Basico", "LEATHER", 0xCFD8DC, 12, 12, 2));
        m.put(2, crear(2, "&bTraje Espacial Reforzado", "IRON", 0xFFFFFF, 22, 22, 6));
        m.put(3, crear(3, "&6Traje Termico", "GOLDEN", 0xFFFFFF, 35, 35, 20));
        m.put(4, crear(4, "&dTraje de Alta Presion", "DIAMOND", 0xFFFFFF, 50, 50, 60));
        m.put(5, crear(5, "&5Traje de Exploracion Extrema", "NETHERITE", 0xFFFFFF, 75, 75, 120));
        return m;
    }

    private static SuitTier crear(int id, String nombre, String material, int color,
                                  double frio, double calor, double presion) {
        SuitTier t = new SuitTier(id);
        t.nombre = nombre;
        t.material = material;
        t.color = color;
        t.proteccionFrio = frio;
        t.proteccionCalor = calor;
        t.resistenciaPresion = presion;
        return t;
    }

    /** Material de una pieza concreta ("HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS"). */
    public Material materialPieza(String pieza) {
        Material m = Material.matchMaterial(material + "_" + pieza);
        return (m != null) ? m : Material.IRON_HELMET;
    }

    public boolean usaCuero() {
        return "LEATHER".equals(material);
    }

    public int getId() { return id; }

    public String getNombre() { return nombre; }
    public void setNombre(String v) { this.nombre = v; }

    public String getMaterial() { return material; }
    public void setMaterial(String v) { this.material = v; }

    public int getColor() { return color; }
    public void setColor(int v) { this.color = v & 0xFFFFFF; }

    public int getCustomModelData() { return customModelData; }
    public void setCustomModelData(int v) { this.customModelData = Math.max(0, v); }

    public double getProteccionFrio() { return proteccionFrio; }
    public void setProteccionFrio(double v) { this.proteccionFrio = Math.max(0, Math.min(500, v)); }

    public double getProteccionCalor() { return proteccionCalor; }
    public void setProteccionCalor(double v) { this.proteccionCalor = Math.max(0, Math.min(500, v)); }

    public double getResistenciaPresion() { return resistenciaPresion; }
    public void setResistenciaPresion(double v) { this.resistenciaPresion = Math.max(0, Math.min(1000, v)); }
}
