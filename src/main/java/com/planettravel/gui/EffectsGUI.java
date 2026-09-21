package com.planettravel.gui;

import com.planettravel.config.PlanetData;
import com.planettravel.config.PlanetEnvironment;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Selector de EFECTOS PERMANENTES para un planeta.
 *
 * Muestra una seleccion curada de efectos que tienen sentido tematicamente en
 * un planeta (no los 30 y pico que existen, que serian inmanejables). Cada uno
 * se activa/desactiva y se le sube o baja el nivel con clic izquierdo/derecho.
 */
public class EffectsGUI {

    public static final String PREFIJO_TITULO = "§0Efectos: ";

    /**
     * Efectos ofrecidos, con el item que los representa y una descripcion
     * de por que encajan en un planeta. Mapa ordenado para que la GUI salga
     * siempre igual.
     */
    private static final Map<String, EfectoInfo> EFECTOS = new LinkedHashMap<>();

    static {
        EFECTOS.put("SLOWNESS", new EfectoInfo(Material.SOUL_SAND,
                "§8Lentitud", "Terreno denso o gravedad pesada"));
        EFECTOS.put("SPEED", new EfectoInfo(Material.SUGAR,
                "§bVelocidad", "Gravedad ligera, aire enrarecido"));
        EFECTOS.put("JUMP_BOOST", new EfectoInfo(Material.RABBIT_FOOT,
                "§aSalto potenciado", "Poca gravedad"));
        EFECTOS.put("SLOW_FALLING", new EfectoInfo(Material.PHANTOM_MEMBRANE,
                "§fCaida lenta", "Atmosfera densa"));
        EFECTOS.put("NIGHT_VISION", new EfectoInfo(Material.GOLDEN_CARROT,
                "§eVision nocturna", "Planeta en penumbra eterna"));
        EFECTOS.put("BLINDNESS", new EfectoInfo(Material.INK_SAC,
                "§8Ceguera", "Tormentas de polvo, niebla espesa"));
        EFECTOS.put("DARKNESS", new EfectoInfo(Material.BLACK_WOOL,
                "§8Oscuridad", "Planeta sin estrella cercana"));
        EFECTOS.put("POISON", new EfectoInfo(Material.SPIDER_EYE,
                "§2Veneno", "Aire toxico permanente"));
        EFECTOS.put("WITHER", new EfectoInfo(Material.WITHER_ROSE,
                "§8Marchitamiento", "Radiacion letal"));
        EFECTOS.put("HUNGER", new EfectoInfo(Material.ROTTEN_FLESH,
                "§6Hambre", "El cuerpo consume mas energia"));
        EFECTOS.put("WEAKNESS", new EfectoInfo(Material.FERMENTED_SPIDER_EYE,
                "§7Debilidad", "Atmosfera que agota"));
        EFECTOS.put("MINING_FATIGUE", new EfectoInfo(Material.IRON_PICKAXE,
                "§7Fatiga al minar", "Roca extremadamente dura"));
        EFECTOS.put("FIRE_RESISTANCE", new EfectoInfo(Material.MAGMA_CREAM,
                "§cResistencia al fuego", "Adaptacion a planeta volcanico"));
        EFECTOS.put("WATER_BREATHING", new EfectoInfo(Material.PUFFERFISH,
                "§3Respiracion acuatica", "Planeta oceanico"));
        EFECTOS.put("REGENERATION", new EfectoInfo(Material.GHAST_TEAR,
                "§dRegeneracion", "Planeta con propiedades curativas"));
        EFECTOS.put("RESISTANCE", new EfectoInfo(Material.SHIELD,
                "§9Resistencia", "Campo protector natural"));
        EFECTOS.put("GLOWING", new EfectoInfo(Material.GLOWSTONE_DUST,
                "§fBrillo", "Esporas luminosas en el aire"));
        EFECTOS.put("LEVITATION", new EfectoInfo(Material.SHULKER_SHELL,
                "§5Levitacion", "Anomalia gravitatoria"));
    }

    public static String tituloPara(PlanetData planeta) {
        return PREFIJO_TITULO + planeta.getId();
    }

    public static String idDesdeTitulo(String titulo) {
        if (!titulo.startsWith(PREFIJO_TITULO)) {
            return null;
        }
        return titulo.substring(PREFIJO_TITULO.length());
    }

    /** Devuelve el nombre del efecto que ocupa un slot dado, o null. */
    public static String efectoEnSlot(int slot) {
        List<String> claves = new ArrayList<>(EFECTOS.keySet());
        if (slot < 0 || slot >= claves.size()) {
            return null;
        }
        return claves.get(slot);
    }

    public void abrir(Player jugador, PlanetData planeta) {
        Inventory inv = Bukkit.createInventory(null, 36, tituloPara(planeta));
        PlanetEnvironment env = planeta.getEntorno();

        int slot = 0;
        for (Map.Entry<String, EfectoInfo> entrada : EFECTOS.entrySet()) {
            String clave = entrada.getKey();
            EfectoInfo info = entrada.getValue();
            Integer nivel = env.getEfectosPermanentes().get(clave);
            boolean activo = (nivel != null);

            List<String> lore = new ArrayList<>();
            lore.add("§7" + info.descripcion);
            lore.add("");
            if (activo) {
                lore.add("§a✔ ACTIVO §7— nivel §f" + nivel);
            } else {
                lore.add("§c✖ Inactivo");
            }
            lore.add("");
            lore.add("§eClic izq §7activar / subir nivel");
            lore.add("§eClic der §7bajar nivel / quitar");

            ItemStack item = PlanetEditorGUI.item(
                    activo ? info.icono : Material.GRAY_DYE,
                    info.nombre,
                    lore.toArray(new String[0]));
            inv.setItem(slot++, item);
        }

        // Relleno del hueco restante hasta la barra inferior
        ItemStack relleno = PlanetEditorGUI.item(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = slot; i < 27; i++) {
            inv.setItem(i, relleno);
        }
        for (int i = 27; i < 36; i++) {
            inv.setItem(i, relleno);
        }

        inv.setItem(31, PlanetEditorGUI.item(Material.ARROW, "§7« Volver al editor"));

        jugador.openInventory(inv);
    }

    /** Info estatica de cada efecto ofrecido en la GUI. */
    private record EfectoInfo(Material icono, String nombre, String descripcion) { }
}
